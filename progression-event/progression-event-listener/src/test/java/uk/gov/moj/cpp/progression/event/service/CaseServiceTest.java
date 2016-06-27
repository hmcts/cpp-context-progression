package uk.gov.moj.cpp.progression.event.service;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.moj.cpp.progression.domain.event.AllStatementsIdentified;
import uk.gov.moj.cpp.progression.domain.event.AllStatementsServed;
import uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.domain.event.DefenceIssuesAdded;
import uk.gov.moj.cpp.progression.domain.event.DefenceTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.domain.event.PTPHearingVacated;
import uk.gov.moj.cpp.progression.domain.event.ProsecutionTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.SfrIssuesAdded;
import uk.gov.moj.cpp.progression.event.converter.CaseAddedToCrownCourtToCaseProgressionDetailConverter;
import uk.gov.moj.cpp.progression.event.converter.CaseSentToCrownCourtToCaseProgressionDetailConverter;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

/**
 * @author Ted Pritchard
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceTest {

    private static final UUID CASE_PROGRESSION_ID = UUID.randomUUID();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    private CaseProgressionDetailRepository repository;

    @Mock
    private CaseAddedToCrownCourtToCaseProgressionDetailConverter caseAddedToCrownCourt;

    @Mock
    private CaseSentToCrownCourtToCaseProgressionDetailConverter caseSentToCrownCourt;

    @InjectMocks
    private CaseService service;

    final static Long VERSION = 1l;

    @Test
    public void addDefenceIssuesTest() {
        final DefenceIssuesAdded event = mock(DefenceIssuesAdded.class, RETURNS_DEEP_STUBS);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class, RETURNS_DEEP_STUBS);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(event.getCaseProgressionId())).thenReturn(entity);

        service.addDefenceIssues(event, VERSION);
        verify(repository, times(1)).findBy(event.getCaseProgressionId());
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addSendingCommittalHearingInformationTest() {
        final SendingCommittalHearingInformationAdded event = mock(SendingCommittalHearingInformationAdded.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.addSendingCommittalHearingInformation(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addTrialEstimateDefenceTest() {
        final DefenceTrialEstimateAdded event = mock(DefenceTrialEstimateAdded.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.addTrialEstimateDefence(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addTrialEstimateProsecutionTest() {
        final ProsecutionTrialEstimateAdded event = mock(ProsecutionTrialEstimateAdded.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.addTrialEstimateProsecution(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addSFRIssuesTest() {
        final SfrIssuesAdded event = mock(SfrIssuesAdded.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(repository.findBy(event.getCaseProgressionId())).thenReturn(entity);

        service.addSFRIssues(event, VERSION);
        verify(repository, times(1)).findBy(event.getCaseProgressionId());
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void indicateAllStatementsServedTest() {
        final AllStatementsServed event = mock(AllStatementsServed.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.indicateAllStatementsServed(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void directionIssuedTest() {
        final DirectionIssued event = mock(DirectionIssued.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.directionIssued(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void indicateAllStatementsIdentifiedTest() {
        final AllStatementsIdentified event = mock(AllStatementsIdentified.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.indicateAllStatementsIdentified(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void ptpHearingVacatedTest() {
        final PTPHearingVacated event = mock(PTPHearingVacated.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(repository.findBy(event.getCaseProgressionId())).thenReturn(entity);

        service.vacatePtpHeaing(event, VERSION);
        verify(repository, times(1)).findBy(event.getCaseProgressionId());
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addSentenceHearingDateTest() {
        final SentenceHearingDateAdded event = mock(SentenceHearingDateAdded.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(repository.findBy(event.getCaseProgressionId())).thenReturn(entity);

        service.addSentenceHearingDate(event, VERSION);
        verify(repository, times(1)).findBy(event.getCaseProgressionId());
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void caseToBeAssignedUpdatedTest() {
        final CaseToBeAssignedUpdated event = mock(CaseToBeAssignedUpdated.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.caseToBeAssigned(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void caseAssignedForReviewUpdatedTest() {
        final CaseAssignedForReviewUpdated event = mock(CaseAssignedForReviewUpdated.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.caseAssignedForReview(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void caseAssignedForReviewShouldThrowExceptionTest() throws Exception {
        final CaseAssignedForReviewUpdated event = mock(CaseAssignedForReviewUpdated.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.caseAssignedForReview(event, VERSION);
    }

}
