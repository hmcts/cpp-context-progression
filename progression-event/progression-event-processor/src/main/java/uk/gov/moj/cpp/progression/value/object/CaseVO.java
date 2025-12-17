package uk.gov.moj.cpp.progression.value.object;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Builder
@EqualsAndHashCode
@Getter
@ToString
public class CaseVO {
    private UUID caseId;
    private String caseURN;
    private String defendantList;
}
