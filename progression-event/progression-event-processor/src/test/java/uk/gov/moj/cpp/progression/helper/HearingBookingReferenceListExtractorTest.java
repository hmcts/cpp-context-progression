package uk.gov.moj.cpp.progression.helper;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtApplication;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtApplicationWithCourtApplicationCases;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtApplicationWithCourtOrder;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildNextHearing;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildProsecutionCase;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildProsecutionCaseWithoutJudicialResult;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingBookingReferenceListExtractorTest {

    private static final UUID COURT_APPLICATION_ID_1 = randomUUID();
    private static final UUID COURT_APPLICATION_ID_2 = randomUUID();
    private static final UUID COURT_APPLICATION_ID_3 = randomUUID();

    private static final UUID CASE_ID_1 = randomUUID();
    private static final UUID DEFENDANT_ID_1 = randomUUID();
    private static final UUID OFFENCE_ID_1 = randomUUID();

    private static final UUID CASE_ID_2 = randomUUID();
    private static final UUID DEFENDANT_ID_2 = randomUUID();
    private static final UUID OFFENCE_ID_2 = randomUUID();

    private static final UUID CASE_ID_3 = randomUUID();
    private static final UUID DEFENDANT_ID_3 = randomUUID();
    private static final UUID OFFENCE_ID_3 = randomUUID();

    private static final UUID HEARING_TYPE = randomUUID();
    private static final UUID BOOKING_REFERENCE_1 = randomUUID();
    private static final UUID BOOKING_REFERENCE_2 = randomUUID();

    private static final String COURT_LOCATION = "CourtLocation";
    private static final ZonedDateTime LISTED_START_DATETIME = ZonedDateTime.now();

    @InjectMocks
    private HearingBookingReferenceListExtractor extractor;

    @Spy
    private HearingResultHelper hearingResultHelper;

    @Test
    public void extractBookingReferenceList(){
        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCaseWithoutJudicialResult(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE, BOOKING_REFERENCE_1, COURT_LOCATION,null, LISTED_START_DATETIME)),
                buildProsecutionCase(CASE_ID_3, DEFENDANT_ID_3, OFFENCE_ID_3, buildNextHearing(HEARING_TYPE, null, COURT_LOCATION, null, LISTED_START_DATETIME))
        ));

        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(1));
        assertThat(result.get(0), equalTo(BOOKING_REFERENCE_1));
    }

    @Test
    public void extractBookingReferenceListMustBeDistinct(){

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCaseWithoutJudicialResult(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE, BOOKING_REFERENCE_1, COURT_LOCATION,null, LISTED_START_DATETIME)),
                buildProsecutionCase(CASE_ID_3, DEFENDANT_ID_3, OFFENCE_ID_3, buildNextHearing(HEARING_TYPE, BOOKING_REFERENCE_1, COURT_LOCATION,null, LISTED_START_DATETIME))
        ));

        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(1));
        assertThat(result.get(0), equalTo(BOOKING_REFERENCE_1));
    }

    @Test
    public void extractTestWhenHavingTwoBookingReferences(){

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCaseWithoutJudicialResult(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE, BOOKING_REFERENCE_1, COURT_LOCATION, null, LISTED_START_DATETIME)),
                buildProsecutionCase(CASE_ID_3, DEFENDANT_ID_3, OFFENCE_ID_3, buildNextHearing(HEARING_TYPE, BOOKING_REFERENCE_2, COURT_LOCATION,null, LISTED_START_DATETIME))
        ));

        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(2));
    }

    @Test
    public void extractTestWhenHavingNullNextHearing(){

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCaseWithoutJudicialResult(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE, BOOKING_REFERENCE_1, COURT_LOCATION,null, LISTED_START_DATETIME)),
                buildProsecutionCase(CASE_ID_3, DEFENDANT_ID_3, OFFENCE_ID_3, null)
        ));

        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(1));
    }

    @Test
    public void shouldReturnOneBookingReferenceWhenOneBookingReferenceExistsInResults(){
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(singletonList(
                buildCourtApplication(COURT_APPLICATION_ID_1, buildNextHearing(null, BOOKING_REFERENCE_1, null, null, null))
        ));
        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(1));
        assertThat(result.get(0), equalTo(BOOKING_REFERENCE_1));
    }

    @Test
    public void shouldReturnTwoBookingReferencesWhenTwoDistinctBookingReferencesExistInResults(){
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplication(COURT_APPLICATION_ID_1, buildNextHearing(null, BOOKING_REFERENCE_1, null, null, null)),
                buildCourtApplication(COURT_APPLICATION_ID_2, buildNextHearing(null, BOOKING_REFERENCE_2, null, null, null))
        ));
        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(2));
        assertThat(result, hasItems(BOOKING_REFERENCE_1, BOOKING_REFERENCE_2));
    }

    @Test
    public void shouldReturnTwoBookingReferencesWhenTwoDistinctBookingReferencesExistInResultsWithCourtOrder(){
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplicationWithCourtOrder(COURT_APPLICATION_ID_1, buildNextHearing(null, BOOKING_REFERENCE_1, null, null, null)),
                buildCourtApplicationWithCourtOrder(COURT_APPLICATION_ID_2, buildNextHearing(null, BOOKING_REFERENCE_2, null, null, null))
        ));
        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(2));
        assertThat(result, hasItems(BOOKING_REFERENCE_1, BOOKING_REFERENCE_2));
    }

    @Test
    public void shouldReturnTwoBookingReferencesWhenTwoDistinctBookingReferencesExistInResultsWithCourtApplicationCase(){
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplicationWithCourtApplicationCases(COURT_APPLICATION_ID_1, buildNextHearing(null, BOOKING_REFERENCE_1, null, null, null)),
                buildCourtApplicationWithCourtApplicationCases(COURT_APPLICATION_ID_2, buildNextHearing(null, BOOKING_REFERENCE_2, null, null, null))
        ));
        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(2));
        assertThat(result, hasItems(BOOKING_REFERENCE_1, BOOKING_REFERENCE_2));
    }

    @Test
    public void shouldReturnDistinctBookingReferencesWhenDuplicateBookingReferencesExistInResults(){
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplication(COURT_APPLICATION_ID_1, buildNextHearing(null, BOOKING_REFERENCE_1, null, null, null)),
                buildCourtApplication(COURT_APPLICATION_ID_2, buildNextHearing(null, BOOKING_REFERENCE_2, null, null, null)),
                buildCourtApplication(COURT_APPLICATION_ID_3, buildNextHearing(null, BOOKING_REFERENCE_2, null, null, null))
        ));
        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(2));
        assertThat(result, hasItems(BOOKING_REFERENCE_1, BOOKING_REFERENCE_2));
    }

    @Test
    public void shouldReturnEmptyListWhenNoBookingReferencesExistInResults(){
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplication(COURT_APPLICATION_ID_1, buildNextHearing(null, null, null, null, null)),
                buildCourtApplication(COURT_APPLICATION_ID_2, buildNextHearing(null, null, null, null, null)),
                buildCourtApplication(COURT_APPLICATION_ID_3, buildNextHearing(null, null, null, null, null))
        ));
        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(0));
    }

    @Test
    public void shouldReturnEmptyListWhenNoNextHearingsExistInResults(){
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplication(COURT_APPLICATION_ID_1, null),
                buildCourtApplication(COURT_APPLICATION_ID_2, null),
                buildCourtApplication(COURT_APPLICATION_ID_3, null)
        ));
        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(0));
    }

    @Test
    public void shouldReturnEmptyListWhenNoJudicialResultsExistInResults() {
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(singletonList(
                CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .build()
        ));
        List<UUID> result = extractor.extractBookingReferenceList(hearing);
        assertThat(result.size(), is(0));
    }
}