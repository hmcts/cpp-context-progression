package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingNowsEventProcessorTest {

    private static final String PROGRESSION_COMMAND_FOR_NOW_NOTIFICATION_GENERATED = "progression.command.record-now-notification-generated";
    private final UUID ID = UUID.randomUUID();

    @Mock
    private Sender sender;
    @InjectMocks
    private HearingNowsEventProcessor hearingNowsEventProcessor;

    @Test
    public void shouldProcessNowNotificationGeneratedEvent() {
        final JsonObject requestJson = Json.createObjectBuilder().add("key", "value").build();
        final Metadata metadata = metadataFrom(Json.createObjectBuilder().add("id", ID.toString()).build())
                .withName(PROGRESSION_COMMAND_FOR_NOW_NOTIFICATION_GENERATED)
                .build();
        final JsonEnvelope event = envelopeFrom(metadata, requestJson);

        hearingNowsEventProcessor.processNowNotificationGeneratedEvent(event);

        ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender).send(captor.capture());

        JsonEnvelope capturedEvent = captor.getValue();
        assertThat(capturedEvent.metadata().id(), is(metadata.id()));
        assertThat(capturedEvent.metadata().name(), is(PROGRESSION_COMMAND_FOR_NOW_NOTIFICATION_GENERATED));
        assertThat(capturedEvent.payloadAsJsonObject(), is(requestJson));
    }
}
