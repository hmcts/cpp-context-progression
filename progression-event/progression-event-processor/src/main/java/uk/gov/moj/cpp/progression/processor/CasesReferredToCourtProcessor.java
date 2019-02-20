package uk.gov.moj.cpp.progression.processor;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CreateHearingDefendantRequest;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReferProsecutionCasesToCourtRejected;
import uk.gov.justice.core.courts.ReferredListHearingRequest;
import uk.gov.justice.core.courts.SendCaseForListing;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.DataValidationException;
import uk.gov.moj.cpp.progression.exception.MissingRequiredFieldException;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.MessageService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ReferredCourtDocumentTransformer;
import uk.gov.moj.cpp.progression.transformer.ReferredProsecutionCaseTransformer;
import uk.gov.moj.cpp.progression.transformer.SendCaseForListingTransformer;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;


@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:CommentedOutCodeLine","squid:S2789"})
public class CasesReferredToCourtProcessor {

    static final String REFER_PROSECUTION_CASES_TO_COURT_REJECTED = "public.progression.refer-prosecution-cases-to-court-rejected";
    static final String PROGRESSION_COMMAND_CREATE_HEARING_DEFENDANT_REQUEST = "progression.command.create-hearing-defendant-request";

    private static final Logger LOGGER = LoggerFactory.getLogger(CasesReferredToCourtProcessor.class.getCanonicalName());

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private ListingService listingService;
    @Inject
    private MessageService messageService;
    @Inject
    private ProgressionService progressionService;
    @Inject
    private ReferredProsecutionCaseTransformer referredProsecutionCaseTransformer;
    @Inject
    private ReferredCourtDocumentTransformer referredCourtDocumentTransformer;
    @Inject
    private SendCaseForListingTransformer sendCaseForListingTransformer;

