package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;


@RunWith(MockitoJUnitRunner.class)
public class NoMoreInformationRequiredCommandHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private NoMoreInformationRequiredHandler noMoreInformationRequiredHandler;

    private UUID caseId= UUID.randomUUID();
    private UUID defendantId= UUID.randomUUID();

    @Test
    public void shouldPassNoMoreInformationRequired() throws EventStreamException {

        when(jsonObject.getString("caseId")).thenReturn(caseId.toString());
        when(jsonObject.getString("defendantId")).thenReturn(defendantId.toString());
        when(caseProgressionAggregate.noMoreInformationForDefendant(defendantId,caseId,CASE_PROGRESSION_ID))
                        .thenReturn(events);

        noMoreInformationRequiredHandler.noMoreInformationRequired(jsonEnvelope);
        verify(caseProgressionAggregate).noMoreInformationForDefendant(defendantId,caseId,CASE_PROGRESSION_ID);
    }
}
