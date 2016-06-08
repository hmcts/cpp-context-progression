package uk.gov.moj.cpp.progression.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.progression.domain.event.IndicateEvidenceServed;
import uk.gov.moj.cpp.progression.persistence.entity.IndicateStatement;

public class IndicateEvidenceServedToIndicateStatementConverter implements Converter<IndicateEvidenceServed, IndicateStatement> {

    @Override
    public IndicateStatement convert(IndicateEvidenceServed event) {

        IndicateStatement indicateStatement = new IndicateStatement();
        indicateStatement.setCaseId(event.getCaseId());
        indicateStatement.setIsKeyEvidence(event.getIsKeyEvidence());
        indicateStatement.setPlanDate(event.getPlanDate());
        indicateStatement.setEvidenceName(event.getEvidenceName());
        indicateStatement.setId(event.getIndicateEvidenceServedId());
        return indicateStatement;
    }
}
