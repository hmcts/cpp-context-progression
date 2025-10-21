package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.DefendantsToRemove;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequested;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.listing.courts.HearingPartiallyUpdated;
import uk.gov.justice.listing.events.HearingRequestedForListing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingNotificationHelper;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.dto.HearingNotificationInputData;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ListHearingRequestedProcessor {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ListHearingRequestedProcessor.class);

    private static final String NEW_HEARING_NOTIFICATION_TEMPLATE_NAME = "NewHearingNotification";

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ListingService listingService;

    @Inject
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private HearingNotificationHelper hearingNotificationHelper;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;


    @Handles("progression.event.list-hearing-requested")
    public void handle(final JsonEnvelope jsonEnvelope) {

        final ListHearingRequested listHearingRequested = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ListHearingRequested.class);

        final ListCourtHearing listCourtHearing = convertListCourtHearing(listHearingRequested, jsonEnvelope);

        final JsonObject eventPayload = jsonEnvelope.payloadAsJsonObject();
        final boolean sendNotification = eventPayload.getBoolean("sendNotificationToParties", false);

        listingService.listCourtHearing(jsonEnvelope, listCourtHearing);

        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);

        if (sendNotification) {
            sendHearingNotificationsToDefenceAndProsecutor(jsonEnvelope, listHearingRequested);
        } else {
            LOGGER.info("Notification is not sent for HearingId {}  , Notification sent flag {}", listHearingRequested.getHearingId(), false);
        }
    }

    @Handles("public.listing.hearing-requested-for-listing")
    public void handlePublicEvent(final JsonEnvelope jsonEnvelope) {
        final HearingRequestedForListing hearingRequestedForListing = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingRequestedForListing.class);

        final CourtCentre enrichedCourtCentre = progressionService.transformCourtCentre(hearingRequestedForListing.getListNewHearing().getCourtCentre(), jsonEnvelope);

        final HearingRequestedForListing enrichedHearingRequestedForListing = HearingRequestedForListing.hearingRequestedForListing()
                .withValuesFrom(hearingRequestedForListing)
                .withListNewHearing(CourtHearingRequest.courtHearingRequest()
                        .withValuesFrom(hearingRequestedForListing.getListNewHearing())
                        .withCourtCentre(enrichedCourtCentre)
                        .build())
                .build();

        sender.send(
                Enveloper.envelop(objectToJsonObjectConverter.convert(enrichedHearingRequestedForListing))
                        .withName("progression.command.list-new-hearing")
                        .withMetadataFrom(jsonEnvelope));

    }

    @Handles("public.listing.hearing-partially-updated")
    public void handlePublicEventForPartiallyUpdate(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("Handling public.listing.hearing-partially-updated {}", jsonEnvelope.payload());

        HearingPartiallyUpdated hearingPartiallyUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingPartiallyUpdated.class);
        UpdateHearingForPartialAllocation updateHearingForPartialAllocation = UpdateHearingForPartialAllocation.updateHearingForPartialAllocation()
                .withHearingId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProsecutionCasesToRemove(hearingPartiallyUpdated.getProsecutionCases().stream()
                        .map(prosecutionCase -> ProsecutionCasesToRemove.prosecutionCasesToRemove()
                                .withCaseId(prosecutionCase.getCaseId())
                                .withDefendantsToRemove(prosecutionCase.getDefendants().stream()
                                        .map(defendant -> DefendantsToRemove.defendantsToRemove()
                                                .withDefendantId(defendant.getDefendantId())
                                                .withOffencesToRemove(defendant.getOffences().stream()
                                                        .map(offence -> OffencesToRemove.offencesToRemove()
                                                                .withOffenceId(offence.getOffenceId())
                                                                .build())
                                                        .collect(Collectors.toList()))
                                                .build())
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build();

        sender.send(
                Enveloper.envelop(objectToJsonObjectConverter.convert(updateHearingForPartialAllocation))
                        .withName("progression.command.update-hearing-for-partial-allocation")
                        .withMetadataFrom(jsonEnvelope));
    }

    private ListCourtHearing convertListCourtHearing(ListHearingRequested listHearingRequested, final JsonEnvelope jsonEnvelope) {
        final Set<UUID> caseIds = listHearingRequested.getListNewHearing().getListDefendantRequests().stream()
                .map(ListDefendantRequest::getProsecutionCaseId)
                .collect(Collectors.toSet());

        final List<ProsecutionCase> cases = caseIds.stream().map(caseId -> progressionService.getProsecutionCaseDetailById(jsonEnvelope, caseId.toString()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class))
                .collect(Collectors.toList());

        return listCourtHearingTransformer.transform(jsonEnvelope, cases, listHearingRequested.getListNewHearing(), listHearingRequested.getHearingId());
    }

    private void sendHearingNotificationsToDefenceAndProsecutor(final JsonEnvelope jsonEnvelope, final ListHearingRequested listHearingRequested) {

        final HearingNotificationInputData hearingNotificationInputData = new HearingNotificationInputData();
        final Set<UUID> caseIds = listHearingRequested.getListNewHearing().getListDefendantRequests().stream()
                .map(ListDefendantRequest::getProsecutionCaseId)
                .collect(Collectors.toSet());
        hearingNotificationInputData.setCaseIds(new ArrayList<>(caseIds));

        final Set<UUID> defendantIdSet = new HashSet<>();
        final Map<UUID, List<UUID>> defendantOffenceListMap = new HashMap<>();
        listHearingRequested.getListNewHearing().getListDefendantRequests()
                .forEach(listDef -> {
                    defendantIdSet.add(listDef.getDefendantId());
                    defendantOffenceListMap.put(listDef.getDefendantId(), listDef.getDefendantOffences());
                });
        hearingNotificationInputData.setDefendantIds(new ArrayList<>(defendantIdSet));
        hearingNotificationInputData.setDefendantOffenceListMap(defendantOffenceListMap);
        hearingNotificationInputData.setTemplateName(NEW_HEARING_NOTIFICATION_TEMPLATE_NAME);

        hearingNotificationInputData.setHearingId(listHearingRequested.getHearingId());
        hearingNotificationInputData.setHearingDateTime(hearingNotificationHelper.getEarliestStartDateTime(listHearingRequested.getListNewHearing().getEarliestStartDateTime()));
        hearingNotificationInputData.setEmailNotificationTemplateId(fromString(applicationParameters.getNotifyHearingTemplateId()));
        hearingNotificationInputData.setCourtCenterId(listHearingRequested.getListNewHearing().getCourtCentre().getId());
        hearingNotificationInputData.setCourtRoomId(listHearingRequested.getListNewHearing().getCourtCentre().getRoomId());
        hearingNotificationInputData.setHearingType(listHearingRequested.getListNewHearing().getHearingType().getDescription());

        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, hearingNotificationInputData);

    }

    public ListHearingRequestedProcessor() {
        super();
    }

}