package uk.gov.moj.cpp.progression.aggregate.rules;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyPriorityHelper.periodToDays;

import java.io.Serializable;
import java.util.Objects;

public class RetentionPolicy implements Serializable {

    private final RetentionPolicyType policyType;
    private final String period;
    private final HearingInfo hearingInfo;

    public RetentionPolicy(final RetentionPolicyType policyType, final String period, final HearingInfo hearingInfo) {
        this.policyType = policyType;
        this.period = period;
        this.hearingInfo = hearingInfo;
    }

    public RetentionPolicyType getPolicyType() {
        return policyType;
    }

    public String getPeriod() {
        return period;
    }

    public HearingInfo getHearingInfo() {
        return hearingInfo;
    }

    public int getPeriodDays() {
        if (isBlank(period)) {
            return 0;
        }
        return periodToDays(period);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RetentionPolicy that = (RetentionPolicy) o;
        return Objects.equals(policyType, that.policyType) &&
                Objects.equals(period, that.period);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyType, period);
    }

    @Override
    public String toString() {
        return "RetentionPolicy{" +
                "policyType=" + policyType +
                ", period='" + period + '\'' +
                '}';
    }
}
