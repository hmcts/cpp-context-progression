package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

@Deprecated
@RunWith(MockitoJUnitRunner.class)
public class SentenceHearingDateCommandHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private SentenceHearingDateHandler sentenceHearingDateHandler;

    private final LocalDate sentenceHearingDate = LocalDate.now();

    @Test
    public void shouldPassNoMoreInformationRequired() throws EventStreamException {

        when(jsonObject.getString("caseId")).thenReturn(CASE_ID.toString());

        when(jsonObject.getString("sentenceHearingDate")).thenReturn(sentenceHearingDate.toString());
        when(caseAggregate.addSentenceHearingDate(CASE_ID,sentenceHearingDate))
                        .thenReturn(events);

        sentenceHearingDateHandler.addSentenceHearingDate(jsonEnvelope);
        verify(caseAggregate).addSentenceHearingDate(CASE_ID,sentenceHearingDate);
    }
}
