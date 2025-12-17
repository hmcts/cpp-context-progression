package uk.gov.moj.cpp.progression.value.object;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@EqualsAndHashCode
@Getter
@ToString
public class DefendantVO {
    private String firstName;
    private String lastName;
    private String middleName;
    private String legalEntityName;
}

