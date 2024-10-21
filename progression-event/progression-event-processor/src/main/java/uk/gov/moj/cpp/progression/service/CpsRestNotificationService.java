package uk.gov.moj.cpp.progression.service;

import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.UpdateSendToCpsFlag;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpsRestNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsRestNotificationService.class);

    @Inject
    @Value(key = "cpsPayloadTransformAndSendUrl", defaultValue = "http://localhost:8080/notification-cms/v1/transformAndSendCms")
    private String cpsPayloadTransformAndSendUrl;

    @Inject
    @Value(key = "progressionCourtDocument.subscription.key", defaultValue = "3674a16507104b749a76b29b6c837352")
    private String subscriptionKey;

    @Inject
    private RestEasyClientService restEasyClientService;

    @Inject
    private MaterialService materialService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public void sendMaterial(final String payload, final UUID courtDocumentId, final JsonEnvelope envelope) {
        final Response response = restEasyClientService.post(cpsPayloadTransformAndSendUrl, payload, subscriptionKey);
        LOGGER.info("API-M {} called with Request: {} and received status response: {}", cpsPayloadTransformAndSendUrl, payload, response.getStatus());

        final JsonObject event = envelope.payloadAsJsonObject();
        final CourtDocument courtDocument = jsonObjectConverter.convert(event.getJsonObject("courtDocument"), CourtDocument.class);

        UpdateSendToCpsFlag.Builder updateSendToCpsFlagBuilder = UpdateSendToCpsFlag.updateSendToCpsFlag()
                .withCourtDocumentId(courtDocumentId).withCourtDocument(courtDocument);

        if (HttpStatus.SC_OK == response.getStatus()) {
            updateSendToCpsFlagBuilder = updateSendToCpsFlagBuilder.withSendToCps(true);
        }

        sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("progression.command.update-send-to-cps-flag").build(),
                this.objectToJsonObjectConverter.convert(updateSendToCpsFlagBuilder.build())));
    }
}