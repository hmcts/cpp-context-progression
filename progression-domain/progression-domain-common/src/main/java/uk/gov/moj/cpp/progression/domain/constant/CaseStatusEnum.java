package uk.gov.moj.cpp.progression.domain.constant;

public enum CaseStatusEnum {

    INCOMPLETE("INCOMPLETE"),
    READY_FOR_REVIEW("READY_FOR_REVIEW"),
    SJP_REFERRAL("SJP_REFERRAL"),
    INACTIVE("INACTIVE"),
    ACTIVE("ACTIVE"),
    CLOSED("CLOSED");

    private String description;

    private CaseStatusEnum(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static CaseStatusEnum getCaseStatusk(final String value) {
        final CaseStatusEnum[] caseStatusArray = CaseStatusEnum.values();
        for (final CaseStatusEnum caseStatus : caseStatusArray) {
            if (caseStatus.getDescription().equals(value)) {
                return caseStatus;
            }
        }
        return null;
    }

}
