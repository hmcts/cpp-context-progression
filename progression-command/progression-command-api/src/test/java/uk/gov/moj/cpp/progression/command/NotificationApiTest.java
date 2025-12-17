package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationApiTest {
    @Mock
    private Sender sender;

    @InjectMocks
    private NotificationApi notificationApi;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleSendEmail() {
        //Given
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("applicationId", randomUUID().toString())
                .add("materialId", randomUUID().toString())
                .add("notifications", createArrayBuilder().add(createObjectBuilder()
                        .add("notificationId", randomUUID().toString())
                        .add("templateId", randomUUID().toString())
                        .add("sendToAddress", "sendToAddress")
                        .build()))
                .build();

        final JsonEnvelope commandEnvelope = createNotificationCommandEnvelope("progression.send.email", payload);
        //When
        notificationApi.handleSendEmail(commandEnvelope);
        //Then
        verifyNotificationResults(payload, "progression.command.email");
    }

    @Test
    public void shouldHandleSendPrint() {
        //Given
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("notificationId", randomUUID().toString())
                .add("materialId", randomUUID().toString())
                .add("postage", false)
                .build();

        final JsonEnvelope commandEnvelope = createNotificationCommandEnvelope("progression.send.print", payload);
        //When
        notificationApi.handleSendPrint(commandEnvelope);
        //Then
        verifyNotificationResults(payload, "progression.command.print");
    }

    @Test
    public void shouldHandleSendToCps() {
        //Given
        final JsonObject payload = Json.createObjectBuilder()
                .add("courtDocumentId", randomUUID().toString())
                .add("sendToCps", true)
                .build();

        final JsonEnvelope commandEnvelope = createNotificationCommandEnvelope("progression.update-send-to-cps-flag", payload);
        //When
        notificationApi.handleSendToCps(commandEnvelope);
        //Then
        verifyNotificationResults(payload, "progression.command.update-send-to-cps-flag");
    }

    private void verifyNotificationResults(final JsonObject payload, final String commandName) {
        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is(commandName));
        assertThat(capturedEnvelope.payload(), is(payload));
    }

    private JsonEnvelope createNotificationCommandEnvelope(final String commandName, final JsonObject payload) {
        final UUID uuid = randomUUID();
        final UUID userId = randomUUID();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(commandName)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();
        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }
}