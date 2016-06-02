package uk.gov.moj.cpp.progression.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;

public class CaseAddedToCrownCourtToCaseProgressionDetailConverter implements Converter<CaseAddedToCrownCourt, CaseProgressionDetail> {

    @Override
    public CaseProgressionDetail convert(CaseAddedToCrownCourt event) {
    	
        CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setId(event.getCaseProgressionId());
        caseProgressionDetail.setCaseId(event.getCaseId());
        caseProgressionDetail.setCourtCentreId(event.getCourtCentreId());
        return caseProgressionDetail;
    }
}
