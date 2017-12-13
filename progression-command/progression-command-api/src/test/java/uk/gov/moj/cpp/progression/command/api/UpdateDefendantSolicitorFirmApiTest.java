package uk.gov.moj.cpp.progression.command.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefendantSolicitorFirmApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @InjectMocks
    private UpdateDefendantSolicitorFirm api;

    @Test
    public void shouldUpdateSolicitorFirmForDefendant() {
        api.updateSolicitorFirm(command);
        verify(sender).send(command);
    }
}
