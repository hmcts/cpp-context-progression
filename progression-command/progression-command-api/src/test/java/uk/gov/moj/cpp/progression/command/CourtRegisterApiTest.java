package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.time.LocalDate;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtRegisterApiTest {
    private static final String ADD_COURT_REGISTER_NAME = "progression.add-court-register";
    private static final String ADD_COURT_REGISTER_COMMAND_NAME = "progression.command.add-court-register";
    private static final String GENERATE_COURT_REGISTER = "progression.generate-court-register";
    private static final String GENERATE_COURT_REGISTER_COMMAND_NAME = "progression.command.generate-court-register";
    private static final String GENERATE_COURT_REGISTER_BY_DATE = "progression.generate-court-register-by-date";
    private static final String GENERATE_COURT_REGISTER_BY_DATE_COMMAND_NAME = "progression.command.generate-court-register-by-date";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private CourtRegisterApi courtRegisterApi;

    @Test
    public void shouldHandleCourtRegisterDocumentRequestCommand() {
        assertThat(CourtRegisterApi.class, isHandlerClass(COMMAND_API)
                .with(method("handleAddCourtRegister").thatHandles(ADD_COURT_REGISTER_NAME)));
    }

    @Test
    public void shouldRecordCourtRegisterDocumentRequest() {

        final JsonEnvelope commandEnvelope = buildEnvelope();

        courtRegisterApi.handleAddCourtRegister(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(ADD_COURT_REGISTER_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void shouldGenerateCourtRegisterDocument() {
        final JsonEnvelope commandEnvelope = buildGenerateCourtRegisterEnvelope();
        courtRegisterApi.handleGenerateCourtRegister(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());
        final DefaultEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is(GENERATE_COURT_REGISTER_COMMAND_NAME));
        final JsonObject payloadAsJsonObject = commandEnvelope.payloadAsJsonObject();
        final JsonObject wrappedObject = JsonObjects.createObjectBuilder(payloadAsJsonObject).add("registerDate", LocalDate.now().toString()).build();
        assertThat(newCommand.payload(), equalTo(wrappedObject));
    }

    @Test
    public void shouldGenerateCourtRegisterDocumentByDate() {
        final JsonEnvelope commandEnvelope = buildGenerateCourtRegisterByDateEnvelope();
        courtRegisterApi.handleGenerateCourtRegisterByDate(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());
        final DefaultEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is(GENERATE_COURT_REGISTER_BY_DATE_COMMAND_NAME));
        final JsonObject payloadAsJsonObject = commandEnvelope.payloadAsJsonObject();
        assertThat(newCommand.payload(), equalTo(payloadAsJsonObject));
    }

    private JsonEnvelope buildGenerateCourtRegisterByDateEnvelope() {
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("registerDate", LocalDate.now().toString())
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(GENERATE_COURT_REGISTER_BY_DATE)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();
        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

    private JsonEnvelope buildGenerateCourtRegisterEnvelope() {
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(GENERATE_COURT_REGISTER)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();
        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("courtRegisterDocumentRequest", JsonObjects.createObjectBuilder().add("courtCentreId", randomUUID().toString()).build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_COURT_REGISTER_COMMAND_NAME)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }
}
