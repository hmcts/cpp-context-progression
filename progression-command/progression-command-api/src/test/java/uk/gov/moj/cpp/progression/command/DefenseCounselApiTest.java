package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefenseCounselApiTest {
    private static final String ADD_HEARING_DEFENCE_COUNSEL_NAME = "progression.add-hearing-defence-counsel";
    private static final String ADD_COMMAND_HEARING_DEFENCE_COUNSEL_NAME = "progression.command.handler.add-hearing-defence-counsel";
    private static final String UPDATE_HEARING_DEFENCE_COUNSEL_NAME = "progression.update-hearing-defence-counsel";
    private static final String UPDATE_COMMAND_HEARING_DEFENCE_COUNSEL_NAME = "progression.command.handler.update-hearing-defence-counsel";
    private static final String REMOVE_HEARING_DEFENCE_COUNSEL_NAME = "progression.remove-hearing-defence-counsel";
    private static final String REMOVE_COMMAND_HEARING_DEFENCE_COUNSEL_NAME = "progression.command.handler.remove-hearing-defence-counsel";

    @InjectMocks
    private DefenseCounselApi defenseCounselApi;

    private UUID uuid;
    private UUID userId;

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @BeforeEach
    public void setUp() {
        uuid = randomUUID();
        userId = randomUUID();
    }

    @Test
    public void shouldHandleAddDefenceCounselCommand() {
        assertThat(DefenseCounselApi.class, isHandlerClass(COMMAND_API)
                .with(method("handleAddDefenceCounsel").thatHandles(ADD_HEARING_DEFENCE_COUNSEL_NAME)));
    }

    @Test
    public void shouldAddDefenceCounsel() {
        final JsonObject payload = CommandClientTestBase.readJson("json/progression.add-hearing-defence-counsel.json", JsonObject.class);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_HEARING_DEFENCE_COUNSEL_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();
        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        when(enveloper.withMetadataFrom(command, ADD_COMMAND_HEARING_DEFENCE_COUNSEL_NAME))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        defenseCounselApi.handleAddDefenceCounsel(command);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.payload(), notNullValue());
    }

    @Test
    public void shouldUpdateHearingDefenceCounsel() {
        final JsonObject payload = CommandClientTestBase.readJson("json/progression.add-hearing-defence-counsel.json", JsonObject.class);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(UPDATE_HEARING_DEFENCE_COUNSEL_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();
        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        when(enveloper.withMetadataFrom(command, UPDATE_COMMAND_HEARING_DEFENCE_COUNSEL_NAME))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        defenseCounselApi.handleUpdateDefenseCounsel(command);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.payload(), notNullValue());
    }

    @Test
    public void shouldRemoveHearingDefenceCounsel() {
        final JsonObject payload = CommandClientTestBase.readJson("json/progression.remove-hearing-defence-counsel.json", JsonObject.class);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(REMOVE_HEARING_DEFENCE_COUNSEL_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();
        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        when(enveloper.withMetadataFrom(command, REMOVE_COMMAND_HEARING_DEFENCE_COUNSEL_NAME))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        defenseCounselApi.handleRemoveDefenseCounsel(command);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.payload(), notNullValue());
    }
}
