package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CaseNotesQueryView;
import uk.gov.moj.cpp.progression.query.DefendantByLAAContractNumberQueryView;
import uk.gov.moj.cpp.progression.query.HearingQueryView;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProgressionQueryApiTest {
    @Mock
    private JsonEnvelope query;
    @Mock
    private JsonEnvelope response;

    @Mock
    private HearingQueryView hearingQueryView;

    @Mock
    private CaseNotesQueryView caseNotesQueryView;

    @Mock
    private DefendantByLAAContractNumberQueryView defendantByLAAContractNumberQueryView;

    @InjectMocks
    private ProgressionQueryApi progressionHearingsQueryApi;

    @Test
    public void shouldGetHearingByHearingId() {
        when(hearingQueryView.getHearing(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getHearing(query), equalTo(response));
    }

    @Test
    public void shouldGetCaseNotesByCaseId() {
        when(caseNotesQueryView.getCaseNotes(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getCaseNotes(query), equalTo(response));
    }
    @Test
    public void shouldGetDefendantsByLAAContractNumber(){
        when(defendantByLAAContractNumberQueryView.getDefendantsByLAAContractNumber(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getDefendantsByLAAContractNumber(query), equalTo(response));
    }

}
