package uk.gov.moj.cpp.progression.domain.aggregate.utils;


import static com.google.common.collect.ImmutableList.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.ReportingRestrictionHelper.dedupReportingRestrictions;

import uk.gov.justice.core.courts.ReportingRestriction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class ReportingRestrictionHelperTest {

    @Test
    public void testDedupReportingRestrictionsWithDifferentLabelAndDate() {
        final List<ReportingRestriction> input = of(newRR("A", LocalDate.now()), newRR("B", LocalDate.now()));
        final List<ReportingRestriction> actual = dedupReportingRestrictions(input);
        assertThat(actual, is(input));
    }

    @Test
    public void testDedupReportingRestrictionsWithSameLabelAndDate() {
        final List<ReportingRestriction> input = of(newRR("A", LocalDate.now()), newRR("A", LocalDate.now()));
        final List<ReportingRestriction> actual = dedupReportingRestrictions(input);
        assertThat(actual, is(of(input.get(1))));
    }

    @Test
    public void testDedupReportingRestrictionsWithSameLabelAndDifferentDateRetainsOldest() {
        final List<ReportingRestriction> input = of(newRR("A", LocalDate.now()), newRR("A", LocalDate.now().minusDays(7)));
        final List<ReportingRestriction> actual = dedupReportingRestrictions(input);
        assertThat(actual, is(of(input.get(1))));
    }

    @Test
    public void testDedupReportingRestrictionsWithSameLabelAndDifferentResultId() {
        final List<ReportingRestriction> input = of(newRR(UUID.randomUUID(), "A", LocalDate.now()), newRR(UUID.randomUUID(), "A", LocalDate.now()));
        final List<ReportingRestriction> actual = dedupReportingRestrictions(input);
        assertThat(actual, is(input));
    }

    private ReportingRestriction newRR(String label, LocalDate date) {
        return new ReportingRestriction(UUID.randomUUID(), null, label, date);
    }

    private ReportingRestriction newRR(UUID resultId, String label, LocalDate date) {
        return new ReportingRestriction(UUID.randomUUID(), resultId, label, date);
    }
}