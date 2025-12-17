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
public class EjectCaseApplicationApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private EjectCaseApplicationApi ejectCaseApplicationApi;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Test
    public void shouldEjectCaseApplication() {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.eject-case-or-application"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        ejectCaseApplicationApi.handle(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

    @Test
    public void shouldEjectCaseViaBDF() {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.eject-case-via-bdf"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        ejectCaseApplicationApi.handleForBdf(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

}
