package uk.gov.moj.cpp.progression.event.service;

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
import uk.gov.moj.cpp.progression.event.converter.DefendantEventToDefendantConverter;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantDocumentRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

public class CaseService {

    @Inject
    DefendantEventToDefendantConverter defendantEventToDefendantConverter;
    @Inject
    DefendantRepository defendantRepository;
    @Inject
    DefendantDocumentRepository defendantDocumentRepository;
    @Inject
    private CaseProgressionDetailRepository caseProgressionDetailRepo;

    @Transactional
    public void directionIssued(final DirectionIssued event) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        caseProgressionDetail.setDirectionIssuedOn(event.getDirectionIssuedDate());
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void addSendingCommittalHearingInformation(final SendingCommittalHearingInformationAdded event) {

        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        caseProgressionDetail.setFromCourtCentre(event.getFromCourtCentre());
        caseProgressionDetail.setSendingCommittalDate(event.getSendingCommittalDate());
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void addSentenceHearingDate(final SentenceHearingDateAdded event) {
        updateSentenceHearingDateForCase(event.getCaseProgressionId(), event.getSentenceHearingDate());
    }

    @Transactional
    public void updateSentenceHearingDate(final SentenceHearingDateUpdated event) {
        updateSentenceHearingDateForCase(event.getCaseProgressionId(), event.getSentenceHearingDate());
    }

    private void updateSentenceHearingDateForCase(UUID caseProgressionId, LocalDate sentenceHearingDate) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(caseProgressionId);
        caseProgressionDetail.setSentenceHearingDate(sentenceHearingDate);
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void addSentenceHearing(final SentenceHearingAdded event) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        caseProgressionDetail.setSentenceHearingId(event.getSentenceHearingId());
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void caseToBeAssigned(final CaseToBeAssignedUpdated event) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        caseProgressionDetail.setStatus(event.getStatus());
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void caseAssignedForReview(final CaseAssignedForReviewUpdated event) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        caseProgressionDetail.setStatus(event.getStatus());
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void caseReadyForSentenceHearing(final CaseReadyForSentenceHearing event) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        caseProgressionDetail.setStatus(event.getStatus());
        caseProgressionDetail.setCaseStatusUpdatedDateTime(event.getCaseStatusUpdatedDateTime());
        caseProgressionDetailRepo.save(caseProgressionDetail);
    }

    @Transactional
    public void casePendingForSentenceHearing(final CasePendingForSentenceHearing event) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
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
}