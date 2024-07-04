package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt;
import static uk.gov.moj.cpp.progression.aggregate.rules.CustodialSentenceRetentionRule.TIMP_RESULT_DEFINITION_ID;
import static uk.gov.moj.cpp.progression.aggregate.rules.CustodialSentenceRetentionRule.TOTAL_CUSTODIAL_PERIOD_PROMPT;
import static uk.gov.moj.cpp.progression.aggregate.rules.CustodialSentenceRetentionRule.TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.CUSTODIAL;

import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

public class CustodialSentenceRetentionRuleTest {

    private CustodialSentenceRetentionRule custodialSentenceRetentionRule;
    private HearingInfo hearingInfo = new HearingInfo(randomUUID(), "hearingType", JurisdictionType.CROWN.name(),
            randomUUID(), "courtCentreName", randomUUID(), "courtRoomName");

    @Test
    public void shouldReturnFalseWhenDefendantJudicialResultsNull() {
        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, null, null);

        assertThat(custodialSentenceRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenDefendantJudicialResultsEmpty() {
        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, emptyList(), emptyList());

        assertThat(custodialSentenceRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoJudicialResultInDefendantJudicialResults() {
        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult().build());
        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, emptyList());

        assertThat(custodialSentenceRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoDefendantJudicialResultsOfTypeCustodial() {
        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult().withJudicialResult(JudicialResult.judicialResult().build()).build());
        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, emptyList());

        assertThat(custodialSentenceRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnTrueWhenDefendantJudicialResultsHadJudicialResultTypeCustodialTIMP() {
        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withOrderedDate(LocalDate.now())
                        .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                        .withJudicialResultPrompts(asList(judicialResultPrompt()
                                        .withJudicialResultPromptTypeId(TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID)
                                        .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                                        .withValue("10 Years")
                                        .build(),
                                judicialResultPrompt()
                                        .withPromptReference("imprisonmentPeriod")
                                        .withValue("1 Years")
                                        .build()))
                        .build()).build());

        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, emptyList());

