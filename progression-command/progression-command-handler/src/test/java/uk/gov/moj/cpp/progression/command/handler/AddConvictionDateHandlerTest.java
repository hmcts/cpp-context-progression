package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
@Deprecated
@ExtendWith(MockitoExtension.class)
public class AddConvictionDateHandlerTest extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private AddConvictionDateHandler addConvictionDateHandler;

    @Test
    public void addConvictionDateToOffence() throws EventStreamException {

        final UUID offenceId = UUID.randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        when(jsonObject.getString("caseId")).thenReturn(CASE_ID.toString());
        when(jsonObject.getString("offenceId")).thenReturn(offenceId.toString());
        when(jsonObject.getString("convictionDate")).thenReturn(convictionDate.toString());
        when(caseAggregate.addConvictionDateToOffence(CASE_ID, offenceId, convictionDate))
                .thenReturn(events);

        addConvictionDateHandler.addConvictionDateToOffence(jsonEnvelope);

        verify(caseAggregate).addConvictionDateToOffence(CASE_ID, offenceId, convictionDate);

    }
}
