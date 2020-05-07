package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@Deprecated
@RunWith(MockitoJUnitRunner.class)
public class SendCommittalHearingInformationHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private SendCommittalHearingInformationHandler sendCommittalHearingInformationHandler;

    @Test
    public void shouldSendCommittalHearingInformation() throws EventStreamException {

        when(caseAggregate.sendingHearingCommittal(jsonEnvelope))
                        .thenReturn(events);

        sendCommittalHearingInformationHandler.sendCommittalHearingInformation(jsonEnvelope);
        verify(caseAggregate).sendingHearingCommittal(jsonEnvelope);
    }
}
