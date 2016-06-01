package uk.gov.moj.cpp.progression.domain;

import static uk.gov.justice.domain.aggregate.condition.Precondition.assertPrecondition;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.progression.domain.event.AllStatementsIdentified;
import uk.gov.moj.cpp.progression.domain.event.AllStatementsServed;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CaseSentToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.DefenceIssuesAdded;
import uk.gov.moj.cpp.progression.domain.event.DefenceTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.domain.event.PTPHearingVacated;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportOrdered;
import uk.gov.moj.cpp.progression.domain.event.ProsecutionTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SfrIssuesAdded;

public class CaseProgressionDetails implements Aggregate {
	
	
	private static final String CASE_PROGRESSION_DETAILS_DOES_NOT_EXIST = "Case progression details does not exist";

    private static final Long INITIAL_VERSION = 0L;

	private UUID caseProgressionId;
	private UUID caseId;
	private LocalDate dateOfSendng;
	private String defenceIssues;
	private String sfrIssues;
	private Long version;
	private String courtCentreId;
	private String fromCourtCentre;
    private LocalDate sendingCommittalDate;
    private int defenceTrialEstimate;
    private Boolean isPSROrdered;

	public Stream<Object> sendCaseToCrownCourt(final UUID caseProgressionId, final UUID caseId) {
		//assertPrecondition(this.caseProgressionId == null).orElseThrow("Case progression details already added");
		return apply(Stream.of(new CaseSentToCrownCourt(caseProgressionId, caseId, LocalDate.now(), INITIAL_VERSION)));
	}
	
	public Stream<Object> addCaseToCrownCourt(final UUID caseProgressionId, final UUID caseId, final String courtCentreId) {
		//assertPrecondition(this.caseProgressionId == null).orElseThrow("Case progression details already added");
		return apply(Stream.of(new CaseAddedToCrownCourt(caseProgressionId, caseId, courtCentreId, INITIAL_VERSION)));
	}
	
	public Stream<Object> addDefenceIssues(final UUID caseProgressionId, final String defenceIssues, final Long version) {
		assertPrecondition(this.caseProgressionId != null).orElseThrow(CASE_PROGRESSION_DETAILS_DOES_NOT_EXIST);
		return apply(Stream.of(new DefenceIssuesAdded(caseProgressionId, defenceIssues, version)));
	}
	
	public Stream<Object> addSfrIssues(final UUID caseProgressionId, final String sfrIssues, final Long version) {
		assertPrecondition(this.caseProgressionId != null).orElseThrow(CASE_PROGRESSION_DETAILS_DOES_NOT_EXIST);
		return apply(Stream.of(new SfrIssuesAdded(caseProgressionId, sfrIssues, version)));
	}

    public Stream<Object> sendCommittalHearingInformation(UUID caseProgressionId,  String fromCourtCentre,
            LocalDate sendingCommittalDate, Long version) {
        assertPrecondition(this.caseProgressionId != null)
                .orElseThrow(CASE_PROGRESSION_DETAILS_DOES_NOT_EXIST);
        return apply(Stream.of(new SendingCommittalHearingInformationAdded(
                caseProgressionId,fromCourtCentre, sendingCommittalDate, version)));
    }
	
    public Stream<Object> addDefenceTrialEstimate(final UUID caseProgressionId, final int defenceTrialEstimate, final Long version) {
		assertPrecondition(this.caseProgressionId != null).orElseThrow(CASE_PROGRESSION_DETAILS_DOES_NOT_EXIST);
		return apply(Stream.of(new DefenceTrialEstimateAdded(caseProgressionId, defenceTrialEstimate, version)));
	}
    
    public Stream<Object> addProsecutionTrialEstimate(final UUID caseProgressionId, final int prosecutionTrialEstimate, final Long version) {
        assertPrecondition(this.caseProgressionId != null).orElseThrow(CASE_PROGRESSION_DETAILS_DOES_NOT_EXIST);
        return apply(Stream.of(new ProsecutionTrialEstimateAdded(caseProgressionId, prosecutionTrialEstimate, version)));
    }
    
    public Stream<Object> issueDirection(UUID caseProgressionId,
            Long version) {
        assertPrecondition(this.caseProgressionId != null).orElseThrow(CASE_PROGRESSION_DETAILS_DOES_NOT_EXIST);
        return apply(Stream.of(new DirectionIssued(caseProgressionId,  version, LocalDate.now())));
    }
    
