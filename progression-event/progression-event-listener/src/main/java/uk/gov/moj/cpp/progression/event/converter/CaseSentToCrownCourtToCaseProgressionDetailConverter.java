package uk.gov.moj.cpp.progression.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.progression.domain.event.CaseSentToCrownCourt;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;

public class CaseSentToCrownCourtToCaseProgressionDetailConverter implements Converter<CaseSentToCrownCourt, CaseProgressionDetail> {

    @Override
    public CaseProgressionDetail convert(CaseSentToCrownCourt event) {
    	
		CaseProgressionDetail caseProgressionDetail=new CaseProgressionDetail();
		caseProgressionDetail.setCaseId(event.getCaseId());
		caseProgressionDetail.setDateOfSending(event.getDateOfSending());
		caseProgressionDetail.setId(event.getCaseProgressionId());
        return caseProgressionDetail;
    }
}
