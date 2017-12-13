package uk.gov.moj.cpp.progression.command.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefendantAllocationDecisionHandlerTest  extends CaseProgressionCommandHandlerTest {
    @InjectMocks
    private UpdateDefendantAllocationDecisionHandler handler;

    @Test
    public void shouldUpdateDefendantAllocationDecision() throws EventStreamException {
        when(caseProgressionAggregate.updateDefendantAllocationDecision(jsonEnvelope)).thenReturn(events);

        handler.updateDefendantAllocationDecision(jsonEnvelope);

        verify(caseProgressionAggregate).updateDefendantAllocationDecision(jsonEnvelope);
    }
}