package uk.gov.moj.cpp.progression.event.service;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.domain.event.DefenceIssuesAdded;
import uk.gov.moj.cpp.progression.domain.event.DefenceTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.domain.event.PTPHearingVacated;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportOrdered;
import uk.gov.moj.cpp.progression.domain.event.ProsecutionTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.SfrIssuesAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.event.converter.CaseAddedToCrownCourtToCaseProgressionDetailConverter;
import uk.gov.moj.cpp.progression.event.converter.CaseSentToCrownCourtToCaseProgressionDetailConverter;
import uk.gov.moj.cpp.progression.event.converter.DefendantEventToDefendantConverter;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.progression.persistence.repository.DefendantRepository;

/**
 * @author Ted Pritchard
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceTest {

    private static final UUID CASE_PROGRESSION_ID = UUID.randomUUID();

    private static final UUID DEFENDANT_ID = UUID.randomUUID();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    private CaseProgressionDetailRepository repository;

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private CaseAddedToCrownCourtToCaseProgressionDetailConverter caseAddedToCrownCourt;

    @Mock
    private CaseSentToCrownCourtToCaseProgressionDetailConverter caseSentToCrownCourt;

    @Mock
    private DefendantEventToDefendantConverter defendantEventToDefendantConverter;

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
    public void addDefenceIssuesShouldThrowExceptionTest() throws Exception {
        final DefenceIssuesAdded event = mock(DefenceIssuesAdded.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.addDefenceIssues(event, VERSION);
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void addSendingCommittalHearingInformationTest() {
        final SendingCommittalHearingInformationAdded event =
                        mock(SendingCommittalHearingInformationAdded.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.addSendingCommittalHearingInformation(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addSendingCommittalHearingInformationShouldThrowExceptionTest() throws Exception {
        final SendingCommittalHearingInformationAdded event =
                        mock(SendingCommittalHearingInformationAdded.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.addSendingCommittalHearingInformation(event, VERSION);
        verifyNoMoreInteractions(repository);
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
    public void addTrialEstimateDefenceShouldThrowExceptionTest() throws Exception {
        final DefenceTrialEstimateAdded event = mock(DefenceTrialEstimateAdded.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.addTrialEstimateDefence(event, VERSION);
        verifyNoMoreInteractions(repository);
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
    public void addTrialEstimateProsecutionShouldThrowExceptionTest() throws Exception {
        final ProsecutionTrialEstimateAdded event = mock(ProsecutionTrialEstimateAdded.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.addTrialEstimateProsecution(event, VERSION);
        verifyNoMoreInteractions(repository);
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
    public void addSFRIssuesShouldThrowExceptionTest() throws Exception {
        final SfrIssuesAdded event = mock(SfrIssuesAdded.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.addSFRIssues(event, VERSION);
        verifyNoMoreInteractions(repository);
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
    public void indicateAllStatementsServedShouldThrowExceptionTest() throws Exception {
        final AllStatementsServed event = mock(AllStatementsServed.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.indicateAllStatementsServed(event, VERSION);
        verifyNoMoreInteractions(repository);
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
    public void directionIssuedShouldThrowExceptionTest() throws Exception {
        final DirectionIssued event = mock(DirectionIssued.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.directionIssued(event, VERSION);
        verifyNoMoreInteractions(repository);
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
    public void indicateAllStatementsIdentifiedShouldThrowExceptionTest() throws Exception {
        final AllStatementsIdentified event = mock(AllStatementsIdentified.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.indicateAllStatementsIdentified(event, VERSION);
        verifyNoMoreInteractions(repository);
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
    public void ptpHearingVacatedShouldThrowExceptionTest() throws Exception {
        final PTPHearingVacated event = mock(PTPHearingVacated.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.vacatePtpHeaing(event, VERSION);
        verifyNoMoreInteractions(repository);
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
    public void addSentenceHearingDateShouldThrowExceptionTest() throws Exception {
        final SentenceHearingDateAdded event = mock(SentenceHearingDateAdded.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.addSentenceHearingDate(event, VERSION);
        verifyNoMoreInteractions(repository);
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
    public void caseToBeAssignedUpdatedShouldThrowExceptionTest() throws Exception {
        final CaseToBeAssignedUpdated event = mock(CaseToBeAssignedUpdated.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.caseToBeAssigned(event, VERSION);
        verifyNoMoreInteractions(repository);
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

    @Test
    public void caseReadyForScentenceHearingUpdatedTest() {
        final CaseReadyForSentenceHearing event = mock(CaseReadyForSentenceHearing.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.caseReadyForSentenceHearing(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void caseReadyForSentenceHearingShouldThrowExceptionTest() throws Exception {
        final CaseReadyForSentenceHearing event = mock(CaseReadyForSentenceHearing.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.caseReadyForSentenceHearing(event, VERSION);
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void casePendingForScentenceHearingUpdatedTest() {
        final CasePendingForSentenceHearing event = mock(CasePendingForSentenceHearing.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.casePendingForSentenceHearing(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void casePendingForSentenceHearingShouldThrowExceptionTest() throws Exception {
        final CasePendingForSentenceHearing event = mock(CasePendingForSentenceHearing.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.casePendingForSentenceHearing(event, VERSION);
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void preSentenceReportOrderedTest() {
        final PreSentenceReportOrdered event = mock(PreSentenceReportOrdered.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);
        service.preSentenceReportOrdered(event, VERSION);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);
    }

    @Test
    public void preSentenceReportOrderedShouldThrowExceptionTest() throws Exception {
        final PreSentenceReportOrdered event = mock(PreSentenceReportOrdered.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(null);
        exception.expect(RuntimeException.class);
        exception.expectMessage("CaseProgressionDetail not found");
        service.preSentenceReportOrdered(event, VERSION);
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void addAdditionalInformationForDefendantTest() {
        final DefendantAdditionalInformationAdded event =
                        mock(DefendantAdditionalInformationAdded.class);
        final Defendant entity = mock(Defendant.class);
        when(event.getDefendantId()).thenReturn(DEFENDANT_ID);
        when(defendantRepository.findByDefendantId(DEFENDANT_ID)).thenReturn(entity);
        when(defendantEventToDefendantConverter.populateAdditionalInformation(entity, event))
                        .thenReturn(entity);
        service.addAdditionalInformationForDefendant(event);
        verify(defendantRepository, times(1)).findByDefendantId(DEFENDANT_ID);
        verify(defendantRepository, times(1)).save(entity);
    }

    @Test
    public void addAdditionalInformationForDefendantShouldThrowExceptionTest() throws Exception {
        final DefendantAdditionalInformationAdded event =
                        mock(DefendantAdditionalInformationAdded.class);
        when(event.getDefendantId()).thenReturn(DEFENDANT_ID);
        when(defendantRepository.findByDefendantId(DEFENDANT_ID)).thenReturn(null);
        service.addAdditionalInformationForDefendant(event);
        verify(defendantRepository, times(1)).findByDefendantId(DEFENDANT_ID);
        verifyNoMoreInteractions(defendantRepository);
    }
}
