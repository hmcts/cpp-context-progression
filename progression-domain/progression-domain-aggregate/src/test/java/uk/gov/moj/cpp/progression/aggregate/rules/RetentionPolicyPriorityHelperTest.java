package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyPriorityHelper.getRetentionPolicyByPriority;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.ACQUITTAL;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.CUSTODIAL;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.LIFE;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.NON_CUSTODIAL;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.NOT_GUILTY;

import uk.gov.justice.core.courts.JurisdictionType;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class RetentionPolicyPriorityHelperTest {
    private HearingInfo hearingInfo = new HearingInfo(randomUUID(), "hearingType", JurisdictionType.CROWN.name(),
            randomUUID(), "courtCentreName", randomUUID(), "courtRoomName");

    @Test
    public void shouldReturnRetentionPolicyThatIsHighPriority() {
        final List<RetentionPolicy> retentionPolicies = new ArrayList<>();
        retentionPolicies.add(new RetentionPolicy(LIFE, "99Y0M0D", hearingInfo));
        retentionPolicies.add(new RetentionPolicy(NON_CUSTODIAL, "1Y0M0D", hearingInfo));
        retentionPolicies.add(new RetentionPolicy(NOT_GUILTY, "1Y0M0D", hearingInfo));

        final RetentionPolicy retentionPolicyByPriority = getRetentionPolicyByPriority(retentionPolicies);

        assertThat(retentionPolicyByPriority.getPolicyType(), is(LIFE));
        assertThat(retentionPolicyByPriority.getPeriodDays(), is(36135));
    }

    @Test
    public void shouldReturnRetentionPolicyWhenThePrioritySame() {
        final List<RetentionPolicy> retentionPolicies = new ArrayList<>();
        retentionPolicies.add(new RetentionPolicy(ACQUITTAL, "1Y0M0D", hearingInfo));
        retentionPolicies.add(new RetentionPolicy(ACQUITTAL, "1Y0M0D", hearingInfo));

        final RetentionPolicy retentionPolicyByPriority = getRetentionPolicyByPriority(retentionPolicies);

        assertThat(retentionPolicyByPriority.getPolicyType(), is(ACQUITTAL));
        assertThat(retentionPolicyByPriority.getPeriodDays(), is(365));
    }

    @Test
    public void shouldReturnRetentionPolicyWithHighSentenceWhenThePrioritySame() {
        final List<RetentionPolicy> retentionPolicies = new ArrayList<>();
        retentionPolicies.add(new RetentionPolicy(CUSTODIAL, "1Y1M1D", hearingInfo));
        retentionPolicies.add(new RetentionPolicy(ACQUITTAL, "1Y0M0D", hearingInfo));
        retentionPolicies.add(new RetentionPolicy(CUSTODIAL, "1Y0M1D", hearingInfo));

        final RetentionPolicy retentionPolicyByPriority = getRetentionPolicyByPriority(retentionPolicies);

        assertThat(retentionPolicyByPriority.getPolicyType(), is(CUSTODIAL));
        assertThat(retentionPolicyByPriority.getPeriodDays(), is(396));
    }


}