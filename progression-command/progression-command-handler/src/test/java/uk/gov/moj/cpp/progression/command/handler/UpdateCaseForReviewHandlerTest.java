package uk.gov.moj.cpp.progression.command.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class UpdateCaseForReviewHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private UpdateCaseForReviewHandler updateCaseForReviewHandler;

    @Test
    public void shouldUpdateCaseForReview() throws EventStreamException {

        when(caseProgressionAggregate.caseAssignedForReview(jsonEnvelope))
                        .thenReturn(events);

        updateCaseForReviewHandler.updateCaseAssignedForReview(jsonEnvelope);
        verify(caseProgressionAggregate).caseAssignedForReview(jsonEnvelope);
    }
}
