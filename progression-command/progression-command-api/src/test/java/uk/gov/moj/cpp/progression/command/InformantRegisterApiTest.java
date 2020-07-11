package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.time.LocalDate;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InformantRegisterApiTest {

    private static final String ADD_INFORMANT_REGISTER_REQUEST_NAME = "progression.add-informant-register";
    private static final String ADD_INFORMANT_REGISTER_COMMAND_NAME = "progression.command.add-informant-register";
    private static final String GENERATE_INFORMANT_REGISTER_REQUEST_NAME = "progression.generate-informant-register";
    private static final String GENERATE_INFORMANT_REGISTER_COMMAND_NAME = "progression.command.generate-informant-register";
    private static final String GENERATE_INFORMANT_REGISTER_BY_DATE_REQUEST_NAME = "progression.generate-informant-register-by-date";
    private static final String GENERATE_INFORMANT_REGISTER_BY_DATE_COMMAND_NAME = "progression.command.generate-informant-register-by-date";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private InformantRegisterApi informantRegisterApi;

    @Test
    public void shouldHandleAddInformantRegisterCommand() {
        assertThat(InformantRegisterApi.class, isHandlerClass(COMMAND_API)
                .with(method("handleAddInformantRegister").thatHandles(ADD_INFORMANT_REGISTER_REQUEST_NAME)));
    }

    @Test
    public void shouldRecordInformantRegisterRequest() {

        final JsonEnvelope commandEnvelope = buildEnvelope();

        informantRegisterApi.handleAddInformantRegister(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(ADD_INFORMANT_REGISTER_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void shouldGenerateInformantRegister() {

        final JsonEnvelope commandEnvelope = buildGenerateInformantRegisterEnvelope(GENERATE_INFORMANT_REGISTER_REQUEST_NAME);

        informantRegisterApi.handleGenerateInformantRegister(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(GENERATE_INFORMANT_REGISTER_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void shouldGenerateInformantRegisterByDate() {

        final JsonEnvelope commandEnvelope = buildGenerateInformantRegisterEnvelope(GENERATE_INFORMANT_REGISTER_BY_DATE_REQUEST_NAME);

        informantRegisterApi.handleGenerateInformantRegisterByDate(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(GENERATE_INFORMANT_REGISTER_BY_DATE_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("informantRegisterRequest", Json.createObjectBuilder().add("prosecutionAuthorityId", randomUUID().toString()).build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_INFORMANT_REGISTER_REQUEST_NAME)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

    private JsonEnvelope buildGenerateInformantRegisterEnvelope(final String commandName) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("registerDate", LocalDate.now().toString())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(commandName)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }
}
