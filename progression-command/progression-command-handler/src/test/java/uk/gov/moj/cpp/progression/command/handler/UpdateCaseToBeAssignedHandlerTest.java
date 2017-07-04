package uk.gov.moj.cpp.progression.command.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class UpdateCaseToBeAssignedHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private UpdateCaseToBeAssignedHandler updateCaseToBeAssignedHandler;

    @Test
    public void shouldUpdateCaseToBeAssigned() throws EventStreamException {

        when(caseProgressionAggregate.caseToBeAssigned(jsonEnvelope))
                        .thenReturn(events);

        updateCaseToBeAssignedHandler.updateCaseToBeAssigned(jsonEnvelope);
        verify(caseProgressionAggregate).caseToBeAssigned(jsonEnvelope);
    }
}
