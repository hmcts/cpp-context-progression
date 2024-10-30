package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
@Deprecated
@ExtendWith(MockitoExtension.class)
public class RemoveConvictionDateHandlerTest extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private RemoveConvictionDateHandler removeConvictionDateHandler;

    @Test
    public void addConvictionDateToOffence() throws EventStreamException {

        final UUID offenceId = UUID.randomUUID();

        when(jsonObject.getString("caseId")).thenReturn(CASE_ID.toString());
        when(jsonObject.getString("offenceId")).thenReturn(offenceId.toString());
        when(caseAggregate.removeConvictionDateFromOffence(CASE_ID, offenceId)).thenReturn(events);

        removeConvictionDateHandler.removeConvictionDateFromOffence(jsonEnvelope);

        verify(caseAggregate).removeConvictionDateFromOffence(CASE_ID, offenceId);

    }
}
