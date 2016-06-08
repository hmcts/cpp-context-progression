package uk.gov.moj.cpp.progression.command.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.api.ProgressionCommandApi;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionCommandApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @InjectMocks
    private ProgressionCommandApi progressionCommandApi;

    @Test
    public void shouldSendToCrownCourt() throws Exception {
        progressionCommandApi.sendToCrownCourt(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldAddCaseToCrownCourt() throws Exception {
        progressionCommandApi.addCaseToCrownCourt(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldAddDefenceIssues() throws Exception {
        progressionCommandApi.addDefenceIssues(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldAddSfrIssues() throws Exception {
        progressionCommandApi.addSfrIssues(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldSendCommittalHearingInformation() throws Exception {
        progressionCommandApi.sendCommittalHearingInformation(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldAddDefenceTrialEstimate() throws Exception {
        progressionCommandApi.addDefenceTrialEstimate(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldAddProsecutionTrialEstimate() throws Exception {
        progressionCommandApi.addProsecutionTrialEstimate(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldIssueDirection() throws Exception {
        progressionCommandApi.issueDirection(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldPreSentenceReport() throws Exception {
        progressionCommandApi.preSentenceReport(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldIndicatestatement() throws Exception {
        progressionCommandApi.indicateStatement(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldIndicateAllStatementsIdentified() throws Exception {
        progressionCommandApi.indicateAllStatementsIdentified(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldIndicateAllStatementsServed() throws Exception {
        progressionCommandApi.indicateAllStatementsServed(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldVacatePTPHearing() throws Exception {
        progressionCommandApi.vacatePTPHearing(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldSentenceHearingDate() throws Exception {
        progressionCommandApi.addSentenceHearingDate(command);
        verify(sender, times(1)).send(command);
    }
    
    @Test
    public void shouldCaseToBeAssigned() throws Exception {
        progressionCommandApi.updateCaseToBeAssigned(command);
        verify(sender, times(1)).send(command);
    }
}