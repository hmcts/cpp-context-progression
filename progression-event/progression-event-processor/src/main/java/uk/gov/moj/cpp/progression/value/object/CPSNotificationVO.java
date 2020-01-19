package uk.gov.moj.cpp.progression.value.object;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;


@Builder
@EqualsAndHashCode
@Getter
@ToString
public class CPSNotificationVO  {
    private String cpsEmailAddress;
    private EmailTemplateType templateType;
    private Optional<CaseVO> caseVO;
    private Optional<DefendantVO> defendantVO;
    private HearingVO hearingVO;
    private Optional<DefenceOrganisationVO> defenceOrganisationVO;
}






