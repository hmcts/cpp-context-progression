package uk.gov.moj.cpp.progression.command.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefendantAllocationDecisionApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @InjectMocks
    private UpdateDefendantAllocationDecision updateDefendantAllocationDecision;


    @Mock
    private Enveloper enveloper;

    @Mock
    private Function<Object, JsonEnvelope> function;



    @Test
    public void shouldAllocationDecisionForDefendant() throws Exception {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.handler.update-allocation-decision-for-defendant"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        updateDefendantAllocationDecision.updateAllocationDecision(command);

        verify(sender, times(1)).send(commandEnvelope);
    }
}
