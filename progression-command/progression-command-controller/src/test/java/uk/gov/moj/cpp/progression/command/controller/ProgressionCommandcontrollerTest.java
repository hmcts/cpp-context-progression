package uk.gov.moj.cpp.progression.command.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Mockito.*;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.controller.service.StructureReadService;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionCommandcontrollerTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Mock
    JsonEnvelope modifiedJsonEnvelope;

    @Mock
    private StructureReadService structureCaseService;

    @InjectMocks
    private ProgressionCommandController progressionCommandcontroller;

    @Test
    public void shouldSendCaseToCrownCourt() throws Exception {
        progressionCommandcontroller.sendToCrownCourt(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldAddCaseToCrownCourt() throws Exception {

        String caseId = UUID.randomUUID().toString();
        String defendantId = UUID.randomUUID().toString();
        JsonObject value = mock(JsonObject.class);
        when(command.payloadAsJsonObject()).thenReturn(value);
        when(value.getString("caseId")).thenReturn(caseId);
        when(structureCaseService.getStructureCaseDefendentsId(caseId))
                        .thenReturn(Arrays.asList(defendantId));
        when(enveloper.withMetadataFrom(command, "progression.command.add-case-to-progression"))
                        .thenReturn(function);
        when(function.apply(any())).thenReturn(modifiedJsonEnvelope);
        progressionCommandcontroller.addCaseToCrownCourt(command);
        verify(sender, times(1)).send(modifiedJsonEnvelope);

    }


    @Test
    public void shouldAddDefenceIssues() throws Exception {
        progressionCommandcontroller.addDefenceIssues(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldAddSfrIssues() throws Exception {
        progressionCommandcontroller.addSfrIssues(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldSendCommittalHearingInformation() throws Exception {
        progressionCommandcontroller.sendCommittalHearingInformation(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldAddDefenceTrialEstimate() throws Exception {
        progressionCommandcontroller.addDefenceTrialEstimate(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldAddProsecutionTrialEstimate() throws Exception {
        progressionCommandcontroller.addProsecutionTrialEstimate(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldIssueDirection() throws Exception {
        progressionCommandcontroller.issueDirection(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldPreSentenceReport() throws Exception {
        progressionCommandcontroller.preSentenceReport(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldIndicatestatement() throws Exception {
        progressionCommandcontroller.indicateStatement(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldIndicateAllStatementsIdentified() throws Exception {
        progressionCommandcontroller.indicateAllStatementsIdentified(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldIndicateAllStatementsServed() throws Exception {
        progressionCommandcontroller.indicateAllStatementsServed(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldVacatePTPHearing() throws Exception {
        progressionCommandcontroller.vacatePTPHearing(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldSentenceHearingDate() throws Exception {
        progressionCommandcontroller.addSentenceHearingDate(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldCaseToBeAssigned() throws Exception {
        progressionCommandcontroller.updateCaseToBeAssigned(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldCaseAssignedForReview() throws Exception {
        progressionCommandcontroller.updateCaseAssignedForReview(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldPrepareForSentenceHearing() throws Exception {
        progressionCommandcontroller.prepareForSentenceHearing(command);
        verify(sender, times(1)).send(command);
    }
}
