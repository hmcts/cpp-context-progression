package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.command.api.UserDetailsLoader;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Mock
    private UserDetailsLoader userDetailsLoader;

    @Mock
    private Requester requester;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
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

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(ADD_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payload()));
    }

    @Test(expected = BadRequestException.class)
    public void shouldThroughBadRequest() {
        final JsonEnvelope commandEnvelope = buildEnvelopeWithResource(randomUUID().toString());
        addCourtDocumentApi.handleAddCourtDocumentForDefence(commandEnvelope);
    }

    @Test(expected = ForbiddenRequestException.class)
    public void shouldThroughForbiddenRequest() {
        final JsonEnvelope commandEnvelope = buildEnvelopeWithResource("e1d32d9d-29ec-4934-a932-22a50f223966");
        when(userDetailsLoader.isPermitted(any(), any())).thenReturn(false);
        addCourtDocumentApi.handleAddCourtDocumentForDefence(commandEnvelope);
    }


    @Test
    public void shouldProcessRequestForDefence() {
        final JsonEnvelope commandEnvelope = buildEnvelopeWithResource("e1d32d9d-29ec-4934-a932-22a50f223966");
        when(userDetailsLoader.isPermitted(any(), any())).thenReturn(true);
        addCourtDocumentApi.handleAddCourtDocumentForDefence(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(ADD_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(expected().payload()));
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

    private JsonEnvelope buildEnvelopeWithResource(String defendantId) {
        final JsonObject payload = CommandClientTestBase.readJson("json/add-court-document.json", JsonObject.class);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_COURT_DOCUMENT_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, Json.createObjectBuilder().add("courtDocument", payload).add("defendantId", defendantId).build());
    }

    private JsonEnvelope expected() {
        final JsonObject payload = CommandClientTestBase.readJson("json/add-court-document.json", JsonObject.class);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_COURT_DOCUMENT_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, Json.createObjectBuilder().add("courtDocument", payload).build());
    }
}
