package uk.gov.moj.cpp.progression.helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.Hearing;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildNextHearing;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildProsecutionCase;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildProsecutionCaseWithoutJudicialResult;

@RunWith(MockitoJUnitRunner.class)
public class HearingBookingReferenceListExtractorTest {
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
    HearingBookingReferenceListExtractor extractor;

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

}