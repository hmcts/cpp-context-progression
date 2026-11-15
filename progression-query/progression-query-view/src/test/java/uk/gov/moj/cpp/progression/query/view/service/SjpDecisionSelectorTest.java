package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SjpDecisionSelectorTest {

    private static final ZonedDateTime DAY_1 = ZonedDateTime.parse("2026-01-01T10:00:00Z");

    @Test
    void shouldKeepAllDecisionsOldestFirstWhenThereIsNoApplicationDecision() {
        final SjpCaseDecision later = decision(2, false);
        final SjpCaseDecision earlier = decision(1, false);

        assertThat(SjpDecisionSelector.selectDecisionIds(asList(later, earlier)), contains(earlier.id(), later.id()));
    }

    @Test
    void shouldDropDecisionsUpToAndIncludingTheLatestApplicationDecision() {
        final SjpCaseDecision original = decision(1, false);
        final SjpCaseDecision setAside = decision(2, true);
        final SjpCaseDecision reDecision = decision(3, false);

        assertThat(SjpDecisionSelector.selectDecisionIds(asList(original, setAside, reDecision)), contains(reDecision.id()));
    }

    @Test
    void shouldCutAtTheLatestOfSeveralApplicationDecisions() {
        final SjpCaseDecision first = decision(1, false);
        final SjpCaseDecision firstSetAside = decision(2, true);
        final SjpCaseDecision second = decision(3, false);
        final SjpCaseDecision secondSetAside = decision(4, true);
        final SjpCaseDecision third = decision(5, false);
        final SjpCaseDecision fourth = decision(6, false);

        assertThat(SjpDecisionSelector.selectDecisionIds(asList(fourth, second, secondSetAside, first, third, firstSetAside)),
                contains(third.id(), fourth.id()));
    }

    @Test
    void shouldDropDecisionSavedAtTheSameTimeAsTheApplicationDecision() {
        final SjpCaseDecision setAside = decision(1, true);
        final SjpCaseDecision sameTime = decision(1, false);

        assertThat(SjpDecisionSelector.selectDecisionIds(asList(setAside, sameTime)), empty());
    }

    @Test
    void shouldReturnNothingWhenTheOnlyDecisionIsAnApplicationDecision() {
        assertThat(SjpDecisionSelector.selectDecisionIds(asList(decision(1, false), decision(2, true))), empty());
    }

    @Test
    void shouldReturnNothingForNoDecisions() {
        assertThat(SjpDecisionSelector.selectDecisionIds(emptyList()), empty());
    }

    private static SjpCaseDecision decision(final int day, final boolean applicationDecision) {
        return new SjpCaseDecision(UUID.randomUUID(), DAY_1.plusDays(day - 1L), applicationDecision);
    }
}
