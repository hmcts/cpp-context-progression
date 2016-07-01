package uk.gov.moj.cpp.progression.event.service;

import javax.inject.Inject;
import javax.transaction.Transactional;

import uk.gov.moj.cpp.progression.domain.event.AllStatementsIdentified;
import uk.gov.moj.cpp.progression.domain.event.AllStatementsServed;
import uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated;
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
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

/**
 * @author jchondig
 *
 */

public class CaseService {

    private static final String CASE_PROGRESSION_DETAIL_NOT_FOUND =
                    "CaseProgressionDetail not found";
    @Inject
    private CaseProgressionDetailRepository caseProgressionDetailRepo;

    @Transactional
    public void indicateAllStatementsIdentified(AllStatementsIdentified event, Long version) {

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
    public void preSentenceReportOrdered(PreSentenceReportOrdered event, Long version) {
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
    public void directionIssued(DirectionIssued event, Long version) {
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
    public void indicateAllStatementsServed(AllStatementsServed event, Long version) {
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
    public void addDefenceIssues(DefenceIssuesAdded event, Long version) {

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
    public void addSFRIssues(SfrIssuesAdded event, Long version) {

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
    public void addTrialEstimateDefence(DefenceTrialEstimateAdded event, Long version) {

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
    public void addTrialEstimateProsecution(ProsecutionTrialEstimateAdded event, Long version) {

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

    public void vacatePtpHeaing(PTPHearingVacated event, Long version) {
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
    public void addSendingCommittalHearingInformation(SendingCommittalHearingInformationAdded event,
                    Long version) {

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
    public void addSentenceHearingDate(SentenceHearingDateAdded event, Long version) {
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
    public void caseToBeAssigned(CaseToBeAssignedUpdated event, Long version) {
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
    public void caseAssignedForReview(CaseAssignedForReviewUpdated event, Long version) {
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
    public void caseReadyForSentenceHearing(CaseReadyForSentenceHearing event, Long version) {
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
}
