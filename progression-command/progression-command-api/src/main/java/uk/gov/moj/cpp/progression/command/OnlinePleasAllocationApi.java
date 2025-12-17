package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class OnlinePleasAllocationApi {

    private static final String TRIGGER_DATE = "triggerDate";
    private static final String REQUEST_OPA_PUBLIC_LIST_NOTICE = "progression.command.request-opa-public-list-notice";
    private static final String REQUEST_OPA_PRESS_LIST_NOTICE = "progression.command.request-opa-press-list-notice";
    private static final String REQUEST_OPA_RESULT_LIST_NOTICE = "progression.command.request-opa-result-list-notice";

    @Inject
    private Sender sender;

    @Handles("progression.request-opa-public-list-notice")
    public void handleRequestOpaPublicListNotice(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = buildWrappedPayload(jsonEnvelope.payloadAsJsonObject());

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(REQUEST_OPA_PUBLIC_LIST_NOTICE).build(), payload));
    }

    @Handles("progression.request-opa-press-list-notice")
    public void handleRequestOpaPressListNotice(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = buildWrappedPayload(jsonEnvelope.payloadAsJsonObject());

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(REQUEST_OPA_PRESS_LIST_NOTICE).build(), payload));
    }

    @Handles("progression.request-opa-result-list-notice")
    public void handleRequestOpaResultListNotice(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = buildWrappedPayload(jsonEnvelope.payloadAsJsonObject());

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(REQUEST_OPA_RESULT_LIST_NOTICE).build(), payload));
    }

    private JsonObject buildWrappedPayload(final JsonObject payload) {
        if (!getString(payload, TRIGGER_DATE).isPresent()) {
            return JsonObjects.createObjectBuilder(payload).add(TRIGGER_DATE, LocalDate.now().toString()).build();
        }

        return payload;
    }
}
