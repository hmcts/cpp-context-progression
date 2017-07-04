package uk.gov.moj.cpp.progression.command.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class SendCommittalHearingInformationHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private SendCommittalHearingInformationHandler sendCommittalHearingInformationHandler;

    @Test
    public void shouldSendCommittalHearingInformation() throws EventStreamException {

        when(caseProgressionAggregate.sendingHearingCommittal(jsonEnvelope))
                        .thenReturn(events);

        sendCommittalHearingInformationHandler.sendCommittalHearingInformation(jsonEnvelope);
        verify(caseProgressionAggregate).sendingHearingCommittal(jsonEnvelope);
    }
}