    public Stream<Object> preSentenceReport(UUID caseProgressionId, Boolean isPSROrdered,
            Long version) {
        assertPrecondition(this.caseProgressionId != null).orElseThrow(CASE_PROGRESSION_DETAILS_DOES_NOT_EXIST);
        return apply(Stream.of(new PreSentenceReportOrdered(caseProgressionId,isPSROrdered,  version)));
    }
    
    public Stream<Object> allStatementsIdentified(UUID caseProgressionId,
            Long version) {
        assertPrecondition(this.caseProgressionId != null).orElseThrow(CASE_PROGRESSION_DETAILS_DOES_NOT_EXIST);
        return apply(Stream.of(new AllStatementsIdentified(caseProgressionId,  version)));
    }
    
    public Stream<Object> allStatementsServed(UUID caseProgressionId,
            Long version) {
        assertPrecondition(this.caseProgressionId != null).orElseThrow(CASE_PROGRESSION_DETAILS_DOES_NOT_EXIST);
        return apply(Stream.of(new AllStatementsServed(caseProgressionId,  version)));
    }
    
    public Stream<Object> vacatePTPHearing(UUID caseProgressionId, LocalDate ptpHearingVacatedDate,
            Long version) {
        assertPrecondition(this.caseProgressionId != null).orElseThrow(CASE_PROGRESSION_DETAILS_DOES_NOT_EXIST);
        return apply(Stream.of(new PTPHearingVacated(caseProgressionId, ptpHearingVacatedDate,  version)));
    }
    
	@Override
	public Object apply(Object event) {
		return match(event).with(when(CaseSentToCrownCourt.class).apply(caseSentToCrownCourt -> {
			caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
			caseId = caseSentToCrownCourt.getCaseId();
			dateOfSendng = caseSentToCrownCourt.getDateOfSending();
		}), when(CaseAddedToCrownCourt.class).apply(caseSentToCrownCourt -> {
            caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
            caseId = caseSentToCrownCourt.getCaseId();
            courtCentreId = caseSentToCrownCourt.getCourtCentreId();
		}), when(DefenceIssuesAdded.class).apply(caseSentToCrownCourt -> {
			caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
			defenceIssues = caseSentToCrownCourt.getDefenceIssues();
			version = caseSentToCrownCourt.getVersion();
		}), when(SendingCommittalHearingInformationAdded.class).apply(caseSentToCrownCourt -> {
            caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
            fromCourtCentre = caseSentToCrownCourt.getFromCourtCentre();
            sendingCommittalDate= caseSentToCrownCourt.getSendingCommittalDate();
            version = caseSentToCrownCourt.getVersion();
		}), when(SfrIssuesAdded.class).apply(caseSentToCrownCourt -> {
			caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
			sfrIssues = caseSentToCrownCourt.getSfrIssues();
			version = caseSentToCrownCourt.getVersion();
		}), when(DefenceTrialEstimateAdded.class).apply(caseSentToCrownCourt -> {
			caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
			defenceTrialEstimate = caseSentToCrownCourt.getDefenceTrialEstimate();
			version = caseSentToCrownCourt.getVersion();
	    }), when(ProsecutionTrialEstimateAdded.class).apply(caseSentToCrownCourt -> {
            caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
            defenceTrialEstimate = caseSentToCrownCourt.getprosecutionTrialEstimate();
            version = caseSentToCrownCourt.getVersion();			
	    }), when(DirectionIssued.class).apply(caseSentToCrownCourt -> {
            caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
            version = caseSentToCrownCourt.getVersion();	
	    }), when(PreSentenceReportOrdered.class).apply(caseSentToCrownCourt -> {
            caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
            version = caseSentToCrownCourt.getVersion();  
            isPSROrdered = caseSentToCrownCourt.getIsPSROrdered();
        }), when(AllStatementsIdentified.class).apply(caseSentToCrownCourt -> {
            caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
            version = caseSentToCrownCourt.getVersion();    
        }), when(AllStatementsServed.class).apply(caseSentToCrownCourt -> {
            caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
            version = caseSentToCrownCourt.getVersion();   
        }), when(PTPHearingVacated.class).apply(caseSentToCrownCourt -> {
            caseProgressionId = caseSentToCrownCourt.getCaseProgressionId();
            version = caseSentToCrownCourt.getVersion();   
		}));
	}
   
}
