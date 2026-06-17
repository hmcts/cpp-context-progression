package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.service.ProgressionService.getEarliestDate;

import uk.gov.justice.core.courts.AllHearingOffencesUpdatedV2;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.core.courts.HearingUpdatedProcessed;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.event.ApplicationHearingDefendantUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingNotificationHelper;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.dto.HearingNotificationInputData;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1168")
@ServiceComponent(EVENT_PROCESSOR)
public class HearingUpdatedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingUpdatedEventProcessor.class.getName());

    private static final String PROGRESSION_COMMAND_PROCESS_HEARING_UPDATED = "progression.command.process-hearing-updated";
    public static final String HEARING_ID = "hearingId";
    private static final String AMENDED_HEARING_NOTIFICATION_TEMPLATE_NAME = "AmendedHearingNotification";
    public static final String PROGRESSION_COMMAND_UPDATE_APPLICATION_DEFENDANT = "progression.command.update-application-defendant";
    public static final String UPDATED_OFFENCES = "updatedOffences";
    public static final String NEW_OFFENCES = "newOffences";

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingNotificationHelper hearingNotificationHelper;

    @Inject
    private ApplicationParameters applicationParameters;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    @Handles("public.listing.hearing-updated")
    public void processHearingUpdated(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("public.listing.hearing-updated event received with metadata {} and payload {}", jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        final HearingUpdated hearingUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingUpdated.class);
        final ConfirmedHearing confirmedUpdatedHearing = hearingUpdated.getUpdatedHearing();
        boolean sendNotificationToParties = false;
        if (nonNull(hearingUpdated.getSendNotificationToParties())) {
            sendNotificationToParties = hearingUpdated.getSendNotificationToParties();
        }

        boolean isNotificationAllocationFieldUpdated = false;
        if (nonNull(hearingUpdated.getIsNotificationAllocationFieldUpdated())) {
            isNotificationAllocationFieldUpdated = hearingUpdated.getIsNotificationAllocationFieldUpdated();
        }

        final List<UUID> courtApplicationIds = hearingUpdated.getUpdatedHearing().getCourtApplicationIds();
        final UUID hearingId = hearingUpdated.getUpdatedHearing().getId();
        final Optional<JsonObject> hearingPayloadOptional = progressionService.getHearing(jsonEnvelope, hearingId.toString());

        if (hearingPayloadOptional.isEmpty() || !hearingPayloadOptional.get().containsKey("hearing")) {
            LOGGER.error("Hearing not found for id:{}", hearingId);
            return;
        }
        JsonObject hearing = hearingPayloadOptional.get().get("hearing").asJsonObject();
        final Hearing hearingEntity = jsonObjectToObjectConverter.convert(hearing, Hearing.class);
        Hearing updatedHearing = progressionService.updateHearingForHearingUpdated(hearingUpdated.getUpdatedHearing(), jsonEnvelope, hearingEntity);
        if (HearingListingStatus.SENT_FOR_LISTING.toString().equals(hearingPayloadOptional.map(jsonObject -> jsonObject.getString("hearingListingStatus")).orElse(""))) {
            updatedHearing = Hearing.hearing().withValuesFrom(updatedHearing)
                    .withProsecutionCases(removeJudicialResults(updatedHearing.getProsecutionCases()))
                    .build();
        }
        if (isNotEmpty(courtApplicationIds)) {
            progressionService.linkApplicationsToHearing(jsonEnvelope, updatedHearing, courtApplicationIds, HearingListingStatus.HEARING_INITIALISED);
            if (isNotEmpty(hearingUpdated.getUpdatedHearing().getProsecutionCases())) {
                progressionService.updateHearingListingStatusToHearingUpdate(jsonEnvelope, updatedHearing);
            }
            progressionService.publishHearingDetailChangedPublicEvent(jsonEnvelope, confirmedUpdatedHearing);
        } else {

            final ConfirmedHearing confirmedHearing = hearingUpdated.getUpdatedHearing();
            final boolean prosecutionCasesOrDefendantsOrOffencesNullOrEmpty = isProsecutionCasesOrDefendantsOrOffencesNullOrEmpty(updatedHearing.getProsecutionCases());

            if (prosecutionCasesOrDefendantsOrOffencesNullOrEmpty) {
                LOGGER.error("Invalid Schema for event:{}, prosecutionCasesOrDefendantsOrOffencesNullOrEmpty:{}",
                        PROGRESSION_COMMAND_PROCESS_HEARING_UPDATED, prosecutionCasesOrDefendantsOrOffencesNullOrEmpty);
                return;
            }

            sender.send(envelop(
                    createObjectBuilder()
                            .add("confirmedHearing", objectToJsonObjectConverter.convert(confirmedHearing))
                            .add("updatedHearing", objectToJsonObjectConverter.convert(updatedHearing))
                            .build())
                    .withName(PROGRESSION_COMMAND_PROCESS_HEARING_UPDATED)
                    .withMetadataFrom(jsonEnvelope));
        }
        if (sendNotificationToParties && isNotificationAllocationFieldUpdated) {
            sendHearingNotificationsToDefenceAndProsecutor(jsonEnvelope, confirmedUpdatedHearing, updatedHearing);
        } else {
            LOGGER.info("Notification is not sent for HearingId {}  , Notification flag {} and notificationAllocatedFieldsUpdated flag {}", updatedHearing.getId(), sendNotificationToParties, isNotificationAllocationFieldUpdated);
        }
    }

    protected boolean isProsecutionCasesOrDefendantsOrOffencesNullOrEmpty(List<ProsecutionCase> prosecutionCases) {
        if (prosecutionCases == null || prosecutionCases.isEmpty()) {
            return true;
        }

        return prosecutionCases.stream()
                .anyMatch(ca ->
                        ca.getDefendants() == null ||
                                ca.getDefendants().isEmpty() ||
                                ca.getDefendants().stream()
                                        .anyMatch(de -> de.getOffences() == null || de.getOffences().isEmpty()));
    }

    private void sendHearingNotificationsToDefenceAndProsecutor(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedUpdatedHearing, Hearing updatedHearing) {
        final HearingNotificationInputData hearingNotificationInputData = new HearingNotificationInputData();

        final Set<UUID> caseIds = new HashSet<>();
        final Map<UUID, List<UUID>> defendantOffenceListMap = new HashMap<>();
        final Set<UUID> defendantIdSet = new HashSet<>();

        if (isNotEmpty(confirmedUpdatedHearing.getProsecutionCases())) {
            confirmedUpdatedHearing.getProsecutionCases().stream().forEach(prosecutionCase -> {
                caseIds.add(prosecutionCase.getId());
                prosecutionCase.getDefendants().stream()
                        .forEach(defendant -> {
                            defendantIdSet.add(defendant.getId());
                            defendantOffenceListMap.put(defendant.getId(),
                                    defendant.getOffences().stream()
                                            .map(ConfirmedOffence::getId)
                                            .collect(toList()));
                        });
            });
        } else if (isNotEmpty(updatedHearing.getCourtApplications())) {
            updatedHearing.getCourtApplications().stream().filter(courtApplication -> isNotEmpty(courtApplication.getCourtApplicationCases()))
                    .flatMap(courtApplication -> courtApplication.getCourtApplicationCases().stream())
                    .forEach(courtApplicationCase -> caseIds.add(courtApplicationCase.getProsecutionCaseId()));

            caseIds.stream().forEach(applicationCaseId -> {
                final JsonObject prosecutionCaseJson = progressionService.getProsecutionCaseById(jsonEnvelope, applicationCaseId.toString());
                if (nonNull(prosecutionCaseJson)) {
                    final ProsecutionCase prosecutionCaseEntity = jsonObjectToObjectConverter.convert(prosecutionCaseJson.getJsonObject("prosecutionCase"), ProsecutionCase.class);
                    prosecutionCaseEntity.getDefendants().stream().forEach(defendant -> {
                        defendantIdSet.add(defendant.getId());
                        defendantOffenceListMap.put(defendant.getId(),
                                defendant.getOffences().stream()
                                        .map(Offence::getId)
                                        .collect(toList()));
                    });
                }
            });
        }

        final ZonedDateTime hearingStartDateTime = getEarliestDate(confirmedUpdatedHearing.getHearingDays());

        hearingNotificationInputData.setHearingType(confirmedUpdatedHearing.getType().getDescription());
        hearingNotificationInputData.setCaseIds(new ArrayList<>(caseIds));
        hearingNotificationInputData.setDefendantIds(new ArrayList<>(defendantIdSet));
        hearingNotificationInputData.setDefendantOffenceListMap(defendantOffenceListMap);
        hearingNotificationInputData.setTemplateName(AMENDED_HEARING_NOTIFICATION_TEMPLATE_NAME);
        hearingNotificationInputData.setHearingId(confirmedUpdatedHearing.getId());
        hearingNotificationInputData.setHearingDateTime(hearingNotificationHelper.getEarliestStartDateTime(hearingStartDateTime));
        hearingNotificationInputData.setEmailNotificationTemplateId(fromString(applicationParameters.getNotifyHearingTemplateId()));
        hearingNotificationInputData.setCourtCenterId(confirmedUpdatedHearing.getCourtCentre().getId());
        hearingNotificationInputData.setCourtRoomId(confirmedUpdatedHearing.getCourtCentre().getRoomId());

        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, hearingNotificationInputData);
    }

    @Handles("progression.event.hearing-updated-processed")
    public void publishHearingDetailChangedPublicEvent(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.event.hearing-updated-processed event received with  {}", jsonEnvelope.toObfuscatedDebugString());
        }

        final HearingUpdatedProcessed hearingUpdatedProcessed = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingUpdatedProcessed.class);
        final ConfirmedHearing confirmedHearing = hearingUpdatedProcessed.getConfirmedHearing();
        final Hearing updatedHearing = progressionService.updateHearingForHearingUpdated(confirmedHearing, jsonEnvelope, hearingUpdatedProcessed.getHearing());

        if (isNotEmpty(confirmedHearing.getProsecutionCases())) {
            progressionService.updateHearingListingStatusToHearingUpdate(jsonEnvelope, updatedHearing);
        }
        progressionService.publishHearingDetailChangedPublicEvent(jsonEnvelope, confirmedHearing);
        progressionService.populateHearingToProbationCaseworker(jsonEnvelope, confirmedHearing.getId());
    }

    @Handles("public.events.listing.hearing-days-without-court-centre-corrected")
    public void handlerHearingChangedToProbationCaseWorker(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("public.events.listing.hearing-days-without-court-centre-corrected event received with  {}", event.toObfuscatedDebugString());
        }

        sender.send(envelop(event.payloadAsJsonObject())
                .withName("progression.command.correct-hearing-days-without-court-centre")
                .withMetadataFrom(event));
    }

    @Handles("progression.event.all-hearing-offences-updated-v2")
    public void handleAllHearingOffenceUpdated(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.event.all-hearing-offences-updated event received with  {}", event.toObfuscatedDebugString());
        }
        final JsonObject privateEventPayload = event.payloadAsJsonObject();
        final AllHearingOffencesUpdatedV2 allHearingOffencesUpdated = jsonObjectToObjectConverter.convert(privateEventPayload, AllHearingOffencesUpdatedV2.class);


        allHearingOffencesUpdated.getHearingIds().forEach(hearingId -> {
                   final JsonObjectBuilder payload = JsonObjects.createObjectBuilder()
                           .add("defendantId", allHearingOffencesUpdated.getDefendantId().toString())
                           .add(HEARING_ID, hearingId.toString());
                   if(privateEventPayload.containsKey(UPDATED_OFFENCES)){
                       payload.add(UPDATED_OFFENCES, privateEventPayload.get(UPDATED_OFFENCES));
                   }
                   if(privateEventPayload.containsKey(NEW_OFFENCES)){
                       payload.add(NEW_OFFENCES, privateEventPayload.get(NEW_OFFENCES));
                   }
                    sender.send(envelop(payload.build()).withName("progression.command.update-offences-for-hearing").withMetadataFrom(event));
                }
        );
    }

    @Handles("progression.event.hearing-offences-updated-v2")
    public void handleHearingOffenceUpdated(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.event.hearing-offences-updated event received with  {}", event.toObfuscatedDebugString());
        }
        final JsonObject privateEventPayload = event.payloadAsJsonObject();
        progressionService.populateHearingToProbationCaseworker(event, fromString(privateEventPayload.getString(HEARING_ID)));
    }

    @Handles("progression.event.new-defendant-added-to-hearing")
    public void addedNewDefendantToHearing(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.event.new-defendant-added-to-hearing event received with  {}", event.toObfuscatedDebugString());
        }
        final JsonObject privateEventPayload = event.payloadAsJsonObject();
        progressionService.populateHearingToProbationCaseworker(event, fromString(privateEventPayload.getString(HEARING_ID)));
    }

    @Handles("public.hearing.hearing-offence-verdict-updated")
    public void hearingOffenceVerdictUpdated(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("public.hearing.hearing-offence-verdict-updated with  {}", event.toObfuscatedDebugString());
        }
        final JsonObject privateEventPayload = event.payloadAsJsonObject();
        sender.send(envelop(privateEventPayload).withName("progression.command.update-hearing-offence-verdict").withMetadataFrom(event));
    }

    @Handles("progression.event.application-hearing-defendant-updated")
    public void processUpdateDefendantOnApplicationHearing(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event in processor progression.event.application-hearing-defendant-updated {} ", event.toObfuscatedDebugString());
        }
        final ApplicationHearingDefendantUpdated hearingDefendantUpdated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ApplicationHearingDefendantUpdated.class);
        final List<CourtApplication> courtApplications = hearingDefendantUpdated.getHearing().getCourtApplications();
        final DefendantUpdate updatedDefendant = hearingDefendantUpdated.getDefendant();
        final UUID updatedDefendantId = ofNullable(updatedDefendant.getMasterDefendantId()).orElse(updatedDefendant.getId());

        final List<CourtApplication> updatedCourtApplications = courtApplications.stream().filter(application ->
                (nonNull(application.getApplicant()) && nonNull(application.getApplicant().getMasterDefendant())
                        && updatedDefendantId.equals(application.getApplicant().getMasterDefendant().getMasterDefendantId())) ||
                        (nonNull(application.getSubject()) && nonNull(application.getSubject().getMasterDefendant())
                                && updatedDefendantId.equals(application.getSubject().getMasterDefendant().getMasterDefendantId())))
                .collect(toList());

        ofNullable(updatedCourtApplications).ifPresent(applications -> applications.forEach(application ->
            sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PROGRESSION_COMMAND_UPDATE_APPLICATION_DEFENDANT).build()
                    , createObjectBuilder().add("courtApplication", objectToJsonObjectConverter.convert(application)).build()))
        ));
    }

    private List<ProsecutionCase> removeJudicialResults(List<ProsecutionCase> prosecutionCases) {
        if (isEmpty(prosecutionCases)) {
            return null;
        }
        return prosecutionCases.stream().map(prosecutionCase ->
                ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                        .withDefendants(prosecutionCase.getDefendants().stream()
                                .map(defendant -> Defendant.defendant().withValuesFrom(defendant)
                                        .withOffences(defendant.getOffences().stream()
                                                .map(offence -> Offence.offence().withValuesFrom(offence)
                                                        .withJudicialResults(null)
                                                        .build())
                                                .collect(toList()))
                                        .build())
                                .collect(toList()))
                        .build())
                .collect(toList());
    }
}
