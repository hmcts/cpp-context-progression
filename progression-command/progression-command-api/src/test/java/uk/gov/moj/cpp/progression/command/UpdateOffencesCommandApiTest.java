package uk.gov.moj.cpp.progression.command;

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
public class UpdateOffencesCommandApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private UpdateOffencesCommandApi updateOffencesCommand;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Test
    public void shouldUpdateOffences() {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.update-offences-for-prosecution-case"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        updateOffencesCommand.handle(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

    @Test
    public void shouldUpdateDefendantOffences() {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.update-defendant-offences"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        updateOffencesCommand.handleUpdateDefendantOffences(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

    @Test
    public void shouldUpdatePlea()
    {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.update-hearing-offence-plea"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        updateOffencesCommand.handleUpdatePlea(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

    @Test
    public void shouldUpdateVerdict()
    {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.update-hearing-offence-verdict"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        updateOffencesCommand.handleUpdateVerdict(command);

        verify(sender, times(1)).send(commandEnvelope);
    }
}
