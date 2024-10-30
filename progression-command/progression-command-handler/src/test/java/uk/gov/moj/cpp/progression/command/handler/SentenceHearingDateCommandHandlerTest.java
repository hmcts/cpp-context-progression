package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@Deprecated
@ExtendWith(MockitoExtension.class)
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
