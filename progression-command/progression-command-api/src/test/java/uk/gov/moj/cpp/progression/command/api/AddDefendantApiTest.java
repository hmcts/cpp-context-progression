package uk.gov.moj.cpp.progression.command.api;

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
@Deprecated
@ExtendWith(MockitoExtension.class)
public class AddDefendantApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private AddDefendantApi addDefendantApi;

    @Mock
    private Function<Object, JsonEnvelope> function;



    @Test
    public void shouldAddDefendant() throws Exception {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.record-add-defendant"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        addDefendantApi.addDefendant(command);

        verify(sender, times(1)).send(commandEnvelope);
    }
}
