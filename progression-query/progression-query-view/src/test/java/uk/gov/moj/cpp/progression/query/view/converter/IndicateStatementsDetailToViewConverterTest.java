package uk.gov.moj.cpp.progression.query.view.converter;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.persistence.entity.IndicateStatement;
import uk.gov.moj.cpp.progression.query.view.converter.IndicateStatementsDetailToViewConverter;
import uk.gov.moj.cpp.progression.query.view.response.IndicateStatementsDetailView;

public class IndicateStatementsDetailToViewConverterTest {
    private static final UUID ID = UUID.randomUUID();
    private static final UUID CASEID = UUID.randomUUID();
    private static final String EVIDENCENAME = "evidence name";

    @Test
    public void getIndicateStatementsDetailTest() {
        IndicateStatementsDetailToViewConverter indicateStatementsDetailToVOConverter = new IndicateStatementsDetailToViewConverter();

        IndicateStatement indicateStatement = new IndicateStatement();
        indicateStatement.setId( ID);
        indicateStatement.setPlanDate(LocalDate.now());
        indicateStatement.setCaseId(CASEID);
        indicateStatement.setEvidenceName(EVIDENCENAME);
        indicateStatement.setIsKeyEvidence(true);

        IndicateStatementsDetailView caseProgressionDetailVO = indicateStatementsDetailToVOConverter.convert(indicateStatement);
        assertTrue(caseProgressionDetailVO.getCaseId().equals(CASEID.toString()));
        assertTrue(caseProgressionDetailVO.getIndicateStatementId().equals(ID.toString()));
        assertTrue(caseProgressionDetailVO.getPlanDate().equals((LocalDate.now())));
        assertTrue(caseProgressionDetailVO.getEvidenceName().equals(EVIDENCENAME));
        assertTrue(caseProgressionDetailVO.getIsKeyEvidence().equalsIgnoreCase("true"));
    }

    @Test
    public void getIndicateStatementsDetailNullTest() {
        IndicateStatementsDetailToViewConverter indicateStatementsDetailToVOConverter = new IndicateStatementsDetailToViewConverter();
        assertNull(indicateStatementsDetailToVOConverter.convert(null));
    }
}
