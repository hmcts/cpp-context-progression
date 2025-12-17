package uk.gov.moj.cpp.progression.value.object;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@EqualsAndHashCode
@Getter
@ToString
public class DefenceOrganisationVO {
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String addressLine4;
    private String postcode;
    private String email;
    private String phoneNumber;
}


