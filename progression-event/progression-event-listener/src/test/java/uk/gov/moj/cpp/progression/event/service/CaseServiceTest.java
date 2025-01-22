package uk.gov.moj.cpp.progression.event.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateAdded;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateRemoved;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.progression.persistence.repository.OffenceRepository;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ExtendWith(MockitoExtension.class)
public class CaseServiceTest {

    final static Long VERSION = 1l;
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();

    @Mock
    private CaseProgressionDetailRepository repository;

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private OffenceRepository offenceRepository;    

    @InjectMocks
    private CaseService service;

    @Test
    public void addSendingCommittalHearingInformationTest() {
        final SendingCommittalHearingInformationAdded event =
                mock(SendingCommittalHearingInformationAdded.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(event.getCaseId()).thenReturn(CASE_ID);
        when(repository.findBy(CASE_ID)).thenReturn(entity);

        service.addSendingCommittalHearingInformation(event);
        verify(repository, times(1)).findBy(CASE_ID);
        verify(repository, times(1)).save(entity);

    }

    @Test
    public void addSentenceHearingDateTest() {
        final SentenceHearingDateAdded event = mock(SentenceHearingDateAdded.class);
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(repository.findBy(event.getCaseId())).thenReturn(entity);

        service.addSentenceHearingDate(event);
        verify(repository, times(1)).findBy(event.getCaseId());
        verify(repository, times(1)).save(entity);

    }







    @Test
    public void caseAssignedForReviewTest() {
        //given
        final CaseProgressionDetail entity = mock(CaseProgressionDetail.class);
        when(repository.findBy(CASE_ID)).thenReturn(entity);
        //when
        service.caseAssignedForReview(CASE_ID, CaseStatusEnum.READY_FOR_REVIEW);
        //then
        verify(repository, times(1)).findBy(CASE_ID);
        verify(repository, times(1)).save(entity);

    }
    
    @Test
    public void addConvictionDateToOffenceTest() {

        final UUID caseId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        final OffenceDetail entity = mock(OffenceDetail.class);

        final ConvictionDateAdded convictionDateAdded = ConvictionDateAdded.builder().withCaseId(caseId)
                .withOffenceId(offenceId).withConvictionDate(convictionDate).build();

        when(offenceRepository.findBy(offenceId)).thenReturn(entity);

        service.setConvictionDate(convictionDateAdded.getOffenceId(), convictionDateAdded.getConvictionDate());

        verify(offenceRepository, times(1)).findBy(offenceId);
        verify(offenceRepository, times(1)).save(entity);

    }

    @Test
    public void removeConvictionDateFromOffenceTest() {

        final UUID caseId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();

        final OffenceDetail entity = mock(OffenceDetail.class);

        final ConvictionDateRemoved convictionDateRemoved = ConvictionDateRemoved.builder().withCaseId(caseId)
                .withOffenceId(offenceId).build();

        when(offenceRepository.findBy(offenceId)).thenReturn(entity);

        service.setConvictionDate(convictionDateRemoved.getOffenceId(), null);

        verify(offenceRepository, times(1)).findBy(offenceId);
        verify(offenceRepository, times(1)).save(entity);

    }
}
