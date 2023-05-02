package uk.gov.moj.cpp.progression.aggregate.rules;

public enum RetentionPolicyType {

    LIFE("4", 1),
    CUSTODIAL("3", 2),
    NON_CUSTODIAL("2", 3),
    REMITTAL("2", 4),
    ACQUITTAL("1", 5),
    NOT_GUILTY("1", 6);

    private String policyCode;
    private int priority;

    RetentionPolicyType(final String policyCode, final int priority) {
        this.policyCode = policyCode;
        this.priority = priority;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public int getPriority() {
        return priority;
    }
}
