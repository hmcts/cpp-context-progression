package uk.gov.moj.cpp.progression.command.api;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
@Deprecated
@RunWith(MockitoJUnitRunner.class)
public class UpdateOffencesForDefendantApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @InjectMocks
    private UpdateOffencesForDefendantApi updateOffencesForDefendantApi;

    @Test
    public void shouldUpdateOffencesForDefendant() {
        updateOffencesForDefendantApi.updateOffencesForDefendant(command);
        verify(sender, times(1)).send(command);
    }

}
