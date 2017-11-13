package uk.gov.moj.cpp.progression.command.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @RunWith(MockitoJUnitRunner.class)
    public static class AddDefendantApiTest {

        @Mock
        private Sender sender;

        @Mock
        private JsonEnvelope command;

        @InjectMocks
        private AddDefendantApi addDefendantApi;

        @Test
        public void shouldAddDefendant() {
            addDefendantApi.addDefendant(command);
            verify(sender).send(command);
        }
    }
}
