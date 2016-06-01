package uk.gov.moj.cpp.progression.domain;

import static uk.gov.justice.domain.aggregate.condition.Precondition.assertPrecondition;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.progression.domain.event.IndicateEvidenceServed;

/**
 * 
 * @author jchondig
 *
 */
public class IndicateStatement implements Aggregate {
	private UUID indicateStatementId;
	private UUID caseId;
	private LocalDate planDate;
	private String evidenceName;
    private Boolean isKeyEvidence;
    
    private static final Long INITIAL_VERSION = 0l;
    

	public Stream<Object> serveIndicateEvidence(final UUID indicateStatementId, final UUID caseId,final LocalDate planDate, String evidenceName, Boolean isKeyEvidence) {
		assertPrecondition(this.indicateStatementId == null).orElseThrow("Indicate statement already added");
		return apply(Stream.of(new IndicateEvidenceServed(indicateStatementId, INITIAL_VERSION, caseId, planDate,evidenceName,isKeyEvidence)));
	}
    
	@Override
	public Object apply(Object event) {
		return match(event).with(when(IndicateEvidenceServed.class).apply(indicateStatement -> {
			indicateStatementId = indicateStatement.getIndicateEvidenceServedId();
			caseId = indicateStatement.getCaseId();
			planDate = indicateStatement.getPlanDate();
			evidenceName = indicateStatement.getEvidenceName();
			isKeyEvidence= indicateStatement.getIsKeyEvidence();
		}));
	}

   

   
}
