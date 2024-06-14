package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.core.courts.HearingLanguage.ENGLISH;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CaseLinkedToHearing;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferProsecutionCasesToCourtRejected;
import uk.gov.justice.core.courts.ReferredDefendant;
import uk.gov.justice.core.courts.ReferredListHearingRequest;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.DataValidationException;
import uk.gov.moj.cpp.progression.exception.DocumentGeneratorException;
import uk.gov.moj.cpp.progression.exception.MissingRequiredFieldException;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.processor.summons.SummonsHearingRequestService;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.MessageService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.disqualificationreferral.ReferralDisqualifyWarningGenerationService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;
import uk.gov.moj.cpp.progression.transformer.ReferredCourtDocumentTransformer;
import uk.gov.moj.cpp.progression.transformer.ReferredProsecutionCaseTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S2789", "squid:S1135"})
public class CasesReferredToCourtProcessor {

    public static final String MATERIAL_ID = "materialId";
    public static final String COURT_DOCUMENT = "courtDocument";
    private static final String REFER_PROSECUTION_CASES_TO_COURT_REJECTED = "public.progression.refer-prosecution-cases-to-court-rejected";
    private static final String REFER_PROSECUTION_CASES_TO_COURT_ACCEPTED = "public.progression.refer-prosecution-cases-to-court-accepted";
    private static final String FOR_DISQUALIFICATION = "For disqualification";
    private static final String REASON = "reason";
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
    private ListCourtHearingTransformer listCourtHearingTransformer;
    @Inject
    private SummonsHearingRequestService summonsHearingRequestService;
    @Inject
    private DocumentGeneratorService documentGeneratorService;
    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    @Inject
    private ReferralDisqualifyWarningGenerationService referralDisqualifyWarningGenerationService;

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
     * 5) Determine that the offence mode of trial is correct for the initiation code. If the
     * initiation code is J or Z, the mode of trial cannot be Indictable or EitherWay. When this
     * occurs raise the public event progression.events.referProsecutionCasesToCourtRejected with
     * the relevant offenceId item that is not cannot be Indictable or EitherWay.
     * <p>
     * 6) When a case is received from SJP referral, a check must be performed to avoid the
     * duplicate.
     * <p>
     * 7) In case of SJP referrals, Post code is mandatory.
     * <p>
     * 8) private command progression.command.create-hearing-defendant-request is created to support
     * summons generation. This command contains List<ListDefendantRequest> which is the source for
     * Summons and this details to whom the summons will be generated and what type of Summons
     * required to be generated. This event information will be used to  generate Summons when
     * Hearing is confirmed from Listing context.
     * <p>
     * Once enrichment is complete, the event processor call a private command handler to sync it to
     * view store
     */


    @Handles("progression.event.cases-referred-to-court-v2")
    public void referSJPCasesToCourt(final JsonEnvelope jsonEnvelope) {
        final UUID hearingId = UUID.randomUUID();
        final JsonObject privateEventPayload = jsonEnvelope.payloadAsJsonObject();

        final SjpCourtReferral sjpCourtReferral = jsonObjectToObjectConverter.convert(privateEventPayload.getJsonObject("courtReferral"), SjpCourtReferral.class);
        final List<ProsecutionCase> prosecutionCases;
        final List<CourtDocument> courtDocuments;
        ListCourtHearing listCourtHearing;
        try {
            sjpCourtReferral.getProsecutionCases().forEach(referredProsecutionCase -> {
                searchForDuplicateCases(jsonEnvelope, referredProsecutionCase
                        .getProsecutionCaseIdentifier().getProsecutionAuthorityReference());
                searchForDuplicateCasesByUrn(jsonEnvelope, referredProsecutionCase
                        .getProsecutionCaseIdentifier().getCaseURN());
            });
            prosecutionCases = new ArrayList<>(convertToCourtReferral(jsonEnvelope, sjpCourtReferral));
            courtDocuments = new ArrayList<>(convertToCourtDocument(jsonEnvelope, sjpCourtReferral));

            listCourtHearing = prepareListCourtHearing(jsonEnvelope, sjpCourtReferral, prosecutionCases, hearingId);

            generateDisqualificationWarning(jsonEnvelope, sjpCourtReferral);

        } catch (final MissingRequiredFieldException | DataValidationException | ReferenceDataNotFoundException | DocumentGeneratorException e) {

            LOGGER.error("Transformation and enrichment exception", e);
            final ReferProsecutionCasesToCourtRejected referProsecutionCasesToCourtRejected = ReferProsecutionCasesToCourtRejected
                    .referProsecutionCasesToCourtRejected()
                    .withSjpCourtReferral(sjpCourtReferral)
                    .withRejectedReason(e.getMessage())
                    .build();
            messageService.sendMessage(jsonEnvelope, objectToJsonObjectConverter.convert(referProsecutionCasesToCourtRejected), REFER_PROSECUTION_CASES_TO_COURT_REJECTED);

            return;
        }

        final List<ListDefendantRequest> listDefendantRequests = sjpCourtReferral.getListHearingRequests().stream().map(ReferredListHearingRequest::getListDefendantRequests).flatMap(Collection::stream).collect(Collectors.toList());
        summonsHearingRequestService.addDefendantRequestToHearing(jsonEnvelope, listDefendantRequests, hearingId);

        progressionService.createProsecutionCases(jsonEnvelope, prosecutionCases);
        progressionService.createCourtDocument(jsonEnvelope, courtDocuments);

        listingService.listCourtHearing(jsonEnvelope, listCourtHearing);
        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);

        prosecutionCases.forEach(
                prosecutionCase -> {
                    final CaseLinkedToHearing caseLinkedToHearing = CaseLinkedToHearing.caseLinkedToHearing()
                            .withHearingId(hearingId)
                            .withCaseId(prosecutionCase.getId())
                            .build();

                    sender.send(Enveloper
                            .envelop(objectToJsonObjectConverter.convert(caseLinkedToHearing))
                            .withName("progression.command-link-prosecution-cases-to-hearing")
                            .withMetadataFrom(jsonEnvelope));
                });

        final JsonObject caseReferredForCourtAcceptedJson = createObjectBuilder()
                .add("caseId", listDefendantRequests.get(0).getProsecutionCaseId().toString())
                .add("referralReasonId", listDefendantRequests.get(0).getReferralReason().getId().toString())
                .build();

        messageService.sendMessage(jsonEnvelope, caseReferredForCourtAcceptedJson, REFER_PROSECUTION_CASES_TO_COURT_ACCEPTED);
    }


    @Handles("progression.event.cases-referred-to-court")
    public void process(final JsonEnvelope jsonEnvelope) {
        final UUID hearingId = UUID.randomUUID();
        final JsonObject privateEventPayload = jsonEnvelope.payloadAsJsonObject();

        final SjpCourtReferral sjpCourtReferral = jsonObjectToObjectConverter.convert(privateEventPayload.getJsonObject("courtReferral"), SjpCourtReferral.class);
        final List<ProsecutionCase> prosecutionCases;
        final List<CourtDocument> courtDocuments;
        ListCourtHearing listCourtHearing;
        try {
            sjpCourtReferral.getProsecutionCases().forEach(referredProsecutionCase -> {
                searchForDuplicateCases(jsonEnvelope, referredProsecutionCase
                        .getProsecutionCaseIdentifier().getProsecutionAuthorityReference());
                searchForDuplicateCasesByUrn(jsonEnvelope, referredProsecutionCase
                        .getProsecutionCaseIdentifier().getCaseURN());
            });
            prosecutionCases = new ArrayList<>(convertToCourtReferral(jsonEnvelope, sjpCourtReferral));
            courtDocuments = new ArrayList<>(convertToCourtDocument(jsonEnvelope, sjpCourtReferral));
            listCourtHearing = prepareListCourtHearing(jsonEnvelope, sjpCourtReferral, prosecutionCases, hearingId);

            generateDisqualificationWarning(jsonEnvelope, sjpCourtReferral);

        } catch (final MissingRequiredFieldException | DataValidationException | ReferenceDataNotFoundException | DocumentGeneratorException e) {
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
        summonsHearingRequestService.addDefendantRequestToHearing(jsonEnvelope, listDefendantRequests, hearingId);

        progressionService.createProsecutionCases(jsonEnvelope, prosecutionCases);
        progressionService.createCourtDocument(jsonEnvelope, courtDocuments);

        listingService.listCourtHearing(jsonEnvelope, listCourtHearing);
        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);

        prosecutionCases.forEach(
                prosecutionCase -> sender.send(enveloper.withMetadataFrom(jsonEnvelope, "progression.command-link-prosecution-cases-to-hearing")
                        .apply(CaseLinkedToHearing.caseLinkedToHearing()
                                .withHearingId(hearingId)
                                .withCaseId(prosecutionCase.getId())
                                .build())));

        //This is a temporary fix to update PCF that SJP case is referred to CC until ATCM has permanent fix. Referral reason has taken from first defendant as SJP deals with only one defendant.
        final JsonObject caseReferredForCourtAcceptedJson = createObjectBuilder()
                .add("caseId", listDefendantRequests.get(0).getProsecutionCaseId().toString())
                .add("referralReasonId", listDefendantRequests.get(0).getReferralReason().getId().toString())
                .build();

        messageService.sendMessage(jsonEnvelope, caseReferredForCourtAcceptedJson, REFER_PROSECUTION_CASES_TO_COURT_ACCEPTED);
    }

    private void generateDisqualificationWarning(final JsonEnvelope jsonEnvelope, final SjpCourtReferral sjpCourtReferral) {
        for (final ReferredListHearingRequest listHearingRequest : sjpCourtReferral.getListHearingRequests()) {
            listHearingRequest.getListDefendantRequests().forEach(listDefendantRequest ->
                    {
                        try {
                            processDisqualificationWarning(jsonEnvelope, sjpCourtReferral, listDefendantRequest);
                        } catch (IOException e) {
                            LOGGER.error("Referral DisqualificationWarning generate Pdf document failed ", e);
                            throw new DocumentGeneratorException();
                        }
                    }
            );
        }
    }

    private void processDisqualificationWarning(final JsonEnvelope jsonEnvelope, final SjpCourtReferral sjpCourtReferral, final ListDefendantRequest listDefendantRequest) throws IOException {
        final JsonObject referralReason = searchForReferralReason(jsonEnvelope, listDefendantRequest.getReferralReason().getId());
        if (nonNull(referralReason) && FOR_DISQUALIFICATION.equals(referralReason.getString(REASON))) {
                final String courtHouseCode = sjpCourtReferral.getSjpReferral().getReferringJudicialDecision().getCourtHouseCode();
                final Optional<ReferredProsecutionCase> prosecutionCase = sjpCourtReferral.getProsecutionCases().stream().filter(pc -> pc.getId().equals(listDefendantRequest.getProsecutionCaseId())).findFirst();
                if (prosecutionCase.isPresent()) {
                    processDisqualificationDocument(jsonEnvelope, listDefendantRequest, prosecutionCase.get(), courtHouseCode);
                }
        }
    }

    private void processDisqualificationDocument(final JsonEnvelope jsonEnvelope, final ListDefendantRequest listDefendantRequest, final ReferredProsecutionCase prosecutionCase, final String courtHouseCode) throws IOException {
        final Optional<ReferredDefendant> defendant = prosecutionCase.getDefendants().stream().filter(referredDefendant -> referredDefendant.getId().equals(listDefendantRequest.getReferralReason().getDefendantId())).findFirst();
        final String caseUrn = getCaseUrn(prosecutionCase.getProsecutionCaseIdentifier());
        final UUID caseId = prosecutionCase.getId();
        if (defendant.isPresent()) {
            LOGGER.info("ReferralDisqualifyWarning :::::::");
            referralDisqualifyWarningGenerationService.generateReferralDisqualifyWarning(jsonEnvelope, caseUrn, caseId, defendant.get(), courtHouseCode);
        }
    }

    private void searchForDuplicateCasesByUrn(final JsonEnvelope jsonEnvelope, final String reference) {
        if (StringUtils.isNotEmpty(reference)) {
            LOGGER.info("searchForDuplicateCasesByUrn {} : ", reference);
            final Optional<JsonObject> jsonObject = progressionService.caseExistsByCaseUrn(jsonEnvelope, reference);
            if (jsonObject.isPresent() && !jsonObject.get().isEmpty()) {
                throw new DataValidationException("Case has been already referred");
            }
        }
    }

    private void searchForDuplicateCases(final JsonEnvelope jsonEnvelope, final String reference) {
        if (StringUtils.isNotEmpty(reference)) {
            final Optional<JsonObject> jsonObject = progressionService.searchCaseDetailByReference(jsonEnvelope, reference);
            if (jsonObject.isPresent() && !jsonObject.get().getJsonArray("searchResults").isEmpty()) {
                throw new DataValidationException("Case has been already referred");
            }
        }
    }


    private ListCourtHearing prepareListCourtHearing(final JsonEnvelope jsonEnvelope, final SjpCourtReferral sjpCourtReferral, final List<ProsecutionCase> prosecutionCases, final UUID hearingId) {

        if(nonNull(sjpCourtReferral.getNextHearing())){
            return listCourtHearingTransformer.transformSjpReferralNextHearing(jsonEnvelope, prosecutionCases, hearingId, sjpCourtReferral.getNextHearing(), sjpCourtReferral.getListHearingRequests());
        } else{
            return listCourtHearingTransformer
                    .transform(jsonEnvelope, prosecutionCases, sjpCourtReferral.getSjpReferral(), sjpCourtReferral.getListHearingRequests(), hearingId);
        }
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
        final HearingLanguage hearingLanguage = sjpCourtReferral.getListHearingRequests().stream()
                .flatMap(l -> l.getListDefendantRequests().stream())
                .map(ListDefendantRequest::getHearingLanguageNeeds)
                .filter(HearingLanguage.WELSH::equals)
                .findFirst()
                .orElse(ENGLISH);

        return sjpCourtReferral.getProsecutionCases().stream()
                .map(referredProsecutionCase -> referredProsecutionCaseTransformer
                        .transform(referredProsecutionCase, hearingLanguage, jsonEnvelope)).collect(Collectors.toList());
    }

    private JsonObject searchForReferralReason(final JsonEnvelope jsonEnvelope, final UUID referenceId) {
        return progressionService.getReferralReasonByReferralReasonId(jsonEnvelope, referenceId);
    }

    private String getCaseUrn(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return nonNull(prosecutionCaseIdentifier.getProsecutionAuthorityReference()) ? prosecutionCaseIdentifier.getProsecutionAuthorityReference() : prosecutionCaseIdentifier.getCaseURN();
    }

}
