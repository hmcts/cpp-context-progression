package uk.gov.moj.cpp.progression.event.service;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportOrdered;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.event.converter.DefendantEventToDefendantConverter;
import uk.gov.moj.cpp.progression.event.listener.DefendantEventListener;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.progression.persistence.repository.DefendantRepository;

/**
 * @author jchondig
 *
 */

public class CaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantEventListener.class);
    private static final String CASE_PROGRESSION_DETAIL_NOT_FOUND =
                    "CaseProgressionDetail not found";
    @Inject
    private CaseProgressionDetailRepository caseProgressionDetailRepo;

    @Inject
    DefendantEventToDefendantConverter defendantEventToDefendantConverter;

    @Inject
    DefendantRepository defendantRepository;

    @Transactional
    public void preSentenceReportOrdered(final PreSentenceReportOrdered event) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void directionIssued(final DirectionIssued event) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setDirectionIssuedOn(event.getDirectionIssuedDate());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void addSendingCommittalHearingInformation(
                    final SendingCommittalHearingInformationAdded event) {

        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setFromCourtCentre(event.getFromCourtCentre());
            caseProgressionDetail.setSendingCommittalDate(event.getSendingCommittalDate());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void addSentenceHearingDate(final SentenceHearingDateAdded event) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setSentenceHearingDate(event.getSentenceHearingDate());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void caseToBeAssigned(final CaseToBeAssignedUpdated event) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setStatus(event.getStatus());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void caseAssignedForReview(final CaseAssignedForReviewUpdated event) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setStatus(event.getStatus());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void caseReadyForSentenceHearing(final CaseReadyForSentenceHearing event) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setStatus(event.getStatus());
            caseProgressionDetail
                            .setReadyForSentenceHearingDate(event.getReadyForSentenceHearingDate());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void casePendingForSentenceHearing(final CasePendingForSentenceHearing event) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setStatus(event.getStatus());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }


    public void addAdditionalInformationForDefendant(
                    final DefendantAdditionalInformationAdded defendantEvent) {

        Defendant defendant =
                        defendantRepository.findByDefendantId(defendantEvent.getDefendantId());
        if (null == defendant) {
            LOGGER.info("No case progression defendant found with ID "
                            + defendantEvent.getDefendantProgressionId());
        } else {
            defendant = defendantEventToDefendantConverter.populateAdditionalInformation(defendant,
                            defendantEvent);
            defendant.setSentenceHearingReviewDecision(true);
            defendant.setSentenceHearingReviewDecisionDateTime(
                            defendantEvent.getSentenceHearingReviewDecisionDateTime());
            defendantRepository.save(defendant);
        }

    }
}
