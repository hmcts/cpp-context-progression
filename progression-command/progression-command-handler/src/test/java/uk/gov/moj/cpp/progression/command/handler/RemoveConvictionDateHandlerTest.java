package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
@Deprecated
@RunWith(MockitoJUnitRunner.class)
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
