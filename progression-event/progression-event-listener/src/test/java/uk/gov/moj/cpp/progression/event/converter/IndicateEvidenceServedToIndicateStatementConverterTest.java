package uk.gov.moj.cpp.progression.event.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import uk.gov.moj.cpp.progression.domain.event.IndicateEvidenceServed;
import uk.gov.moj.cpp.progression.persistence.entity.IndicateStatement;

public class IndicateEvidenceServedToIndicateStatementConverterTest {

    private static final UUID CASE_PROGRESSION_ID = UUID.randomUUID();

    private static final UUID CASE_ID = UUID.randomUUID();

	private static final boolean KEY_EVIDENCE = true;

	private static final String EVIDENCE_NAME = "Evidence name";
	
	private static final LocalDate PLAN_DATE = LocalDate.now();
	
	private static final long VERSION = 1l;

    private IndicateEvidenceServedToIndicateStatementConverter converter;
    
    private IndicateEvidenceServed event;

    @Before
    public void setUp() throws Exception {
        converter = new IndicateEvidenceServedToIndicateStatementConverter();
        
        event = new IndicateEvidenceServed(CASE_PROGRESSION_ID, VERSION, CASE_ID, PLAN_DATE, EVIDENCE_NAME, KEY_EVIDENCE);
    }

    @Test
    public void testConvert() throws Exception {
        IndicateStatement cpd = converter.convert(event);

        assertThat(cpd.getId(), equalTo(CASE_PROGRESSION_ID));
        assertThat(cpd.getCaseId(), equalTo(CASE_ID));
        assertThat(cpd.getEvidenceName(), equalTo(EVIDENCE_NAME));
        assertThat(cpd.getIsKeyEvidence(), equalTo(KEY_EVIDENCE));
        assertThat(cpd.getPlanDate(), equalTo(PLAN_DATE));
        assertThat(cpd.getVersion(), equalTo(VERSION));
    }

}
