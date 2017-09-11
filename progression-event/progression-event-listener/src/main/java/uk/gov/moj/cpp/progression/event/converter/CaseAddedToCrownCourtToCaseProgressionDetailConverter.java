package uk.gov.moj.cpp.progression.event.converter;

import java.util.UUID;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

public class CaseAddedToCrownCourtToCaseProgressionDetailConverter
                implements Converter<CaseAddedToCrownCourt, CaseProgressionDetail> {

    @Override
    public CaseProgressionDetail convert(CaseAddedToCrownCourt event) {

        CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setId(event.getCaseProgressionId());
        caseProgressionDetail.setCaseId(event.getCaseId());
        caseProgressionDetail.setCourtCentreId(event.getCourtCentreId());
        caseProgressionDetail.setStatus(event.getStatus());

        if (event.getDefendants() != null) {
            event.getDefendants().stream()
                            .forEach(s -> caseProgressionDetail.getDefendants()
                                            .add(new Defendant(UUID.randomUUID(), s.getId(),
                                                            caseProgressionDetail, false)));
        }
        return caseProgressionDetail;
    }
}
