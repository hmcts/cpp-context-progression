package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.REMITTAL;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class RemittalRetentionRuleTest {

    private RemittalRetentionRule remittalRetentionRule;
    private final List<String> remitResultIds = asList("2aeb9f97-18a9-4380-80e7-2452c64a6e18", "7e6079c6-b5cb-4994-aa8e-6f3cd0e954f8");
    private HearingInfo hearingInfo = new HearingInfo(randomUUID(), "hearingType", JurisdictionType.CROWN.name(),
            randomUUID(), "courtCentreName", randomUUID(), "courtRoomName");

    @Test
    public void shouldReturnFalseWhenNullOffences() {
        remittalRetentionRule = new RemittalRetentionRule(hearingInfo, null, null);
        assertThat(remittalRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenEmptyOffences() {
        remittalRetentionRule = new RemittalRetentionRule(hearingInfo, emptyList(), emptyList());
        assertThat(remittalRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenOffencesWithNoJudicialResultTypeId() {

        final List<Offence> offences = asList(Offence.offence()
                        .withJudicialResults(getJudicialResults(JudicialResultCategory.ANCILLARY, null)).build(),
                Offence.offence()
                        .withJudicialResults(getJudicialResults(JudicialResultCategory.FINAL, null)).build());
        final List<String> remitResultIds = asList("2aeb9f97-18a9-4380-80e7-2452c64a6e18", "7e6079c6-b5cb-4994-aa8e-6f3cd0e954f8");
        remittalRetentionRule = new RemittalRetentionRule(hearingInfo, offences, remitResultIds);

        assertThat(remittalRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenOffencesWhenNoRemittalJudicialResults() {

        final List<Offence> offences = asList(Offence.offence()
                        .withJudicialResults(getJudicialResults(JudicialResultCategory.ANCILLARY, randomUUID())).build(),
                Offence.offence()
                        .withJudicialResults(getJudicialResults(JudicialResultCategory.INTERMEDIARY, randomUUID())).build());
        remittalRetentionRule = new RemittalRetentionRule(hearingInfo, offences, remitResultIds);

        assertThat(remittalRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenOffencesWithNoJudicialResults() {

        final List<Offence> offences = asList(Offence.offence().build(), Offence.offence().build());
        remittalRetentionRule = new RemittalRetentionRule(hearingInfo, offences, remitResultIds);

        assertThat(remittalRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnTrueWhenOffencesJudicialResultsCategoryIntermediaryWithNextHearing() {

        final List<Offence> offences = singletonList(Offence.offence()
                .withJudicialResults(getJudicialResults(JudicialResultCategory.ANCILLARY, fromString("7e6079c6-b5cb-4994-aa8e-6f3cd0e954f8"))).build());
        remittalRetentionRule = new RemittalRetentionRule(hearingInfo, offences, remitResultIds);

        assertThat(remittalRetentionRule.apply(), is(true));
        assertThat(remittalRetentionRule.getPolicy().getPolicyType(), is(REMITTAL));
        assertThat(remittalRetentionRule.getPolicy().getPolicyType().getPolicyCode(), is("2"));
        assertThat(remittalRetentionRule.getPolicy().getPeriod(), is("7Y0M0D"));
    }

    @Test
    public void shouldReturnFalseWhenAnyOffenceDoesNotHaveJudicialResults() {

        final List<Offence> offences = asList(Offence.offence()
                        .withJudicialResults(getJudicialResults(JudicialResultCategory.ANCILLARY, randomUUID())).build(),
                Offence.offence().withId(randomUUID()).build());
        remittalRetentionRule = new RemittalRetentionRule(hearingInfo, offences, remitResultIds);

        assertThat(remittalRetentionRule.apply(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenAllOffencesDoesNotHaveJudicialResults() {

        final List<Offence> offences = asList(Offence.offence()
                        .withId(randomUUID()).build(),
                Offence.offence().withId(randomUUID()).build());
        remittalRetentionRule = new RemittalRetentionRule(hearingInfo, offences, remitResultIds);

        assertThat(remittalRetentionRule.apply(), is(false));
    }


    private List<JudicialResult> getJudicialResults(final JudicialResultCategory judicialResultCategory, final UUID judicialResultTypeId) {
        return singletonList(JudicialResult.judicialResult()
                .withJudicialResultId(randomUUID())
                .withJudicialResultTypeId(judicialResultTypeId)
                .withCategory(judicialResultCategory)
                .build());
    }


}