package uk.gov.moj.cpp.progression.command.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class PrepareForSentenceHearingHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private PrepareForSentenceHearingHandler prepareForSentenceHearingHandler;

    @Test
    public void shouldPrepareForSentenceHearing() throws EventStreamException {

        when(caseProgressionAggregate.prepareForSentenceHearing(jsonEnvelope))
                        .thenReturn(events);

        prepareForSentenceHearingHandler.prepareForSentenceHearing(jsonEnvelope);
        verify(caseProgressionAggregate).prepareForSentenceHearing(jsonEnvelope);
    }
}
