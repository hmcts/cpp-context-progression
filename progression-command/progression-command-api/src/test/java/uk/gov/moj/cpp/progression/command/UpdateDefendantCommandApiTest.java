package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
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
