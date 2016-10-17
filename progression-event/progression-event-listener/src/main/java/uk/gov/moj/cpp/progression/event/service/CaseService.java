package uk.gov.moj.cpp.progression.event.service;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void indicateAllStatementsIdentified(final AllStatementsIdentified event,
                    final Long version) {

        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setIsAllStatementsIdentified(Boolean.TRUE);
            caseProgressionDetail.setVersion(version);
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void preSentenceReportOrdered(final PreSentenceReportOrdered event, final Long version) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setIsPSROrdered(event.getIsPSROrdered());
            caseProgressionDetail.setVersion(version);
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void directionIssued(final DirectionIssued event, final Long version) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setVersion(version);
            caseProgressionDetail.setDirectionIssuedOn(event.getDirectionIssuedDate());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void indicateAllStatementsServed(final AllStatementsServed event, final Long version) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setIsAllStatementsServed(Boolean.TRUE);
            caseProgressionDetail.setVersion(version);
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void addDefenceIssues(final DefenceIssuesAdded event, final Long version) {

        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setVersion(version);
            caseProgressionDetail.setDefenceIssue(event.getDefenceIssues());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void addSFRIssues(final SfrIssuesAdded event, final Long version) {

        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setVersion(version);
            caseProgressionDetail.setSfrIssue(event.getSfrIssues());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void addTrialEstimateDefence(final DefenceTrialEstimateAdded event, final Long version) {

        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setVersion(version);
            caseProgressionDetail
                            .setTrialEstimateDefence(Long.valueOf(event.getDefenceTrialEstimate()));
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void addTrialEstimateProsecution(final ProsecutionTrialEstimateAdded event,
                    final Long version) {

        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setVersion(version);
            caseProgressionDetail.setTrialEstimateProsecution(
                            Long.valueOf(event.getprosecutionTrialEstimate()));
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    public void vacatePtpHeaing(final PTPHearingVacated event, final Long version) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setPtpHearingVacatedDate(event.getPtpHearingVacatedDate());
            caseProgressionDetail.setVersion(version);
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void addSendingCommittalHearingInformation(
                    final SendingCommittalHearingInformationAdded event, final Long version) {

        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setVersion(version);
            caseProgressionDetail.setFromCourtCentre(event.getFromCourtCentre());
            caseProgressionDetail.setSendingCommittalDate(event.getSendingCommittalDate());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void addSentenceHearingDate(final SentenceHearingDateAdded event, final Long version) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setSentenceHearingDate(event.getSentenceHearingDate());
            caseProgressionDetail.setVersion(version);
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void caseToBeAssigned(final CaseToBeAssignedUpdated event, final Long version) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setVersion(version);
            caseProgressionDetail.setStatus(event.getStatus());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void caseAssignedForReview(final CaseAssignedForReviewUpdated event,
                    final Long version) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setVersion(version);
            caseProgressionDetail.setStatus(event.getStatus());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void caseReadyForSentenceHearing(final CaseReadyForSentenceHearing event,
                    final Long version) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setVersion(version);
            caseProgressionDetail.setStatus(event.getStatus());
            caseProgressionDetail
                            .setReadyForSentenceHearingDate(event.getReadyForSentenceHearingDate());
            caseProgressionDetailRepo.save(caseProgressionDetail);
        } else {
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
    }

    @Transactional
    public void casePendingForSentenceHearing(final CasePendingForSentenceHearing event,
                    final Long version) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findBy(event.getCaseProgressionId());
        if (caseProgressionDetail != null) {
            caseProgressionDetail.setVersion(version);
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
