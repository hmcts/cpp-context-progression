package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class UpdateCourtDocumentApiTest {

    private static final String UPDATE_COURT_DOCUMENT_NAME = "progression.update-court-document";
    private static final String UPDATE_COURT_DOCUMENT_COMMAND_NAME = "progression.command.update-court-document";
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();
    @Mock
    private Sender sender;
    @InjectMocks
    private UpdateCourtDocumentApi updateCourtDocumentApi;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    private UUID uuid;
    private UUID userId;

    @BeforeEach
    public void setUp() throws Exception {
        uuid = randomUUID();
        userId = randomUUID();
    }

    @Test
    public void shouldHandleUpdateDocumentCommand() {
        assertThat(UpdateCourtDocumentApi.class, isHandlerClass(COMMAND_API)
                .with(method("handle").thatHandles(UPDATE_COURT_DOCUMENT_NAME)));
    }

    @Test
    public void shouldUpdateDocument() {

        final JsonEnvelope commandEnvelope = buildEnvelope();


        updateCourtDocumentApi.handle(commandEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final Envelope newCommand = envelopeArgumentCaptor.getValue();

        assertThat(newCommand.metadata().name(), equalTo(UPDATE_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payload()));
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = Json.createObjectBuilder().build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(UPDATE_COURT_DOCUMENT_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

}
