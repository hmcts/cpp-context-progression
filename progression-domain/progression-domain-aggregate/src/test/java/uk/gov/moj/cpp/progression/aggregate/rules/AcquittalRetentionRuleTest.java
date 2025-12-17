package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.DefendantJudicialResult.defendantJudicialResult;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.moj.cpp.progression.aggregate.rules.AcquittalRetentionRule.ACQUITTAL_SENTENCE;
import static uk.gov.moj.cpp.progression.aggregate.rules.AcquittalRetentionRule.DISCH_RESULT_DEFINITION_ID;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.ACQUITTAL;

import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;

import java.util.List;

import org.junit.jupiter.api.Test;

public class AcquittalRetentionRuleTest {

    private AcquittalRetentionRule acquittalRetentionRule;
    private HearingInfo hearingInfo = new HearingInfo(randomUUID(), "hearingType", JurisdictionType.CROWN.name(),
            randomUUID(), "courtCentreName", randomUUID(), "courtRoomName");

    @Test
    public void shouldReturnFalseWhenNullDefendantJudicialResultsAndOffences() {
        acquittalRetentionRule = new AcquittalRetentionRule(hearingInfo, null, null);
        assertThat(acquittalRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenEmptyDefendantJudicialResultsAndOffences() {
        acquittalRetentionRule = new AcquittalRetentionRule(hearingInfo, emptyList(), emptyList());
        assertThat(acquittalRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenDefendantJudicialResultsAndOffencesHaveEmptyJudicialResults() {
        final List<Offence> offences = asList(Offence.offence().withJudicialResults(emptyList()).build(),
                Offence.offence().withJudicialResults(emptyList()).build());
        final List<DefendantJudicialResult> defendantJudicialResults = asList(defendantJudicialResult()
                .build(), defendantJudicialResult().withJudicialResult(judicialResult().withCategory(JudicialResultCategory.FINAL).build()).build());

        acquittalRetentionRule = new AcquittalRetentionRule(hearingInfo, defendantJudicialResults, offences);

        assertThat(acquittalRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoOffenceResultedDischarged() {
        final List<Offence> offences = asList(Offence.offence()
                        .withJudicialResults(singletonList(judicialResult()
                                .withCategory(JudicialResultCategory.FINAL)
                                .build())).build(),
                Offence.offence()
                        .withJudicialResults(singletonList(judicialResult()
                                .withCategory(JudicialResultCategory.FINAL)
                                .build())).build());
        acquittalRetentionRule = new AcquittalRetentionRule(hearingInfo, emptyList(), offences);

        assertThat(acquittalRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnTrueWhenAOffenceResultedAsDischarged() {
        final List<Offence> offences = asList(Offence.offence()
                        .withJudicialResults(singletonList(judicialResult()
                                .withCategory(JudicialResultCategory.FINAL)
                                .withJudicialResultTypeId(DISCH_RESULT_DEFINITION_ID)
                                .build())).build(),
                Offence.offence()
                        .withJudicialResults(singletonList(judicialResult()
                                .withCategory(JudicialResultCategory.FINAL)
                                .build())).build());
        acquittalRetentionRule = new AcquittalRetentionRule(hearingInfo, emptyList(), offences);

        assertThat(acquittalRetentionRule.apply(), is(true));
        assertThat(acquittalRetentionRule.getPolicy().getPolicyType(), is(ACQUITTAL));
        assertThat(acquittalRetentionRule.getPolicy().getPolicyType().getPolicyCode(), is("1"));
        assertThat(acquittalRetentionRule.getPolicy().getPeriod(), is(ACQUITTAL_SENTENCE));
    }

    @Test
    public void shouldReturnTrueWhenDefendantJudicialResultsWithDischargedResult() {
        final List<Offence> offences = asList(Offence.offence().withJudicialResults(emptyList()).build(),
                Offence.offence().withJudicialResults(emptyList()).build());
        final List<DefendantJudicialResult> defendantJudicialResults = asList(defendantJudicialResult()
                .build(), defendantJudicialResult().withJudicialResult(judicialResult()
                .withCategory(JudicialResultCategory.FINAL)
                .withJudicialResultTypeId(DISCH_RESULT_DEFINITION_ID)
                .build()).build());

        acquittalRetentionRule = new AcquittalRetentionRule(hearingInfo, defendantJudicialResults, offences);

        assertThat(acquittalRetentionRule.apply(), is(true));
    }
}