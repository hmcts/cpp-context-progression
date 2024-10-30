package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
@Deprecated
@ExtendWith(MockitoExtension.class)
public class CompleteSendingSheetHandlerTest extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private CompleteSendingSheetHandler completeSendingSheetHandler;

    @Test
    public void shouldSendCompleteSendingSheet() throws EventStreamException {
        when(caseAggregate.completeSendingSheet(jsonEnvelope))
                        .thenReturn(events);
        completeSendingSheetHandler.completeSendingSheet(jsonEnvelope);
        verify(caseAggregate).completeSendingSheet(jsonEnvelope);
    }
}
