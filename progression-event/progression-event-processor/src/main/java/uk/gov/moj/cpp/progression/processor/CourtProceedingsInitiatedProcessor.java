package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.CreateHearingDefendantRequest;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

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

/**
 * This processor will split event progression.event.court-proceedings-initiated
 * ( Which is created from command progression.initiate-court-proceedings )
 * into ProsecutionCases , CourtDocuments and ListHearingRequests to individually
 * call private commands for each one of them
 */
@ServiceComponent(EVENT_PROCESSOR)
public class CourtProceedingsInitiatedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtProceedingsInitiatedProcessor.class.getCanonicalName());
    private static final String PROGRESSION_COMMAND_CREATE_HEARING_DEFENDANT_REQUEST = "progression.command.create-hearing-defendant-request";

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
    private ProgressionService progressionService;
    @Inject
    private ListCourtHearingTransformer listCourtHearingTransformer;



    @Handles("progression.event.court-proceedings-initiated")
    public void handle(final JsonEnvelope jsonEnvelope) {
        final JsonObject event = jsonEnvelope.payloadAsJsonObject();
        final UUID hearingId = randomUUID();
        final CourtReferral courtReferral = jsonObjectToObjectConverter.convert(
                event.getJsonObject("courtReferral"), CourtReferral.class);

        final List<ListDefendantRequest> listDefendantRequests = courtReferral.getListHearingRequests().stream().map(ListHearingRequest::getListDefendantRequests).flatMap(Collection::stream).collect(Collectors.toList());
        final JsonObject hearingDefendantRequestJson = objectToJsonObjectConverter.convert(CreateHearingDefendantRequest.createHearingDefendantRequest()
                .withHearingId(hearingId)
                .withDefendantRequests(listDefendantRequests)
                .build());
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_CREATE_HEARING_DEFENDANT_REQUEST).apply(hearingDefendantRequestJson));

        progressionService.createProsecutionCases(
                jsonEnvelope, getProsecutionCasesList(jsonEnvelope, courtReferral.getProsecutionCases()));
        if (nonNull(courtReferral.getCourtDocuments())) {
            progressionService.createCourtDocument(
                    jsonEnvelope,
                    courtReferral.getCourtDocuments());
        }
        listingService.listCourtHearing(jsonEnvelope,
                listCourtHearingTransformer.transform(jsonEnvelope, courtReferral.getProsecutionCases(), courtReferral.getListHearingRequests(), hearingId));
    }

    private List<ProsecutionCase> getProsecutionCasesList(final JsonEnvelope jsonEnvelope, final List<ProsecutionCase> prosecutionCases) {
        return prosecutionCases.stream().filter(pCase -> (isNewCase(jsonEnvelope, pCase
                .getProsecutionCaseIdentifier().getProsecutionAuthorityReference())
                && isNewCase(jsonEnvelope, pCase
                .getProsecutionCaseIdentifier().getCaseURN()))).collect(toList());
    }

    private boolean isNewCase(final JsonEnvelope jsonEnvelope, final String reference) {
        if (StringUtils.isNotEmpty(reference)) {
            final Optional<JsonObject> jsonObject = progressionService.searchCaseDetailByReference(jsonEnvelope, reference);
            if (jsonObject.isPresent() && !jsonObject.get().getJsonArray("searchResults").isEmpty()) {
                LOGGER.info("Prosecution case {} already exists ", reference);
                return false;
            }
        }
        return true;
    }
}
