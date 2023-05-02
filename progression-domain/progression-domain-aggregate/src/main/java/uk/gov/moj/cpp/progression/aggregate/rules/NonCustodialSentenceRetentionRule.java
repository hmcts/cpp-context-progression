package uk.gov.moj.cpp.progression.aggregate.rules;

import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.NON_CUSTODIAL;

public class NonCustodialSentenceRetentionRule implements RetentionRule {

    static final String NON_CUSTODIAL_SENTENCE = "7Y0M0D";
    private HearingInfo hearingInfo;

    public NonCustodialSentenceRetentionRule(final HearingInfo hearingInfo) {
        this.hearingInfo = hearingInfo;
    }

    @Override
    public boolean apply() {
        return true;
    }

    @Override
    public RetentionPolicy getPolicy() {
        return new RetentionPolicy(NON_CUSTODIAL, NON_CUSTODIAL_SENTENCE, hearingInfo);
    }
}
