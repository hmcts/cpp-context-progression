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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InactiveCaseApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private InactiveProsecutionCaseApi inactiveProsecutionCaseApi;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Test
    public void shouldInactiveProsecutionCase() {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.inactive-case-bdf"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        inactiveProsecutionCaseApi.handleCaseInactiveViaBdf(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

}
