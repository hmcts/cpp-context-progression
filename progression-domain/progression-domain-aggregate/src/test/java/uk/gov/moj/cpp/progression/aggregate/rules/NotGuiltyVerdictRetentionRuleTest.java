package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.aggregate.rules.NotGuiltyVerdictRetentionRule.NOT_GUILTY_SENTENCE;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.NOT_GUILTY;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;

import java.util.List;

import org.junit.jupiter.api.Test;

public class NotGuiltyVerdictRetentionRuleTest {

    private NotGuiltyVerdictRetentionRule notGuiltyVerdictRetentionRule;
    private HearingInfo hearingInfo = new HearingInfo(randomUUID(), "hearingType", JurisdictionType.CROWN.name(),
            randomUUID(), "courtCentreName", randomUUID(), "courtRoomName");

    @Test
    public void shouldReturnFalseWhenNullOffences() {
        notGuiltyVerdictRetentionRule = new NotGuiltyVerdictRetentionRule(hearingInfo, null);
        assertThat(notGuiltyVerdictRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenEmptyOffences() {
        notGuiltyVerdictRetentionRule = new NotGuiltyVerdictRetentionRule(hearingInfo, emptyList());
        assertThat(notGuiltyVerdictRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenAllOffencesHaveGuiltyVerdict() {
        final List<Offence> offences = asList(Offence.offence()
                        .withVerdict(getVerdict("GUILTY", "FPR")).build(),
                Offence.offence()
                        .withVerdict(getVerdict("GUILTY", "FPR")).build());
        notGuiltyVerdictRetentionRule = new NotGuiltyVerdictRetentionRule(hearingInfo, offences);

        assertThat(notGuiltyVerdictRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenOneOffenceGuilty() {
        final List<Offence> offences = asList(Offence.offence()
                        .withVerdict(getVerdict("GUILTY", "FPR")).build(),
                Offence.offence()
                        .withVerdict(getVerdict("NOT_GUILTY", "N")).build());
        notGuiltyVerdictRetentionRule = new NotGuiltyVerdictRetentionRule(hearingInfo, offences);

        assertThat(notGuiltyVerdictRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnTrueWhenAllOffencesHaveNotGuiltyVerdict() {
        final List<Offence> offences = asList(Offence.offence()
                        .withVerdict(getVerdict("NOT_GUILTY", "N")).build(),
                Offence.offence()
                        .withVerdict(getVerdict("NOT_GUILTY", "N")).build());
        notGuiltyVerdictRetentionRule = new NotGuiltyVerdictRetentionRule(hearingInfo, offences);

        assertThat(notGuiltyVerdictRetentionRule.apply(), is(true));
        assertThat(notGuiltyVerdictRetentionRule.getPolicy().getPolicyType(), is(NOT_GUILTY));
        assertThat(notGuiltyVerdictRetentionRule.getPolicy().getPolicyType().getPolicyCode(), is("1"));
        assertThat(notGuiltyVerdictRetentionRule.getPolicy().getPeriod(), is(NOT_GUILTY_SENTENCE));
    }

    protected static Verdict getVerdict(final String verdictType, final String verdictCode) {
        return Verdict.verdict()
                .withVerdictType(VerdictType.verdictType()
                        .withCjsVerdictCode(verdictCode)
                        .withCategoryType(verdictType).build()).build();
    }

}