package uk.gov.moj.cpp.progression.util;


import static com.google.common.collect.ImmutableList.of;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupReportingRestrictions;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ReportingRestriction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class ReportingRestrictionHelperTest {

    @Test
    public void testDedupReportingRestrictionsWithDifferentLabelAndDate() {
        final List<ReportingRestriction> input = of(newRR("A", LocalDate.now()), newRR("B", LocalDate.now()));
        final List<ReportingRestriction> actual = dedupReportingRestrictions(input);
        assertThat(actual, is(input));
    }

    @Test
    public void testDedupReportingRestrictionsWithSameLabelAndDate() {
        final List<ReportingRestriction> input = of(newRR("A", LocalDate.now()), newRR("A", LocalDate.now().minusDays(7)), newRR("A", LocalDate.now()));
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

    @Test
    public void testDedupReportingRestrictionsForDefendantListingStatusChanged() {
        final ImmutableList<ReportingRestriction> rrs = of(newRR("A", LocalDate.now()), newRR("A", LocalDate.now().minusDays(10)), newRR("A", LocalDate.now().minusDays(7)));

        final ProsecutionCaseDefendantListingStatusChanged ev = ProsecutionCaseDefendantListingStatusChanged.
                prosecutionCaseDefendantListingStatusChanged().
                withHearing(Hearing.hearing().withProsecutionCases(asList(ProsecutionCase.
                        prosecutionCase().
                        withDefendants(asList(Defendant.
                                defendant().
                                withOffences(asList(Offence.
                                        offence().
                                        withReportingRestrictions(rrs).
                                        build())).
                                build())).
                        build())).
                        build()).
                build();

        final ProsecutionCaseDefendantListingStatusChanged actual = dedupAllReportingRestrictions(ev);

        final List<ReportingRestriction> actualRR = actual.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictions();
        assertThat(actualRR, hasSize(1));
        assertThat(actualRR.get(0), is(rrs.get(1)));
    }

    private ReportingRestriction newRR(String label, LocalDate date) {
        return new ReportingRestriction(UUID.randomUUID(), null, label, date);
    }

    private ReportingRestriction newRR(UUID resultId, String label, LocalDate date) {
        return new ReportingRestriction(UUID.randomUUID(), resultId, label, date);
    }
}