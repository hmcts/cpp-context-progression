package uk.gov.moj.cpp.progression.util;


import static com.google.common.collect.ImmutableList.of;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllApplications;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictionsForCourtApplications;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupReportingRestriction;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupReportingRestrictions;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationProceedingsEdited;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV3;
import uk.gov.justice.core.courts.ReportingRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
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

    @Test
    public void testDedupReportingRestrictionsForDefendantListingStatusChangedV2() {
        final ImmutableList<ReportingRestriction> rrs = of(newRR("A", LocalDate.now()), newRR("A", LocalDate.now().minusDays(10)), newRR("A", LocalDate.now().minusDays(7)));

        final List<ListHearingRequest> listHearingRequests = new ArrayList<>();
        listHearingRequests.add(ListHearingRequest.listHearingRequest()
                .withHearingType(HearingType.hearingType()
                        .withId(UUID.randomUUID())
                        .build())
                .build());

        final ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = ProsecutionCaseDefendantListingStatusChangedV2.
                prosecutionCaseDefendantListingStatusChangedV2().
                withListHearingRequests(listHearingRequests).
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

        final ProsecutionCaseDefendantListingStatusChangedV2 actual = dedupAllReportingRestrictions(prosecutionCaseDefendantListingStatusChangedV2);

        final List<ReportingRestriction> actualRR = actual.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictions();
        assertThat(actualRR, hasSize(1));
        assertThat(actualRR.get(0), is(rrs.get(1)));
    }

    @Test
    public void testDedupAllReportingRestrictionsForCourtApplications(){
        final List<CourtApplication> courtApplications = new ArrayList<>();
        courtApplications.add(CourtApplication.courtApplication().build());
        List<CourtApplication> applications = dedupAllReportingRestrictionsForCourtApplications(courtApplications);
        assertThat(applications, hasSize(1));
    }

    @Test
    public void testDedupReportingRestriction(){
        final CourtApplicationProceedingsEdited initiateCourtApplicationEdited = CourtApplicationProceedingsEdited.courtApplicationProceedingsEdited().build();
        CourtApplicationProceedingsEdited courtApplicationProceedingsEdited = dedupReportingRestriction(initiateCourtApplicationEdited);
        assertThat(courtApplicationProceedingsEdited, notNullValue());
    }

    @Test
    public void testDedupReportingRestrictionOnInitiation(){
        final List<CourtApplicationCase> courtApplicationCases = new ArrayList<>();
        courtApplicationCases.add(CourtApplicationCase.courtApplicationCase()
                .withProsecutionCaseId(UUID.randomUUID())
                .build());
        final CourtApplicationProceedingsInitiated initiateCourtApplicationProceedings = CourtApplicationProceedingsInitiated.courtApplicationProceedingsInitiated()
                .withCourtApplication(CourtApplication.courtApplication()
                        .withCourtApplicationCases(courtApplicationCases)
                        .build())
                .build();
        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = dedupReportingRestriction(initiateCourtApplicationProceedings);
        assertThat(courtApplicationProceedingsInitiated, notNullValue());
    }

    @Test
    public void testDedupReportingRestriction_courtApplication(){
        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence().build());
        final List<CourtOrderOffence> courtOrderOffences = new ArrayList<>();
        courtOrderOffences.add(CourtOrderOffence.courtOrderOffence()
                .withOffence(Offence.offence()
                        .withReportingRestrictions(of(newRR("Test", LocalDate.now())))
                        .build())
                .build());

        final List<CourtApplicationCase> courtApplicationCases = new ArrayList<>();
        courtApplicationCases.add(CourtApplicationCase.courtApplicationCase()
                .withOffences(offences)
                .build());

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(courtOrderOffences)
                        .build())
                .withCourtApplicationCases(courtApplicationCases)
                .build();

        final CourtApplication courtApplication1 = dedupAllReportingRestrictions(courtApplication);
        assertThat(courtApplication1, notNullValue());
    }

    @Test
    public void testDedupAllApplications() {
        final ImmutableList<ReportingRestriction> rrs = of(newRR("A", LocalDate.now()), newRR("A", LocalDate.now().minusDays(10)), newRR("A", LocalDate.now().minusDays(7)));

        final List<ListHearingRequest> listHearingRequests = new ArrayList<>();
        listHearingRequests.add(ListHearingRequest.listHearingRequest()
                .withHearingType(HearingType.hearingType()
                        .withId(UUID.randomUUID())
                        .build())
                .build());


        final List<JudicialResult> judicialResults = new ArrayList<>();
        JudicialResult jr1 = JudicialResult.judicialResult()
                .withJudicialResultId(UUID.randomUUID())
                .withOrderedDate(LocalDate.now())
                .withResultText("ResultText")
                .build();
        judicialResults.add(jr1);
        judicialResults.add(jr1);
        judicialResults.add(jr1);
        judicialResults.add(JudicialResult.judicialResult()
                .withJudicialResultId(UUID.randomUUID())
                .withResultText("ResultText")
                .build());

        final List<JudicialResult> judicialResults1 = new ArrayList<>();
        JudicialResult jr2 = JudicialResult.judicialResult()
                .withJudicialResultId(UUID.randomUUID())
                .withOrderedDate(LocalDate.now())
                .withResultText("ResultText1")
                .build();
        judicialResults.add(jr2);

        final List<CourtOrderOffence> courtOrderOffences = new ArrayList<>();
        courtOrderOffences.add(CourtOrderOffence.courtOrderOffence().build());

        final List<CourtApplication> courtApplications = new ArrayList<>();
        final CourtApplication c1 = CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withJudicialResults(judicialResults)
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(courtOrderOffences)
                        .build())
                .build();
        courtApplications.add(c1);
        courtApplications.add(c1);
        courtApplications.add(c1);
        courtApplications.add(c1);
        courtApplications.add(CourtApplication.courtApplication()
                        .withId(UUID.randomUUID())
                        .withJudicialResults(judicialResults1)
                        .withCourtOrder(CourtOrder.courtOrder()
                                .withCourtOrderOffences(courtOrderOffences)
                                .build())
                .build());

        final ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = ProsecutionCaseDefendantListingStatusChangedV2.
                prosecutionCaseDefendantListingStatusChangedV2().
                withListHearingRequests(listHearingRequests).
                withHearing(Hearing.hearing()
                        .withCourtApplications(courtApplications)
                        .withProsecutionCases(asList(ProsecutionCase.
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

        final ProsecutionCaseDefendantListingStatusChangedV2 actual = dedupAllApplications(prosecutionCaseDefendantListingStatusChangedV2);

        final List<CourtApplication> updatedCourtApplications = actual.getHearing().getCourtApplications();
        assertThat(updatedCourtApplications, hasSize(2));
    }

    @Test
    public void testDedupAllApplicationsV3() {
        final ImmutableList<ReportingRestriction> rrs = of(newRR("A", LocalDate.now()), newRR("A", LocalDate.now().minusDays(10)), newRR("A", LocalDate.now().minusDays(7)));

        final List<JudicialResult> judicialResults = new ArrayList<>();
        JudicialResult jr1 = JudicialResult.judicialResult()
                .withJudicialResultId(UUID.randomUUID())
                .withOrderedDate(LocalDate.now())
                .withResultText("ResultText")
                .build();
        judicialResults.add(jr1);
        judicialResults.add(jr1);
        judicialResults.add(jr1);
        judicialResults.add(JudicialResult.judicialResult()
                .withJudicialResultId(UUID.randomUUID())
                .withResultText("ResultText")
                .build());

        final List<JudicialResult> judicialResults1 = new ArrayList<>();
        JudicialResult jr2 = JudicialResult.judicialResult()
                .withJudicialResultId(UUID.randomUUID())
                .withOrderedDate(LocalDate.now())
                .withResultText("ResultText1")
                .build();
        judicialResults.add(jr2);

        final List<CourtOrderOffence> courtOrderOffences = new ArrayList<>();
        courtOrderOffences.add(CourtOrderOffence.courtOrderOffence().build());

        final List<CourtApplication> courtApplications = new ArrayList<>();
        final CourtApplication c1 = CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withJudicialResults(judicialResults)
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(courtOrderOffences)
                        .build())
                .build();
        courtApplications.add(c1);
        courtApplications.add(c1);
        courtApplications.add(c1);
        courtApplications.add(c1);
        courtApplications.add(CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withJudicialResults(judicialResults1)
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(courtOrderOffences)
                        .build())
                .build());

        final ProsecutionCaseDefendantListingStatusChangedV3 prosecutionCaseDefendantListingStatusChangedV3 = ProsecutionCaseDefendantListingStatusChangedV3.
                prosecutionCaseDefendantListingStatusChangedV3().
                withHearing(Hearing.hearing()
                        .withCourtApplications(courtApplications)
                        .withProsecutionCases(asList(ProsecutionCase.
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

        final ProsecutionCaseDefendantListingStatusChangedV3 actual = dedupAllApplications(prosecutionCaseDefendantListingStatusChangedV3);

        final List<CourtApplication> updatedCourtApplications = actual.getHearing().getCourtApplications();
        assertThat(updatedCourtApplications, hasSize(2));
    }
}