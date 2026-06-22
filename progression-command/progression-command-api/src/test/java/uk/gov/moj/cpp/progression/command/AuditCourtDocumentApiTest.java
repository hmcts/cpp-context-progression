package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuditCourtDocumentApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private AuditCourtDocumentApi auditCourtDocumentApi;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Test
    public void whenRequestReceivedPassItToCommandHandler() {

        final UUID uuid = randomUUID();
        final UUID userId = randomUUID();

        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("organisationId", randomUUID().toString())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.audit-court-document")
                .withId(uuid)
                .withUserId(userId.toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        auditCourtDocumentApi.handle(commandEnvelope);
        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.audit-court-document"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }

}
