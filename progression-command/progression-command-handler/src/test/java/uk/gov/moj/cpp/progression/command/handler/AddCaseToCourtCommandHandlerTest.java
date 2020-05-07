package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@Deprecated
@RunWith(MockitoJUnitRunner.class)
public class AddCaseToCourtCommandHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private AddCaseToCrownCourtHandler addCaseToCrownCourtHandler;

    @Test
    public void shouldAddCaseToCourt() throws EventStreamException {

        when(caseAggregate.addCaseToCrownCourt(jsonEnvelope))
                        .thenReturn(events);

        addCaseToCrownCourtHandler.addCaseToCrownCourt(jsonEnvelope);
        verify(caseAggregate).addCaseToCrownCourt(jsonEnvelope);
    }
}
