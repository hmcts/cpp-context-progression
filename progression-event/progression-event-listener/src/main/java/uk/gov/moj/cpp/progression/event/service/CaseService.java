package uk.gov.moj.cpp.progression.event.service;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.progression.persistence.repository.OffenceRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

/**
 * @deprecated This is deprecated for Release 2.4
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class CaseService {


    @Inject
    DefendantRepository defendantRepository;
    @Inject
    private CaseProgressionDetailRepository caseProgressionDetailRepo;

    @Inject
    private OffenceRepository offenceRepository;

    @Transactional
    public void addSendingCommittalHearingInformation(final SendingCommittalHearingInformationAdded event) {

        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseId());
        caseProgressionDetail.setFromCourtCentre(event.getFromCourtCentre());
        caseProgressionDetail.setSendingCommittalDate(event.getSendingCommittalDate());
        if(event.getCourtCenterID() != null) {
            caseProgressionDetail.setCourtCentreId(event.getCourtCenterID());
        }
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void addSentenceHearingDate(final SentenceHearingDateAdded event) {
        updateSentenceHearingDateForCase(event.getCaseId(), event.getSentenceHearingDate());
    }

    private void updateSentenceHearingDateForCase(final UUID caseId, final LocalDate sentenceHearingDate) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(caseId);
        caseProgressionDetail.setSentenceHearingDate(sentenceHearingDate);
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }


    @Transactional
    public void preSentenceReportForDefendantsRequested(final PreSentenceReportForDefendantsRequested event) {
        final List<DefendantPSR> defendantPsrs = event.getDefendants();
        defendantPsrs.forEach(defPsr -> {
            final Defendant defendant = defendantRepository.findByDefendantId(defPsr.getDefendantId());
            defendant.setIsPSRRequested(defPsr.getPsrIsRequested());
            defendantRepository.save(defendant);
        });
    }

    @Transactional
    public void caseAssignedForReview(final UUID caseId, final CaseStatusEnum caseStatus) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(caseId);
        caseProgressionDetail.setStatus(caseStatus);
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void setConvictionDate(final UUID offenceId, final LocalDate convictionDate) {
        final OffenceDetail offenceDetail = offenceRepository.findBy(offenceId);
        offenceDetail.setConvictionDate(convictionDate);
        offenceRepository.save(offenceDetail);
    }
}