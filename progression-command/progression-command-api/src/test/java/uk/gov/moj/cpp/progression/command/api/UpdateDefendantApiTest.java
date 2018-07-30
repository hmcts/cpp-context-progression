package uk.gov.moj.cpp.progression.command.api;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefendantApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @InjectMocks
    private UpdateDefendantApi updateDefendantApi;

    @Test
    public void shouldUpdateDefendant() {
        updateDefendantApi.updateDefendant(command);
        verify(sender, times(1)).send(command);
    }

}
