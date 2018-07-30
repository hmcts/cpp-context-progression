package uk.gov.moj.cpp.progression.event.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;
import uk.gov.moj.cpp.progression.event.converter.DefendantEventToDefendantConverter;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantDocumentRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.progression.persistence.repository.OffenceRepository;

public class CaseService {

    @Inject
    DefendantEventToDefendantConverter defendantEventToDefendantConverter;
    @Inject
    DefendantRepository defendantRepository;
    @Inject
    DefendantDocumentRepository defendantDocumentRepository;
    @Inject
    private CaseProgressionDetailRepository caseProgressionDetailRepo;
    
    @Inject
    private OffenceRepository offenceRepository;

    @Transactional
    public void addSendingCommittalHearingInformation(final SendingCommittalHearingInformationAdded event) {

        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseId());
        caseProgressionDetail.setFromCourtCentre(event.getFromCourtCentre());
        caseProgressionDetail.setSendingCommittalDate(event.getSendingCommittalDate());
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void addSentenceHearingDate(final SentenceHearingDateAdded event) {
        updateSentenceHearingDateForCase(event.getCaseId(), event.getSentenceHearingDate());
    }

    private void updateSentenceHearingDateForCase(UUID caseId, LocalDate sentenceHearingDate) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(caseId);
        caseProgressionDetail.setSentenceHearingDate(sentenceHearingDate);
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void caseToBeAssigned(final CaseToBeAssignedUpdated event) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseId());
        caseProgressionDetail.setStatus(event.getStatus());
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }


    @Transactional
    public void caseReadyForSentenceHearing(final CaseReadyForSentenceHearing event) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseId());
        caseProgressionDetail.setStatus(event.getStatus());
        caseProgressionDetail.setCaseStatusUpdatedDateTime(event.getCaseStatusUpdatedDateTime());
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void casePendingForSentenceHearing(final CasePendingForSentenceHearing event) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseId());
        caseProgressionDetail.setStatus(event.getStatus());
        caseProgressionDetail.setCaseStatusUpdatedDateTime(event.getCaseStatusUpdatedDateTime());
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }


    public void addAdditionalInformationForDefendant(final DefendantAdditionalInformationAdded defendantEvent) {

        Defendant defendant = defendantRepository.findByDefendantId(defendantEvent.getDefendantId());
        defendant = defendantEventToDefendantConverter.populateAdditionalInformation(defendant,
                defendantEvent);
        defendant.setSentenceHearingReviewDecision(true);
        defendant.setSentenceHearingReviewDecisionDateTime(
                defendantEvent.getSentenceHearingReviewDecisionDateTime());
        defendantRepository.save(defendant);
    }

    @Transactional
    public void preSentenceReportForDefendantsRequested(final PreSentenceReportForDefendantsRequested event) {
        List<DefendantPSR> defendantPsrs = event.getDefendants();
        defendantPsrs.forEach(defPsr -> {
            Defendant defendant = defendantRepository.findByDefendantId(defPsr.getDefendantId());
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