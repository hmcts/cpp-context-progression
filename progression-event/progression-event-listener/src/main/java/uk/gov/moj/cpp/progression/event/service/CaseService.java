package uk.gov.moj.cpp.progression.event.service;

import javax.inject.Inject;
import javax.transaction.Transactional;

import uk.gov.moj.cpp.progression.domain.event.AllStatementsIdentified;
import uk.gov.moj.cpp.progression.domain.event.AllStatementsServed;
import uk.gov.moj.cpp.progression.domain.event.DefenceIssuesAdded;
import uk.gov.moj.cpp.progression.domain.event.DefenceTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.domain.event.PTPHearingVacated;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportOrdered;
import uk.gov.moj.cpp.progression.domain.event.ProsecutionTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SfrIssuesAdded;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

/**
 * @author jchondig
 *
 */

public class CaseService {

	private static final String CASE_PROGRESSION_DETAIL_NOT_FOUND = "CaseProgressionDetail not found";
	@Inject
	private CaseProgressionDetailRepository caseProgressionDetailRepo;

	@Transactional
	public void indicateAllStatementsIdentified(AllStatementsIdentified event) {

		CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findById(event.getCaseProgressionId());
		if(caseProgressionDetail != null){
    		caseProgressionDetail.setIsAllStatementsIdentified(Boolean.TRUE);
    		caseProgressionDetail.setVersion(event.getVersion());
    		caseProgressionDetailRepo.save(caseProgressionDetail);
		}else{
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
		}
	}

	@Transactional
	public void preSentenceReportOrdered(PreSentenceReportOrdered event) {
	    CaseProgressionDetail caseProgressionDetail= caseProgressionDetailRepo.findById(event.getCaseProgressionId());
	    if(caseProgressionDetail != null){
    		caseProgressionDetail.setIsPSROrdered(event.getIsPSROrdered());
    		caseProgressionDetail.setVersion(event.getVersion());
    		caseProgressionDetailRepo.save(caseProgressionDetail);
	    }else{
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
	    }
	}

	@Transactional
	public void directionIssued(DirectionIssued event) {
		CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findById(event.getCaseProgressionId());
		if(caseProgressionDetail != null){
		     caseProgressionDetail.setVersion(event.getVersion());
		     caseProgressionDetail.setDirectionIssuedOn(event.getDirectionIssuedDate());
			caseProgressionDetailRepo.save(caseProgressionDetail);
		}else{
		    throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
		}
	}

	@Transactional
	public void indicateAllStatementsServed(AllStatementsServed event) {
	    CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findById(event.getCaseProgressionId()); 
		if(caseProgressionDetail != null){
    		caseProgressionDetail.setIsAllStatementsServed(Boolean.TRUE);
    		caseProgressionDetail.setVersion(event.getVersion());
    		caseProgressionDetailRepo.save(caseProgressionDetail);
		}else{
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
		}
	}

	@Transactional
	public void addDefenceIssues(DefenceIssuesAdded event) {

		CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findById(event.getCaseProgressionId());
		if(caseProgressionDetail != null){
		    caseProgressionDetail.setVersion(event.getVersion());
		    caseProgressionDetail.setDefenceIssue(event.getDefenceIssues());
			caseProgressionDetailRepo.save(caseProgressionDetail);
		}else{
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
		}
	}

	@Transactional
	public void addSFRIssues(SfrIssuesAdded event) {

		CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findById(event.getCaseProgressionId());
		if(caseProgressionDetail != null){
		    caseProgressionDetail.setVersion(event.getVersion());
		    caseProgressionDetail.setSfrIssue(event.getSfrIssues());
			caseProgressionDetailRepo.save(caseProgressionDetail);
		}else{
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
	}

	@Transactional
	public void addTrialEstimateDefence(DefenceTrialEstimateAdded event) {

	    CaseProgressionDetail caseProgressionDetail =  caseProgressionDetailRepo.findById(event.getCaseProgressionId());
		if(caseProgressionDetail != null){
		    caseProgressionDetail.setVersion(event.getVersion());
		    caseProgressionDetail.setTrialEstimateDefence( Long.valueOf(event.getDefenceTrialEstimate()));
			caseProgressionDetailRepo.save(caseProgressionDetail);
		}else{
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
	}

	@Transactional
	public void addTrialEstimateProsecution(ProsecutionTrialEstimateAdded event) {

	    CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findById(event.getCaseProgressionId());
		if(caseProgressionDetail != null){
		    caseProgressionDetail.setVersion(event.getVersion());
		    caseProgressionDetail.setTrialEstimateProsecution(Long.valueOf(event.getprosecutionTrialEstimate()));
			caseProgressionDetailRepo.save(caseProgressionDetail);
		}else{
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
	}

	public void vacatePtpHeaing(PTPHearingVacated event) {
	    CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findById(event.getCaseProgressionId());
	    if(caseProgressionDetail != null){
    		caseProgressionDetail.setPtpHearingVacatedDate(event.getPtpHearingVacatedDate());
    		caseProgressionDetail.setVersion(event.getVersion());
    		caseProgressionDetailRepo.save(caseProgressionDetail);
    	}else{
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
	}

	@Transactional
	public void addSendingCommittalHearingInformation(SendingCommittalHearingInformationAdded event) {

	    CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findById(event.getCaseProgressionId());
		if(caseProgressionDetail != null){
		    caseProgressionDetail.setVersion(event.getVersion());
		    caseProgressionDetail.setFromCourtCentre(event.getFromCourtCentre());
		    caseProgressionDetail.setSendingCommittalDate(event.getSendingCommittalDate());
			caseProgressionDetailRepo.save(caseProgressionDetail);
		}else{
            throw new NullPointerException(CASE_PROGRESSION_DETAIL_NOT_FOUND);
        }
	}
}
