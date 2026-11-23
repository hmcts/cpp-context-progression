package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MoveGroupCasesApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private MoveGroupCasesApi moveGroupCasesApi;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleMoveGroupCases() {

        final JsonObject payload = createObjectBuilder()
                .add("currentGroup", createObjectBuilder()
                        .add("groupId", randomUUID().toString())
                        .build())
                .add("newGroup", createObjectBuilder()
                        .add("groupId", randomUUID().toString())
                        .add("leadCaseId", randomUUID().toString())
                        .build())
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.move-group-cases")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        moveGroupCasesApi.handle(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.move-group-cases"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }
}
