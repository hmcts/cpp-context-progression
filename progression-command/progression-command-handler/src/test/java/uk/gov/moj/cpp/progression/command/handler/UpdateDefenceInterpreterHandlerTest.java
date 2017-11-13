package uk.gov.moj.cpp.progression.command.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantInterpreter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefenceInterpreterHandlerTest extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private UpdateDefendantInterpreterHandler handler;

    @Mock
    private UpdateDefendantInterpreter updateDefendantInterpreter;

    @Test
    public void shouldUpdateDefendantInterpreter() throws EventStreamException {
        when(converter.convert(jsonObject, UpdateDefendantInterpreter.class))
                .thenReturn(updateDefendantInterpreter);
        when(caseProgressionAggregate.updateDefendantInterpreter(updateDefendantInterpreter))
                .thenReturn(events);

        handler.updateInterpreterForDefendant(jsonEnvelope);

        verify(converter).convert(jsonObject, UpdateDefendantInterpreter.class);
        verify(caseProgressionAggregate).updateDefendantInterpreter(updateDefendantInterpreter);
    }

}
