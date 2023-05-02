package uk.gov.moj.cpp.progression.aggregate.rules;

import static uk.gov.moj.cpp.progression.aggregate.rules.NonCustodialSentenceRetentionRule.NON_CUSTODIAL_SENTENCE;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.NON_CUSTODIAL;

public class DefaultRetentionRule implements RetentionRule {
    private HearingInfo hearingInfo;

    public DefaultRetentionRule(final HearingInfo hearingInfo){
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
