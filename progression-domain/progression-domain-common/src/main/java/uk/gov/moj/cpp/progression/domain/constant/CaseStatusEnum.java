package uk.gov.moj.cpp.progression.domain.constant;

/**
 * 
 * @author jchondig
 *
 */
public enum CaseStatusEnum {

    INCOMPLETE("INCOMPLETE"), READY_FOR_REVIEW("READY_FOR_REVIEW"), IN_REVIEW("IN_REVIEW"), REVIEW_COMPLETE("REVIEW_COMPLETE"), COMPLETED("COMPLETED");
    //IN_REVIEW is  ASSIGNED_TO_JUDGE
    //READY_FOR_REVIEW  is TO_BE_ASSIGNED

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
