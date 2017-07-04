package uk.gov.moj.cpp.progression.command.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class AddSentenceHearingCommandHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private AddSentenceHearingHandler addSentenceHearingHandler;

    private UUID caseId= UUID.randomUUID();
    private UUID sentenceHearingId = UUID.randomUUID();

    @Test
    public void shouldPassAddSentenceHearingHandler() throws EventStreamException {

        when(jsonObject.getString("caseId")).thenReturn(caseId.toString());
        when(jsonObject.getString("sentenceHearingId")).thenReturn(sentenceHearingId.toString());
        when(caseProgressionAggregate.addSentenceHearing(caseId,CASE_PROGRESSION_ID, sentenceHearingId))
                        .thenReturn(events);

        addSentenceHearingHandler.addSentenceHearing(jsonEnvelope);
        verify(caseProgressionAggregate).addSentenceHearing(caseId,CASE_PROGRESSION_ID, sentenceHearingId);
    }
}
