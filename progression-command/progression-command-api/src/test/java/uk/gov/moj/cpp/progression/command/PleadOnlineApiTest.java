package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleadOnlineApiTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @InjectMocks
    private PleadOnlineApi pleadOnlineApi;

    @Test
    public void shouldHandlePleadOnline() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.plead-online")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, createPayload());

        pleadOnlineApi.handlePleadOnline(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.plead-online"));
        assertThat(capturedEnvelope.payload(), is(createPayload()));
    }

    @Test
    public void shouldHandlePleadOnlinePCQVisited() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.plead-online-pcq-visited")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, createPayload());

        pleadOnlineApi.handlePleadOnlinePCQVisited(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.plead-online-pcq-visited"));
        assertThat(capturedEnvelope.payload(), is(createPayload()));
    }

    private JsonObject createPayload(){
        return createObjectBuilder()
                .add("caseId", createObjectBuilder().build())
                .add("defendantId", createObjectBuilder().build())
                .build();
    }
}