package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

@RunWith(MockitoJUnitRunner.class)
public class CompleteSendingSheetHandlerTest extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private CompleteSendingSheetHandler completeSendingSheetHandler;

    @Test
    public void shouldSendCompleteSendingSheet() throws EventStreamException {
        when(caseProgressionAggregate.completeSendingSheet(jsonEnvelope))
                        .thenReturn(events);
        completeSendingSheetHandler.completeSendingSheet(jsonEnvelope);
        verify(caseProgressionAggregate).completeSendingSheet(jsonEnvelope);
    }
}
