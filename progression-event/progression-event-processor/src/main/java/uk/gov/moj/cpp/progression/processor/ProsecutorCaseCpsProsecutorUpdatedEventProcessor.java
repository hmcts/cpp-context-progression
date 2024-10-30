package uk.gov.moj.cpp.progression.processor;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.helper.JsonHelper;
import uk.gov.moj.cpp.progression.events.CaseCpsProsecutorUpdated;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@ServiceComponent(Component.EVENT_PROCESSOR)
public class ProsecutorCaseCpsProsecutorUpdatedEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutorCaseCpsProsecutorUpdatedEventProcessor.class);

    @Inject
    private Sender sender;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public static final String HEARINGS = "hearings";


    @Handles("progression.event.case-cps-prosecutor-updated")
    public void processCpsProsecutorUpdated(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "progression.event.case-cps-prosecutor-updated", event.toObfuscatedDebugString());
        }

        if(event.payloadAsJsonObject().get("isCpsOrgVerifyError") != null && event.payloadAsJsonObject().getBoolean("isCpsOrgVerifyError")){
            return;
        }

        final JsonObject payload = createPublicEventPayload(event);
        sender.send(Enveloper.envelop(payload).withName("public.progression.events.cps-prosecutor-updated").withMetadataFrom(event));
    }

    private JsonArray getHearingIdsForCase(final JsonEnvelope event) {
        final String prosecutionCaseId = event.payloadAsJsonObject().getString("prosecutionCaseId");
        final JsonArrayBuilder hearingIdsBuilder = Json.createArrayBuilder();
        progressionService.getCaseHearings(prosecutionCaseId).ifPresent(caseHearingsResponse -> caseHearingsResponse.getJsonArray(HEARINGS)
        .getValuesAs(JsonObject.class).stream().map(hearing ->
                jsonObjectToObjectConverter.convert(hearing, Hearings.class))
                .map(Hearings::getId)
                .collect(toList()).forEach(hearingId -> hearingIdsBuilder.add(hearingId.toString())));
        return hearingIdsBuilder.build();
    }

    private JsonObject createPublicEventPayload(JsonEnvelope event) {

        final JsonObject payload = event.payloadAsJsonObject();

        final CaseCpsProsecutorUpdated publicCaseCpsProsecutorUpdated = jsonObjectToObjectConverter.convert(payload, CaseCpsProsecutorUpdated.class);

        final JsonObject publicEventPayload = objectToJsonObjectConverter.convert(publicCaseCpsProsecutorUpdated);
        final JsonArray hearingIds = getHearingIdsForCase(event);
        return JsonHelper.addProperty(publicEventPayload, "hearingIds", hearingIds);
    }
}
