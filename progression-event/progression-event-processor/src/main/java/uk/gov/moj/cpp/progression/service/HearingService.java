package uk.gov.moj.cpp.progression.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ApplicationScoped
public class HearingService {
    private static final String HEARING_EVENT_LOG_DOCUMENT = "hearing.get-hearing-event-log-for-documents";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public JsonObject getHearingEventLogs(final JsonEnvelope envelope, final UUID caseId, final Optional<String> applicationId) {
        JsonObjectBuilder query;

        if(applicationId.isPresent() && !applicationId.get().isEmpty()) {
            query = createObjectBuilder()
                    .add("applicationId", applicationId.get());
        } else {
            query = createObjectBuilder()
                    .add("caseId", caseId.toString());
        }

        final Envelope<JsonObject> requestEnvelop = envelop(query.build())
                .withName(HEARING_EVENT_LOG_DOCUMENT)
                .withMetadataFrom(envelope);

        final Envelope<JsonObject> jsonObjectEnvelope = requester.request(requestEnvelop, JsonObject.class);
        return jsonObjectEnvelope.payload();
    }
}