        assertThat(custodialSentenceRetentionRule.apply(), is(true));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPolicyType(), is(CUSTODIAL));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPeriod(), is("10Y0M0D"));
    }

    @Test
    public void shouldReturnTrueWhenDefendantJudicialResultsHadJudicialResultTypeCustodialTIMP_Multi() {
        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withOrderedDate(LocalDate.now())
                        .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                        .withJudicialResultPrompts(asList(judicialResultPrompt()
                                        .withJudicialResultPromptTypeId(TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID)
                                        .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                                        .withValue("10 Years 3 Months 25 Days")
                                        .build(),
                                judicialResultPrompt()
                                        .withPromptReference("imprisonmentPeriod")
                                        .withValue("1 Years")
                                        .build()))
                        .build()).build());

        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, emptyList());

        assertThat(custodialSentenceRetentionRule.apply(), is(true));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPolicyType(), is(CUSTODIAL));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPeriod(), is("10Y3M25D"));
    }

    @Test
    public void shouldReturnDefault7YearSentenceWhenCustodialSentenceAwardedLessThan7Years() {
        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withOrderedDate(LocalDate.now())
                        .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                        .withJudicialResultPrompts(asList(judicialResultPrompt()
                                        .withJudicialResultPromptTypeId(TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID)
                                        .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                                        .withValue("260 Weeks")
                                        .build(),
                                judicialResultPrompt()
                                        .withPromptReference("imprisonmentPeriod")
                                        .withValue("1 Years")
                                        .build()))
                        .build()).build());

        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, emptyList());

        assertThat(custodialSentenceRetentionRule.apply(), is(true));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPolicyType(), is(CUSTODIAL));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPeriod(), is("7Y0M0D"));
    }

    @Test
    public void shouldReturnDefault7YearSentenceWhenCustodialSentenceAwardedLessThan7Years_Multi() {
        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withOrderedDate(LocalDate.of(2024, 06, 01))
                        .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                        .withJudicialResultPrompts(asList(judicialResultPrompt()
                                        .withJudicialResultPromptTypeId(TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID)
                                        .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                                        .withValue("260 Weeks 12 Days")
                                        .build(),
                                judicialResultPrompt()
                                        .withPromptReference("imprisonmentPeriod")
                                        .withValue("1 Years")
                                        .build()))
                        .build()).build());

        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, emptyList());

        assertThat(custodialSentenceRetentionRule.apply(), is(true));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPolicyType(), is(CUSTODIAL));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPeriod(), is("7Y0M0D"));
    }

    @Test
    public void shouldReturnDefault7YearSentenceWhenCustodialSentenceAwardedLessThan7YearsFromOffences() {

        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withOrderedDate(LocalDate.now())
                        .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                        .build()).build());

        final JudicialResult judicialResultImpWithTimpPrompts = JudicialResult.judicialResult()
                .withOrderedDate(LocalDate.now())
                .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                .withJudicialResultPrompts(asList(judicialResultPrompt()
                                .withJudicialResultPromptTypeId(TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID)
                                .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                                .withValue("260 Weeks")
                                .build(),
                        judicialResultPrompt()
                                .withPromptReference("imprisonmentPeriod")
                                .withValue("1 Years")
                                .build()))
                .build();
        final JudicialResult anotherJudicialResultWithTimpPrompts = JudicialResult.judicialResult()
                .withOrderedDate(LocalDate.now())
                .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                .withJudicialResultPrompts(singletonList(judicialResultPrompt()
                        .withJudicialResultPromptTypeId(TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID)
                        .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                        .withValue("4 Years")
                        .build()))
                .build();
        final List<Offence> offences = asList(Offence.offence().withJudicialResults(singletonList(judicialResultImpWithTimpPrompts)).build(),
                Offence.offence().withJudicialResults(singletonList(anotherJudicialResultWithTimpPrompts)).build());

        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, offences);

        assertThat(custodialSentenceRetentionRule.apply(), is(true));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPolicyType(), is(CUSTODIAL));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPeriod(), is("7Y0M0D"));
    }

    @Test
    public void shouldReturnMaxSentenceWhenCustodialSentenceAmendedHavingMultipleJRs() {

        final List<DefendantJudicialResult> defendantJudicialResults = asList(DefendantJudicialResult.defendantJudicialResult()
                        .withJudicialResult(JudicialResult.judicialResult()
                                .withOrderedDate(LocalDate.of(2024, 06, 01))
                                .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                                .withJudicialResultPrompts(singletonList(judicialResultPrompt()
                                        .withJudicialResultPromptTypeId(TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID)
                                        .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                                        .withValue("85 Weeks")
                                        .build()))
                                .build()).build(),
                DefendantJudicialResult.defendantJudicialResult()
                        .withJudicialResult(JudicialResult.judicialResult()
                                .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                                .withJudicialResultPrompts(singletonList(judicialResultPrompt()
                                        .withJudicialResultPromptTypeId(TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID)
                                        .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                                        .withValue("3000 Days")
                                        .build()))
                                .build()).build());

        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, emptyList());

        assertThat(custodialSentenceRetentionRule.apply(), is(true));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPolicyType(), is(CUSTODIAL));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPolicyType().getPolicyCode(), is("3"));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPeriod(), is("8Y2M17D"));
    }

    @Test
    public void shouldReturnMaxSentenceWhenCustodialSentenceAmendedHavingMultipleJRs_Multi() {

        final List<DefendantJudicialResult> defendantJudicialResults = asList(DefendantJudicialResult.defendantJudicialResult()
                        .withJudicialResult(JudicialResult.judicialResult()
                                .withOrderedDate(LocalDate.now().minusDays(20))
                                .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                                .withJudicialResultPrompts(singletonList(judicialResultPrompt()
                                        .withJudicialResultPromptTypeId(TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID)
                                        .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                                        .withValue("85 Weeks 3 Days")
                                        .build()))
                                .build()).build(),
                DefendantJudicialResult.defendantJudicialResult()
                        .withJudicialResult(JudicialResult.judicialResult()
                                .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                                .withJudicialResultPrompts(singletonList(judicialResultPrompt()
                                        .withJudicialResultPromptTypeId(TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID)
                                        .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                                        .withValue("3000 Days")
                                        .build()))
                                .build()).build());

        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, emptyList());

        assertThat(custodialSentenceRetentionRule.apply(), is(true));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPolicyType(), is(CUSTODIAL));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPolicyType().getPolicyCode(), is("3"));
        assertThat(custodialSentenceRetentionRule.getPolicy().getPeriod(), is("8Y2M17D"));
    }

    @Test
    public void shouldReturnFalseWhenDefendantJudicialResultsHaveNoTimpResult() {

        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withJudicialResultTypeId(randomUUID())
                        .build()).build());

        final JudicialResult judicialResultImpWithTimpPrompts = JudicialResult.judicialResult()
                .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                .withJudicialResultPrompts(asList(judicialResultPrompt()
                                .withJudicialResultPromptTypeId(randomUUID())
                                .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                                .withValue("260 Weeks")
                                .build(),
                        judicialResultPrompt()
                                .withPromptReference("imprisonmentPeriod")
                                .withValue("1 Years")
                                .build()))
                .build();

        final List<Offence> offences = singletonList(Offence.offence().withJudicialResults(singletonList(judicialResultImpWithTimpPrompts)).build());

        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, offences);

        assertThat(custodialSentenceRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenDefendantJudicialResultsHaveNoTimpResult_Multi() {

        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withJudicialResultTypeId(randomUUID())
                        .build()).build());

        final JudicialResult judicialResultImpWithTimpPrompts = JudicialResult.judicialResult()
                .withJudicialResultTypeId(TIMP_RESULT_DEFINITION_ID)
                .withJudicialResultPrompts(asList(judicialResultPrompt()
                                .withJudicialResultPromptTypeId(randomUUID())
                                .withPromptReference(TOTAL_CUSTODIAL_PERIOD_PROMPT)
                                .withValue("260 Weeks 8 Days")
                                .build(),
                        judicialResultPrompt()
                                .withPromptReference("imprisonmentPeriod")
                                .withValue("1 Years")
                                .build()))
                .build();

        final List<Offence> offences = singletonList(Offence.offence().withJudicialResults(singletonList(judicialResultImpWithTimpPrompts)).build());

        custodialSentenceRetentionRule = new CustodialSentenceRetentionRule(hearingInfo, defendantJudicialResults, offences);

        assertThat(custodialSentenceRetentionRule.apply(), is(false));
    }
}