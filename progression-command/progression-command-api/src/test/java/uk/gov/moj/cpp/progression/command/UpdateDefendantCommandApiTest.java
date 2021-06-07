package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class UpdateDefendantCommandApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private UpdateDefendantCommandApi updateDefendantCommand;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Test
    public void shouldUpdateDefendant() {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("prosecutionCaseId", randomUUID().toString())
                .build());
        when(enveloper.withMetadataFrom(command, "progression.command.update-defendant-for-prosecution-case"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        updateDefendantCommand.handle(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

    @Test
    public void shouldUpdateMatchedDefendant() {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder()
                .add("prosecutionCaseId", randomUUID().toString())
                .add("matchedDefendantHearingId", randomUUID().toString())
                .build());
        when(enveloper.withMetadataFrom(command, "progression.command.update-defendant-for-matched-defendant"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        updateDefendantCommand.handle(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

}