    /**
     * The inbound progression.event.cases-referred-to-court should be enriched  before it is
     * synchronised to the view store.  The following enrichment is done.
     * <p>
     * 1) Reference data offence service - enrich each offence with the legislation, title,
     * legislationWelsh, titleWelsh, mode of trial and code
     * <p>
     * 2) Reference data service - enrich all person objects with nationalityCode and description
     * based on nationalityId, additionalNationalityCode & description based on
     * additionalNationalityId, ethnicityCode and description based on ethnicityId,
     * observedEthnicityCode and description based on observedEthnicityId, selfDefinedEthnicityCode
     * and description based on selfDefinedEthnicityId
     * <p>
     * 3) Reference data service - enrich court documents setting the documentTypeDescription based
     * on documentTypeId
     * <p>
     * 4) Reference data service - enrich listHearingRequests.hearingType.description based on
     * listHearingRequests.hearingType.Id
     * <p>
     * 5) Determine that the offence mode of trial is correct for the initiation code.
     * If the initiation code is J or Z, the mode of trial cannot be Indictable or EitherWay.
     * When this occurs raise the public event progression.events.referProsecutionCasesToCourtRejected
     * with the relevant offenceId item that is not cannot be Indictable or EitherWay.
     * <p>
     * 6) When a case is received from SJP referral, a check must be performed to avoid the duplicate.
     * <p>
     * 7) In case of SJP referrals, Post code is mandatory.
     * <p>
     * Once enrichment is complete, the event processor call a private command handler to sync it to view store
     */
    @Handles("progression.event.cases-referred-to-court")
    public void process(final JsonEnvelope jsonEnvelope) {
        final UUID hearingId = UUID.randomUUID();
        final JsonObject privateEventPayload = jsonEnvelope.payloadAsJsonObject();

        final SjpCourtReferral sjpCourtReferral = jsonObjectToObjectConverter.convert(privateEventPayload.getJsonObject("courtReferral"), SjpCourtReferral.class);
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        final List<CourtDocument> courtDocuments = new ArrayList<>();
        SendCaseForListing sendCaseForListing = null;
        try {
            sjpCourtReferral.getProsecutionCases().stream().forEach(referredProsecutionCase -> {
                searchForDuplicateCases(jsonEnvelope, referredProsecutionCase
                        .getProsecutionCaseIdentifier().getProsecutionAuthorityReference());
                searchForDuplicateCases(jsonEnvelope, referredProsecutionCase
                        .getProsecutionCaseIdentifier().getCaseURN());
            });
            prosecutionCases.addAll(convertToCourtReferral(jsonEnvelope, sjpCourtReferral));
            courtDocuments.addAll(convertToCourtDocument(jsonEnvelope, sjpCourtReferral));
            sendCaseForListing = prepareSendCaseForListing(jsonEnvelope, sjpCourtReferral, prosecutionCases, hearingId);


        } catch (MissingRequiredFieldException | DataValidationException | ReferenceDataNotFoundException e) {
            //Raise public event
            LOGGER.error("### Transformation and enrichment exception", e);
            final ReferProsecutionCasesToCourtRejected referProsecutionCasesToCourtRejected = ReferProsecutionCasesToCourtRejected
                    .referProsecutionCasesToCourtRejected()
                    .withSjpCourtReferral(sjpCourtReferral)
                    .withRejectedReason(e.getMessage())
                    .build();

            messageService.sendMessage(jsonEnvelope, objectToJsonObjectConverter.convert(referProsecutionCasesToCourtRejected), REFER_PROSECUTION_CASES_TO_COURT_REJECTED);
            return;
        }



        final List<ListDefendantRequest> listDefendantRequests = sjpCourtReferral.getListHearingRequests().stream().map(ReferredListHearingRequest::getListDefendantRequests).flatMap(Collection::stream).collect(Collectors.toList());
        final JsonObject hearingDefendantRequestJson = objectToJsonObjectConverter.convert(CreateHearingDefendantRequest.createHearingDefendantRequest()
                                                                                                                        .withHearingId(hearingId)
                                                                                                                        .withDefendantRequests(listDefendantRequests)
                                                                                                                        .build());
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_CREATE_HEARING_DEFENDANT_REQUEST).apply(hearingDefendantRequestJson));


        progressionService.createProsecutionCases(jsonEnvelope, prosecutionCases);
        progressionService.createCourtDocument(jsonEnvelope, courtDocuments);

        listingService.sendCaseForListing(jsonEnvelope, sendCaseForListing);
        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, sendCaseForListing);
    }

    private void searchForDuplicateCases(final JsonEnvelope jsonEnvelope, final String reference) {
        if (StringUtils.isNotEmpty(reference)) {
            final Optional<JsonObject> jsonObject = progressionService.searchCaseDetailByReference(jsonEnvelope, reference);
            if (jsonObject.isPresent() && !jsonObject.get().getJsonArray("searchResults").isEmpty()) {
                throw new DataValidationException("Case has been already referred");
            }
        }
    }

    private SendCaseForListing prepareSendCaseForListing(final JsonEnvelope jsonEnvelope, final SjpCourtReferral sjpCourtReferral, final List<ProsecutionCase> prosecutionCases, final UUID hearingId) {
        return sendCaseForListingTransformer
                .transform(jsonEnvelope, prosecutionCases, sjpCourtReferral.getSjpReferral(),sjpCourtReferral.getListHearingRequests(),hearingId);
    }

    private List<CourtDocument> convertToCourtDocument(final JsonEnvelope jsonEnvelope, final SjpCourtReferral sjpCourtReferral) {
        final List<CourtDocument> courtDocuments = new ArrayList<>();
        if (sjpCourtReferral.getCourtDocuments() != null) {
            courtDocuments.addAll(sjpCourtReferral.getCourtDocuments().stream()
                    .map(referredCourtDocument -> referredCourtDocumentTransformer
                            .transform(referredCourtDocument, jsonEnvelope)).collect(Collectors.toList()));
        }
        return courtDocuments;
    }

    private List<ProsecutionCase> convertToCourtReferral(final JsonEnvelope jsonEnvelope, final SjpCourtReferral sjpCourtReferral) {
        return sjpCourtReferral.getProsecutionCases().stream()
                .map(referredProsecutionCase -> referredProsecutionCaseTransformer
                        .transform(referredProsecutionCase, jsonEnvelope)).collect(Collectors.toList());
    }


}
