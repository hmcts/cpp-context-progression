package uk.gov.moj.cpp.progression.processor;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings("squid:CallToDeprecatedMethod")
public class CaseApplicationEjectedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseApplicationEjectedEventProcessor.class.getCanonicalName());
    private static final String CASE_OR_APPLICATION_EJECTED = "public.progression.events.case-or-application-ejected";
    private static final String APPLICATION_ID = "applicationId";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String REMOVAL_REASON = "removalReason";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("progression.event.case-ejected")
    public void processCaseEjected(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "progression.event.case-ejected", event.toObfuscatedDebugString());
        }
        final JsonArray hearingIds = getHearingIdsForCaseAllApplications(event);
        final JsonObject payload = event.payloadAsJsonObject();
        final String removalReason = payload.getString(REMOVAL_REASON);
        sendPublicMessage(event, hearingIds, payload.getString(PROSECUTION_CASE_ID), PROSECUTION_CASE_ID, removalReason);
    }

    @Handles("progression.event.application-ejected")
    public void processApplicationEjected(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "progression.event.application-ejected", event.toObfuscatedDebugString());
        }
        final JsonObject payload = event.payloadAsJsonObject();
        final String applicationId = payload.getString(APPLICATION_ID);

        final JsonArray hearingIds = getHearingIdsForAllApplications(event, applicationId);
        final String removalReason = payload.getString(REMOVAL_REASON);
        sendPublicMessage(event, hearingIds, applicationId, APPLICATION_ID, removalReason);
    }

    private JsonArray getHearingIdsForCaseAllApplications(final JsonEnvelope event) {
        final String prosecutionCaseId = event.payloadAsJsonObject().getString(PROSECUTION_CASE_ID);
        final JsonArrayBuilder hearingIdsBuilder = Json.createArrayBuilder();
        progressionService.getProsecutionCaseDetailById(event, prosecutionCaseId).ifPresent(prosecutionCaseJsonObject -> {
            final GetCaseAtAGlance caseAtAGlance = jsonObjectToObjectConverter.
                    convert(prosecutionCaseJsonObject.getJsonObject("caseAtAGlance"),
                            GetCaseAtAGlance.class);
            if (isNotEmpty(caseAtAGlance.getHearings())) {
                caseAtAGlance.getHearings().stream().forEach(hearing -> hearingIdsBuilder.add(hearing.getId().toString()));
            }
        });
        return hearingIdsBuilder.build();
    }

    private void addHearingIds(JsonArray hearingIds, JsonObjectBuilder payloadBuilder) {
         if (isNotEmpty(hearingIds)) {
            payloadBuilder.add("hearingIds", hearingIds);
        }
    }

    private JsonArray getHearingIdsForAllApplications(final JsonEnvelope event, final String applicationId) {
        final JsonArrayBuilder hearingIdsBuilder = Json.createArrayBuilder();
        progressionService.getCourtApplicationById(event, applicationId).ifPresent(applicationAtAGlance -> {
            final JsonArray hearings = applicationAtAGlance.getJsonArray("hearings");

            if (isNotEmpty(hearings)) {
                hearings.getValuesAs(JsonObject.class).stream()
                        .forEach(hearing -> hearingIdsBuilder.add(hearing.getString("id")));
            }
        });
        return hearingIdsBuilder.build();
    }

    public void sendPublicMessage(final JsonEnvelope event, final JsonArray hearingIds, final String id, final String idKey, final String removalReason) {
        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        payloadBuilder.add(idKey, id);
        payloadBuilder.add(REMOVAL_REASON, removalReason);
        addHearingIds(hearingIds, payloadBuilder);
        sender.send(enveloper.withMetadataFrom(event, CASE_OR_APPLICATION_EJECTED).apply(payloadBuilder.build()));
    }
}
