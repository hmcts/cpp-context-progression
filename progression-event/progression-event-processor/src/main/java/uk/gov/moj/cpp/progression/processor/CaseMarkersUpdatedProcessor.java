package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;


import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import uk.gov.justice.core.courts.CaseMarkersSharedWithHearings;
import uk.gov.justice.core.courts.CaseMarkersUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.progression.service.ProgressionService;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S2789", "squid:CallToDeprecatedMethod"})
public class CaseMarkersUpdatedProcessor {
    private static final String CASE_MARKER_UPDATED = "public.progression.case-markers-updated";
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseMarkersUpdated.class);
    private static final String RECEIVED_EVENT_WITH_PAYLOAD = "Received '{}' event with payload {}";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.event.case-markers-updated")
    public void processCaseMarkerUpdated(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.case-markers-updated", event.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(event, CASE_MARKER_UPDATED).apply(event.payloadAsJsonObject()));
    }

    @Handles("progression.event.case-markers-shared-with-hearings")
    public void processCaseMarkerSharedWithHearings(final JsonEnvelope event){
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.case-markers-shared-to-hearings", event.toObfuscatedDebugString());
        }
        final JsonObject privateEventPayload = event.payloadAsJsonObject();
        final CaseMarkersSharedWithHearings caseMarkersSharedWithHearings = jsonObjectToObjectConverter.convert(privateEventPayload, CaseMarkersSharedWithHearings.class);

        caseMarkersSharedWithHearings.getHearingIds().forEach( hearingId ->
                sender.send(envelop(Json.createObjectBuilder()
                        .add("prosecutionCaseId", caseMarkersSharedWithHearings.getProsecutionCaseId().toString())
                        .add("hearingId", hearingId.toString())
                        .add("caseMarkers", privateEventPayload.get("caseMarkers"))
                        .build()).withName("progression.command.update-case-markers-to-hearing").withMetadataFrom(event))
        );


    }


    @Handles("progression.event.case-markers-updated-in-hearing")
    public void processCaseMarkerUpdateInHearing(final JsonEnvelope event){
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.case-markers-updated-in-hearing", event.toObfuscatedDebugString());
        }

        progressionService.populateHearingToProbationCaseworker(event, UUID.fromString(event.payloadAsJsonObject().getString("hearingId")));
    }
}
