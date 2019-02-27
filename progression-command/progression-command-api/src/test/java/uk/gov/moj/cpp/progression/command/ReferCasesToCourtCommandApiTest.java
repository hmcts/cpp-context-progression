package uk.gov.moj.cpp.progression.command;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReferCasesToCourtCommandApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private ReferCasesToCourtCommandApi referCasesToCourtCommandApi;

    @Mock
    private Function<Object, JsonEnvelope> function;



    @Test
    public void shouldAddDefendant() throws Exception {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.refer-cases-to-court"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        referCasesToCourtCommandApi.handle(command);

        verify(sender, times(1)).send(commandEnvelope);
    }
}
