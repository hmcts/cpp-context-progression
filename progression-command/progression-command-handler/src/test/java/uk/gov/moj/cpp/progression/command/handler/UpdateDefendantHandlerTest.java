package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantCommand;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@Deprecated
@RunWith(MockitoJUnitRunner.class)
public class UpdateDefendantHandlerTest extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private UpdateDefendantHandler updateDefendantHandler;

    @Mock
    private UpdateDefendantCommand defendant;

    @Test
    public void shouldAddDefendant() throws EventStreamException {
        when(converter.convert(jsonObject, UpdateDefendantCommand.class)).thenReturn(defendant);
        when(caseAggregate.updateDefendant(defendant)).thenReturn(events);

        updateDefendantHandler.updateDefendant(jsonEnvelope);

        verify(converter).convert(jsonObject, UpdateDefendantCommand.class);
        verify(caseAggregate).updateDefendant(defendant);
    }

}
