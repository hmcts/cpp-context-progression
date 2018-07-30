package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

@RunWith(MockitoJUnitRunner.class)
public class AddConvictionDateHandlerTest extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private AddConvictionDateHandler addConvictionDateHandler;

    @Test
    public void addConvictionDateToOffence() throws EventStreamException {

        UUID offenceId = UUID.randomUUID();
        LocalDate convictionDate = LocalDate.now();

        when(jsonObject.getString("caseId")).thenReturn(CASE_ID.toString());
        when(jsonObject.getString("offenceId")).thenReturn(offenceId.toString());
        when(jsonObject.getString("convictionDate")).thenReturn(convictionDate.toString());
        when(caseProgressionAggregate.addConvictionDateToOffence(CASE_ID, offenceId, convictionDate))
                .thenReturn(events);

        addConvictionDateHandler.addConvictionDateToOffence(jsonEnvelope);

        verify(caseProgressionAggregate).addConvictionDateToOffence(CASE_ID, offenceId, convictionDate);

    }
}
