package uk.gov.moj.cpp.progression.transformer;

import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtApplication;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtApplicationWithCourtApplicationCases;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtApplicationWithCourtOrder;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildNextHearing;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.moj.cpp.progression.helper.HearingBookingReferenceListExtractor;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.helper.TestHelper;
import uk.gov.moj.cpp.progression.service.ProvisionalBookingServiceAdapter;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingListingNeedsTransformerTest {

    private static final UUID COURT_APPLICATION_ID_1 = randomUUID();
    private static final UUID COURT_APPLICATION_ID_2 = randomUUID();

    private static final UUID HEARING_TYPE_1 = randomUUID();
    private static final UUID HEARING_TYPE_2 = randomUUID();
    private static final UUID BOOKING_REFERENCE_1 = randomUUID();
    private static final UUID BOOKING_REFERENCE_2 = randomUUID();

    private static final UUID COURT_SCHEDULE_ID_1 = randomUUID();
    private static final UUID COURT_SCHEDULE_ID_2 = randomUUID();

    private static final String COURT_LOCATION = "CourtLocation";
    private static final LocalDate WEEK_COMMENCING_DATE_1 = LocalDate.now();
    private static final LocalDate WEEK_COMMENCING_DATE_2 = LocalDate.now().plusDays(1);
    private static final ZonedDateTime LISTED_START_DATETIME_1 = ZonedDateTime.now();
    private static final ZonedDateTime LISTED_START_DATETIME_2 = ZonedDateTime.now().plusHours(5);

    @InjectMocks
    private HearingListingNeedsTransformer transformer;

    @Spy
    private HearingBookingReferenceListExtractor hearingBookingReferenceListExtractor;

    @Spy
    private HearingResultHelper hearingResultHelper;

    @Mock
    private ProvisionalBookingServiceAdapter provisionalBookingServiceAdapter;

    @Test
    public void shouldReturnOneHearingNeedsWhenTwoHearingMatch() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());

        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplication(COURT_APPLICATION_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildCourtApplication(COURT_APPLICATION_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getCourtApplications().size(), is(2));
        assertThat(hearingListingNeedsList.get(0).getCourtApplications().get(0).getId(), is(COURT_APPLICATION_ID_1));
        assertThat(hearingListingNeedsList.get(0).getCourtApplications().get(1).getId(), is(COURT_APPLICATION_ID_2));
    }

    @Test
    public void shouldReturnTwoHearingNeedsWhenTwoHearingTypeNotMatch() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());

        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplication(COURT_APPLICATION_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildCourtApplication(COURT_APPLICATION_ID_2, buildNextHearing(HEARING_TYPE_2, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }

    @Test
    public void shouldReturnTwoHearingNeedsWhenTwoHearingWeekCommenceDateNotMatch() {
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplication(COURT_APPLICATION_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildCourtApplication(COURT_APPLICATION_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_2, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }

    @Test
    public void shouldReturnTwoHearingNeedsWhenTwoHearingListedStartDateNotMatch() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplication(COURT_APPLICATION_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, null, LISTED_START_DATETIME_1)),
                buildCourtApplication(COURT_APPLICATION_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, null, LISTED_START_DATETIME_2))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }

    @Test
    public void shouldReturnTwoHearingNeedsWhenTwoHearingDatesNotMatch() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplication(COURT_APPLICATION_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildCourtApplication(COURT_APPLICATION_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, null, LISTED_START_DATETIME_2))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }

    @Test
    public void shouldReturnTwoHearingNeedsWhenTwoHearingDatesNotMatchWithCourtApplicationCase() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplicationWithCourtApplicationCases(COURT_APPLICATION_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildCourtApplicationWithCourtApplicationCases(COURT_APPLICATION_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, null, LISTED_START_DATETIME_2))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }

    @Test
    public void shouldReturnTwoHearingNeedsWhenTwoHearingDatesNotMatchWithWithCourtOrder() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplicationWithCourtOrder(COURT_APPLICATION_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildCourtApplicationWithCourtOrder(COURT_APPLICATION_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, null, LISTED_START_DATETIME_2))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }

    @Test
    public void shouldReturnOneHearingListingNeedsWhenSlotsAreSame() {
        Map<UUID, Set<UUID>> slotsMap = new HashMap<>();
        slotsMap.put(BOOKING_REFERENCE_1, new HashSet<>(asList(COURT_SCHEDULE_ID_1)));
        slotsMap.put(BOOKING_REFERENCE_2, new HashSet<>(asList(COURT_SCHEDULE_ID_1)));

        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(slotsMap);
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplication(COURT_APPLICATION_ID_1, buildNextHearing(null, BOOKING_REFERENCE_1, null, null, null)),
                buildCourtApplication(COURT_APPLICATION_ID_2, buildNextHearing(null, BOOKING_REFERENCE_2, null, null, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(1));
    }

    @Test
    public void shouldReturnTwoHearingListingNeedsWhenSlotsAreDifferent() {
        Map<UUID, Set<UUID>> slotsMap = new HashMap<>();
        slotsMap.put(BOOKING_REFERENCE_1, new HashSet<>(asList(COURT_SCHEDULE_ID_1)));
        slotsMap.put(BOOKING_REFERENCE_2, new HashSet<>(asList(COURT_SCHEDULE_ID_2)));

        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(slotsMap);
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplication(COURT_APPLICATION_ID_1, buildNextHearing(null, BOOKING_REFERENCE_1, null, null, null)),
                buildCourtApplication(COURT_APPLICATION_ID_2, buildNextHearing(null, BOOKING_REFERENCE_2, null, null, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }

    @Test
    public void shouldReturnTwoHearingListingNeedsWhenSlotsAreDifferentWithCourtApplicationCase() {
        final Map<UUID, Set<UUID>> slotsMap = new HashMap<>();
        slotsMap.put(BOOKING_REFERENCE_1, new HashSet<>(asList(COURT_SCHEDULE_ID_1)));
        slotsMap.put(BOOKING_REFERENCE_2, new HashSet<>(asList(COURT_SCHEDULE_ID_2)));

        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(slotsMap);
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplicationWithCourtApplicationCases(COURT_APPLICATION_ID_1, buildNextHearing(null, BOOKING_REFERENCE_1, null, null, null)),
                buildCourtApplicationWithCourtApplicationCases(COURT_APPLICATION_ID_2, buildNextHearing(null, BOOKING_REFERENCE_2, null, null, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }

    @Test
    public void shouldReturnTwoHearingListingNeedsWhenSlotsAreDifferentWithCourtOrder() {
        final Map<UUID, Set<UUID>> slotsMap = new HashMap<>();
        slotsMap.put(BOOKING_REFERENCE_1, new HashSet<>(asList(COURT_SCHEDULE_ID_1)));
        slotsMap.put(BOOKING_REFERENCE_2, new HashSet<>(asList(COURT_SCHEDULE_ID_2)));

        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(slotsMap);
        final Hearing hearing = TestHelper.buildHearingWithCourtApplications(Arrays.asList(
                buildCourtApplicationWithCourtOrder(COURT_APPLICATION_ID_1, buildNextHearing(null, BOOKING_REFERENCE_1, null, null, null)),
                buildCourtApplicationWithCourtOrder(COURT_APPLICATION_ID_2, buildNextHearing(null, BOOKING_REFERENCE_2, null, null, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }
}