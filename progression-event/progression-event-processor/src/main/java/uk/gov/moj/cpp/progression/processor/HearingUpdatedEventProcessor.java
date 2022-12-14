package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.AllHearingOffencesUpdated;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.core.courts.HearingUpdatedProcessed;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("squid:S1168")
@ServiceComponent(EVENT_PROCESSOR)
public class HearingUpdatedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingUpdatedEventProcessor.class.getName());

    private static final String PROGRESSION_COMMAND_PROCESS_HEARING_UPDATED = "progression.command.process-hearing-updated";
    public static final String HEARING_ID = "hearingId";

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    @Handles("public.listing.hearing-updated")
    public void processHearingUpdated(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("public.listing.hearing-updated event received with metadata {} and payload {}", jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        final HearingUpdated hearingUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingUpdated.class);
        final ConfirmedHearing confirmedHearing = hearingUpdated.getUpdatedHearing();
        final List<UUID> courtApplicationIds = hearingUpdated.getUpdatedHearing().getCourtApplicationIds();
        final Optional<JsonObject> hearingPayloadOptional = progressionService.getHearing(jsonEnvelope, hearingUpdated.getUpdatedHearing().getId().toString());
        final JsonObject hearingJson = hearingPayloadOptional.orElseThrow(() -> new RuntimeException("Hearing not found")).getJsonObject("hearing");
        final Hearing hearingEntity = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        Hearing updatedHearing = progressionService.updateHearingForHearingUpdated(hearingUpdated.getUpdatedHearing(), jsonEnvelope, hearingEntity);
        if(HearingListingStatus.SENT_FOR_LISTING.toString().equals(hearingPayloadOptional.map(jsonObject -> jsonObject.getString("hearingListingStatus")).orElse(""))){
            updatedHearing = Hearing.hearing().withValuesFrom(updatedHearing)
                    .withProsecutionCases(removeJudicialResults(updatedHearing.getProsecutionCases()))
                    .build();
        }
        if (isNotEmpty(courtApplicationIds)) {
            progressionService.linkApplicationsToHearing(jsonEnvelope, updatedHearing, courtApplicationIds, HearingListingStatus.HEARING_INITIALISED);
            if (isNotEmpty(hearingUpdated.getUpdatedHearing().getProsecutionCases())) {
                progressionService.updateHearingListingStatusToHearingUpdate(jsonEnvelope, updatedHearing);
            }
            progressionService.publishHearingDetailChangedPublicEvent(jsonEnvelope, confirmedHearing);
        } else {
            sender.send(envelop(
                    createObjectBuilder()
                            .add("confirmedHearing", objectToJsonObjectConverter.convert(hearingUpdated.getUpdatedHearing()))
                            .add("updatedHearing", objectToJsonObjectConverter.convert(updatedHearing))
                            .build())
                    .withName(PROGRESSION_COMMAND_PROCESS_HEARING_UPDATED)
                    .withMetadataFrom(jsonEnvelope));
        }
    }

    @Handles("progression.event.hearing-updated-processed")
    public void publishHearingDetailChangedPublicEvent(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.event.hearing-updated-processed event received with  {}",  jsonEnvelope.toObfuscatedDebugString());
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
    public void handlerHearingChangedToProbationCaseWorker(final JsonEnvelope event){
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("public.events.listing.hearing-days-without-court-centre-corrected event received with  {}",  event.toObfuscatedDebugString());
        }

        sender.send(envelop( event.payloadAsJsonObject())
                        .withName("progression.command.correct-hearing-days-without-court-centre")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.all-hearing-offences-updated")
    public void handleAllHearingOffenceUpdated(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.event.all-hearing-offences-updated event received with  {}", event.toObfuscatedDebugString());
        }
        final JsonObject privateEventPayload = event.payloadAsJsonObject();
        final AllHearingOffencesUpdated allHearingOffencesUpdated = jsonObjectToObjectConverter.convert(privateEventPayload, AllHearingOffencesUpdated.class);
        allHearingOffencesUpdated.getHearingIds().forEach(hearingId ->
                sender.send(envelop(Json.createObjectBuilder()
                        .add("defendantId", allHearingOffencesUpdated.getDefendantId().toString())
                        .add(HEARING_ID, hearingId.toString())
                        .add("updatedOffences", privateEventPayload.get("updatedOffences"))
                        .build()).withName("progression.command.update-offences-for-hearing").withMetadataFrom(event)));
    }

    @Handles("progression.event.hearing-offences-updated")
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
    public void hearingOffenceVerdictUpdated(final JsonEnvelope event){
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("public.hearing.hearing-offence-verdict-updated with  {}", event.toObfuscatedDebugString());
        }
        final JsonObject privateEventPayload = event.payloadAsJsonObject();
        sender.send(envelop(privateEventPayload).withName("progression.command.update-hearing-offence-verdict").withMetadataFrom(event));
    }

    private List<ProsecutionCase> removeJudicialResults(List<ProsecutionCase> prosecutionCases){
        if(isEmpty(prosecutionCases)){
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
