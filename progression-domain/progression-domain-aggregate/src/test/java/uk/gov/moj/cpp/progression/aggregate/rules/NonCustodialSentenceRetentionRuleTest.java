package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.NON_CUSTODIAL;

import uk.gov.justice.core.courts.JurisdictionType;

import org.junit.jupiter.api.Test;

public class NonCustodialSentenceRetentionRuleTest {

    private NonCustodialSentenceRetentionRule nonCustodialSentenceRetentionRule;
    private HearingInfo hearingInfo = new HearingInfo(randomUUID(), "hearingType", JurisdictionType.CROWN.name(),
            randomUUID(), "courtCentreName", randomUUID(), "courtRoomName");

    @Test
    public void shouldReturnTrueForDefaultNonCustodialRule() {
        nonCustodialSentenceRetentionRule = new NonCustodialSentenceRetentionRule(hearingInfo);
        assertThat(nonCustodialSentenceRetentionRule.apply(), is(true));
        assertThat(nonCustodialSentenceRetentionRule.getPolicy().getPolicyType(), is(NON_CUSTODIAL));
        assertThat(nonCustodialSentenceRetentionRule.getPolicy().getPolicyType().getPolicyCode(), is("2"));
        assertThat(nonCustodialSentenceRetentionRule.getPolicy().getPeriod(), is("7Y0M0D"));
    }

}