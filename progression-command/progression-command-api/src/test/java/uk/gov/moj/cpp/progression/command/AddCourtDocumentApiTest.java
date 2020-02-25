package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class AddCourtDocumentApiTest {

    private static final String ADD_COURT_DOCUMENT_NAME = "progression.add-court-document";
    private static final String ADD_COURT_DOCUMENT_COMMAND_NAME = "progression.command.add-court-document";
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();
    @Mock
    private Sender sender;
    @InjectMocks
    private AddCourtDocumentApi addCourtDocumentApi;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;
    private UUID uuid;
    private UUID userId;
    private UUID docTypeId;

    @Before
    public void setUp() throws Exception {
        uuid = randomUUID();
        userId = randomUUID();
        docTypeId = randomUUID();
    }

    @Test
    public void shouldHandleAddDocumentCommand() {
        assertThat(AddCourtDocumentApi.class, isHandlerClass(COMMAND_API)
                .with(method("handle").thatHandles(ADD_COURT_DOCUMENT_NAME)));
    }

    @Test
    public void shouldAddDocument() {

        final JsonEnvelope commandEnvelope = buildEnvelope();


        addCourtDocumentApi.handle(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(commandEnvelope).withName(ADD_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("courtDocument", Json.createObjectBuilder().add("documentTypeId", docTypeId.toString()).build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_COURT_DOCUMENT_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

}
