package uk.gov.moj.cpp.progression.event.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;
import uk.gov.moj.cpp.progression.event.converter.CaseAddedToCrownCourtToCaseProgressionDetailConverter;
import uk.gov.moj.cpp.progression.event.converter.DefendantEventToDefendantConverter;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseServiceTest {

    final static Long VERSION = 1l;
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
    private DefendantEventToDefendantConverter defendantEventToDefendantConverter;
    @InjectMocks
    private CaseService service;

    @Test
    public void addSendingCommittalHearingInformationTest() {
        final SendingCommittalHearingInformationAdded event =
                mock(SendingCommittalHearingInformationAdded.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.addSendingCommittalHearingInformation(event);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void directionIssuedTest() {
        final DirectionIssued event = mock(DirectionIssued.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.directionIssued(event);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addSentenceHearingDateTest() {
        final SentenceHearingDateAdded event = mock(SentenceHearingDateAdded.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(repository.findBy(event.getCaseProgressionId())).thenReturn(entity);

        service.addSentenceHearingDate(event);
        verify(repository, times(1)).findBy(event.getCaseProgressionId());
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void updateSentenceHearingDateTest() {
        final SentenceHearingDateUpdated event = mock(SentenceHearingDateUpdated.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(repository.findBy(event.getCaseProgressionId())).thenReturn(entity);

        service.updateSentenceHearingDate(event);
        verify(repository, times(1)).findBy(event.getCaseProgressionId());
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addSentenceHearingTest() {
        final SentenceHearingAdded event = mock(SentenceHearingAdded.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(repository.findBy(event.getCaseProgressionId())).thenReturn(entity);

        service.addSentenceHearing(event);
        verify(repository, times(1)).findBy(event.getCaseProgressionId());
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void caseToBeAssignedUpdatedTest() {
        final CaseToBeAssignedUpdated event = mock(CaseToBeAssignedUpdated.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.caseToBeAssigned(event);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void caseAssignedForReviewUpdatedTest() {
        final CaseAssignedForReviewUpdated event = mock(CaseAssignedForReviewUpdated.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.caseAssignedForReview(event);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void caseReadyForScentenceHearingUpdatedTest() {
        final CaseReadyForSentenceHearing event = mock(CaseReadyForSentenceHearing.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.caseReadyForSentenceHearing(event);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void casePendingForScentenceHearingUpdatedTest() {
        final CasePendingForSentenceHearing event = mock(CasePendingForSentenceHearing.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(repository.findBy(CASE_PROGRESSION_ID)).thenReturn(entity);

        service.casePendingForSentenceHearing(event);
        verify(repository, times(1)).findBy(CASE_PROGRESSION_ID);
        verify(repository, times(1)).save(entity);

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
    public void preSentenceReportForDefendantsRequestedTest() {
        final PreSentenceReportForDefendantsRequested event =
                mock(PreSentenceReportForDefendantsRequested.class);
        final Defendant entity = mock(Defendant.class);
        final DefendantPSR defendantPSR = new DefendantPSR(DEFENDANT_ID, true);
        List<DefendantPSR> defendantPsrs = new ArrayList<>();
        defendantPsrs.add(defendantPSR);

        when(event.getCaseProgressionId()).thenReturn(CASE_PROGRESSION_ID);
        when(event.getDefendants()).thenReturn(defendantPsrs);
        when(defendantRepository.findByDefendantId(DEFENDANT_ID)).thenReturn(entity);
        service.preSentenceReportForDefendantsRequested(event);
        verify(defendantRepository, times(1)).findByDefendantId(DEFENDANT_ID);
        verify(defendantRepository, times(1)).save(entity);
    }
}
