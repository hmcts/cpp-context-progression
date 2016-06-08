package uk.gov.moj.cpp.progression.query.view.converter;

import uk.gov.moj.cpp.progression.persistence.entity.IndicateStatement;
import uk.gov.moj.cpp.progression.query.view.response.IndicateStatementsDetailView;

public class IndicateStatementsDetailToViewConverter {

    public IndicateStatementsDetailView convert(IndicateStatement indicateStatements) {
        IndicateStatementsDetailView indicateStatementsDetailVo = null;
        if (indicateStatements != null) {
            indicateStatementsDetailVo = new IndicateStatementsDetailView();
            indicateStatementsDetailVo.setId(indicateStatements.getId().toString());
            indicateStatementsDetailVo.setCaseId(indicateStatements.getCaseId().toString());
            indicateStatementsDetailVo.setPlanDate(indicateStatements.getPlanDate());
            indicateStatementsDetailVo.setIsKeyEvidence(indicateStatements.getIsKeyEvidence().toString());
            indicateStatementsDetailVo.setEvidenceName(indicateStatements.getEvidenceName());
        }
        return indicateStatementsDetailVo;
    }
}
