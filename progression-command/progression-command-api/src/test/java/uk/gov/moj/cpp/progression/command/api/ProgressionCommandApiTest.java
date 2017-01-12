package uk.gov.moj.cpp.progression.command.api;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionCommandApiTest {

	@Mock
	private Sender sender;

	@Mock
	private JsonEnvelope command;

	@InjectMocks
	private ProgressionCommandApi progressionCommandApi;

	@Test
	public void shouldAddCaseToCrownCourt() throws Exception {
		progressionCommandApi.addCaseToCrownCourt(command);
		verify(sender, times(1)).send(command);
	}

	@Test
	public void shouldSendCommittalHearingInformation() throws Exception {
		progressionCommandApi.sendCommittalHearingInformation(command);
		verify(sender, times(1)).send(command);
	}

	@Test
	public void shouldPreSentenceReport() throws Exception {
		progressionCommandApi.preSentenceReport(command);
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

	@Test
	public void shouldCaseAssignedForReview() throws Exception {
		progressionCommandApi.updateCaseAssignedForReview(command);
		verify(sender, times(1)).send(command);
	}

	@Test
	public void shouldPrepareForSentenceHearing() throws Exception {
		progressionCommandApi.prepareForSentenceHearing(command);
		verify(sender, times(1)).send(command);
	}

	@Test
	public void shouldAddDefendantProgression() throws Exception {
		progressionCommandApi.addAdditionalInformationForDefendant(command);
		verify(sender, times(1)).send(command);
	}


	@Test
	public void shouldPassNoMoreInformationRequired() throws Exception {
		progressionCommandApi.noMoreInformationRequired(command);
		verify(sender, times(1)).send(command);
	}
	
	@Test
    public void shouldUpdatePSRForDefendants() throws Exception {
        progressionCommandApi.updatePSRForDefendants(command);
        verify(sender, times(1)).send(command);
    }
}
