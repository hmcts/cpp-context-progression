package uk.gov.moj.cpp.progression.event.service;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.moj.cpp.progression.domain.event.AllStatementsIdentified;
import uk.gov.moj.cpp.progression.domain.event.AllStatementsServed;
import uk.gov.moj.cpp.progression.domain.event.DefenceIssuesAdded;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.domain.event.PTPHearingVacated;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SfrIssuesAdded;
import uk.gov.moj.cpp.progression.domain.event.DefenceTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.ProsecutionTrialEstimateAdded;
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
    
	@Mock
	private CaseProgressionDetailRepository repository;

	@Mock
	private CaseAddedToCrownCourtToCaseProgressionDetailConverter caseAddedToCrownCourt;

	@Mock
	private CaseSentToCrownCourtToCaseProgressionDetailConverter caseSentToCrownCourt;

    @InjectMocks
    private CaseService service;
    
    @Test
    public void addDefenceIssuesTest() {
        DefenceIssuesAdded event = mock(DefenceIssuesAdded.class, RETURNS_DEEP_STUBS);
        CaseProgressionDetail entity = mock(CaseProgressionDetail.class, RETURNS_DEEP_STUBS);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findById(event.getCaseProgressionId())).thenReturn(entity);

        service.addDefenceIssues(event);
        verify(repository, times(1)).findById(event.getCaseProgressionId());
        verify(repository, times(1)).save(entity);

    }
    
    @Test
    public void addSendingCommittalHearingInformationTest() {
        SendingCommittalHearingInformationAdded event = mock(SendingCommittalHearingInformationAdded.class);
        CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findById(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.addSendingCommittalHearingInformation(event);
        verify(repository, times(1)).findById(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addTrialEstimateDefenceTest() {
        DefenceTrialEstimateAdded event = mock(DefenceTrialEstimateAdded.class);
        CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findById(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.addTrialEstimateDefence(event);
        verify(repository, times(1)).findById(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addTrialEstimateProsecutionTest() {
        ProsecutionTrialEstimateAdded event = mock(ProsecutionTrialEstimateAdded.class);
        CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findById(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.addTrialEstimateProsecution(event);
        verify(repository, times(1)).findById(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addSFRIssuesTest() {
        SfrIssuesAdded event = mock(SfrIssuesAdded.class);
        CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(repository.findById(event.getCaseProgressionId())).thenReturn(entity);

        service.addSFRIssues(event);
        verify(repository, times(1)).findById(event.getCaseProgressionId());
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void indicateAllStatementsServedTest() {
        AllStatementsServed event = mock(AllStatementsServed.class);
        CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findById(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.indicateAllStatementsServed(event);
        verify(repository, times(1)).findById(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void directionIssuedTest() {
        DirectionIssued event = mock(DirectionIssued.class);
        CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findById(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.directionIssued(event);
        verify(repository, times(1)).findById(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void indicateAllStatementsIdentifiedTest() {
        AllStatementsIdentified event = mock(AllStatementsIdentified.class);
        CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findById(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.indicateAllStatementsIdentified(event);
        verify(repository, times(1)).findById(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void ptpHearingVacatedTest() {
        PTPHearingVacated event = mock(PTPHearingVacated.class);
        CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(repository.findById(event.getCaseProgressionId())).thenReturn(entity);

        service.vacatePtpHeaing(event);
        verify(repository, times(1)).findById(event.getCaseProgressionId());
        verify(repository, times(1)).save(entity);

    }

}
