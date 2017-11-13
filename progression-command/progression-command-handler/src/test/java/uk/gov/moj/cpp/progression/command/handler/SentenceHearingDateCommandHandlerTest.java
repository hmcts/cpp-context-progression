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
public class SentenceHearingDateCommandHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private SentenceHearingDateHandler sentenceHearingDateHandler;

    private UUID caseId= CASE_PROGRESSION_ID;
    private LocalDate sentenceHearingDate = LocalDate.now();

    @Test
    public void shouldPassNoMoreInformationRequired() throws EventStreamException {

        when(jsonObject.getString("caseId")).thenReturn(caseId.toString());
        when(jsonObject.getString("caseProgressionId")).thenReturn(caseId.toString());
        when(jsonObject.getString("sentenceHearingDate")).thenReturn(sentenceHearingDate.toString());
        when(caseProgressionAggregate.addSentenceHearingDate(caseId,CASE_PROGRESSION_ID,sentenceHearingDate))
                        .thenReturn(events);

        sentenceHearingDateHandler.addSentenceHearingDate(jsonEnvelope);
        verify(caseProgressionAggregate).addSentenceHearingDate(caseId,CASE_PROGRESSION_ID,sentenceHearingDate);
    }
}
