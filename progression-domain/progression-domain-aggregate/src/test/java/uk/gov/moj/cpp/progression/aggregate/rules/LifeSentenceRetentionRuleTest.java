package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt;
import static uk.gov.moj.cpp.progression.aggregate.rules.LifeSentenceRetentionRule.LIFE_JUDICIAL_RESULT_PROMPT_REF;
import static uk.gov.moj.cpp.progression.aggregate.rules.LifeSentenceRetentionRule.LIFE_JUDICIAL_RESULT_PROMPT_TYPE_ID;
import static uk.gov.moj.cpp.progression.aggregate.rules.LifeSentenceRetentionRule.LIFE_SENTENCE;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.LIFE;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;

import java.util.List;

import org.junit.jupiter.api.Test;

public class LifeSentenceRetentionRuleTest {

    private LifeSentenceRetentionRule lifeSentenceRetentionRule;
    private HearingInfo hearingInfo = new HearingInfo(randomUUID(), "hearingType", JurisdictionType.CROWN.name(),
            randomUUID(), "courtCentreName", randomUUID(), "courtRoomName");

    @Test
    public void shouldReturnFalseWhenNullOffences() {
        lifeSentenceRetentionRule = new LifeSentenceRetentionRule(hearingInfo, null);
        assertThat(lifeSentenceRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenEmptyOffences() {
        lifeSentenceRetentionRule = new LifeSentenceRetentionRule(hearingInfo, emptyList());
        assertThat(lifeSentenceRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoJudicialResultsHaveLifeDuration() {
        final List<Offence> offences = asList(Offence.offence()
                        .withJudicialResults(singletonList(JudicialResult.judicialResult().withJudicialResultId(randomUUID())
                                .withLifeDuration(Boolean.FALSE).build())).build(),
                Offence.offence()
                        .withJudicialResults(asList(JudicialResult.judicialResult().withJudicialResultId(randomUUID())
                                        .withLifeDuration(Boolean.FALSE).build(),
                                JudicialResult.judicialResult().withJudicialResultId(randomUUID())
                                        .withLifeDuration(Boolean.FALSE).build())).build());
        lifeSentenceRetentionRule = new LifeSentenceRetentionRule(hearingInfo, offences);

        assertThat(lifeSentenceRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnTrueWhenAnyJudicialResultHasLifeDuration() {
        final List<Offence> offences = asList(Offence.offence()
                        .withJudicialResults(singletonList(JudicialResult.judicialResult().withJudicialResultId(randomUUID())
                                .withLifeDuration(Boolean.FALSE).build())).build(),
                Offence.offence()
                        .withJudicialResults(asList(JudicialResult.judicialResult().withJudicialResultId(randomUUID())
                                        .withJudicialResultPrompts(singletonList(judicialResultPrompt()
                                                .withJudicialResultPromptTypeId(LIFE_JUDICIAL_RESULT_PROMPT_TYPE_ID)
                                                .withPromptReference(LIFE_JUDICIAL_RESULT_PROMPT_REF)
                                                .withValue("true")
                                                .build()))
                                        .withJudicialResultTypeId(randomUUID())
                                        .build(),
                                JudicialResult.judicialResult().withJudicialResultId(randomUUID())
                                        .withLifeDuration(Boolean.FALSE).build())).build());
        lifeSentenceRetentionRule = new LifeSentenceRetentionRule(hearingInfo, offences);

        assertThat(lifeSentenceRetentionRule.apply(), is(true));
        assertThat(lifeSentenceRetentionRule.getPolicy().getPolicyType(), is(LIFE));
        assertThat(lifeSentenceRetentionRule.getPolicy().getPolicyType().getPolicyCode(), is("4"));
        assertThat(lifeSentenceRetentionRule.getPolicy().getPeriod(), is(LIFE_SENTENCE));
    }

}