package uk.gov.moj.cpp.progression.transformer;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildNextHearing;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildProsecutionCase;

import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtHouseType;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.progression.helper.HearingBookingReferenceListExtractor;
import uk.gov.moj.cpp.progression.helper.TestHelper;
import uk.gov.moj.cpp.progression.service.ProvisionalBookingServiceAdapter;
import uk.gov.moj.cpp.progression.service.utils.OffenceToCommittingCourtConverter;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingToHearingListingNeedsTransformerTest {
    private static final UUID CASE_ID_1 = randomUUID();
    private static final UUID DEFENDANT_ID_1 = randomUUID();
    private static final UUID OFFENCE_ID_1 = randomUUID();

    private static final UUID CASE_ID_2 = randomUUID();
    private static final UUID DEFENDANT_ID_2 = randomUUID();
    private static final UUID OFFENCE_ID_2 = randomUUID();

    private static final UUID CASE_ID_3 = randomUUID();
    private static final UUID DEFENDANT_ID_3 = randomUUID();
    private static final UUID OFFENCE_ID_3 = randomUUID();

    private static final UUID HEARING_TYPE_1 = randomUUID();
    private static final UUID HEARING_TYPE_2 = randomUUID();
    private static final UUID BOOKING_REFERENCE_1 = randomUUID();
    private static final UUID BOOKING_REFERENCE_2 = randomUUID();

    private static final UUID COURT_SCHEDULE_ID_1 = randomUUID();
    private static final UUID COURT_SCHEDULE_ID_2 = randomUUID();

    private static final String COURT_LOCATION = "CourtLocation";
    private static final LocalDate WEEK_COMMENCING_DATE_1 = LocalDate.now();
    private static final LocalDate WEEK_COMMENCING_DATE_2 = LocalDate.now();
    private static final ZonedDateTime LISTED_START_DATETIME_1 = ZonedDateTime.now();
    private static final ZonedDateTime LISTED_START_DATETIME_2 = ZonedDateTime.now();

    @InjectMocks
    private HearingToHearingListingNeedsTransformer transformer;

    @Spy
    private HearingBookingReferenceListExtractor hearingBookingReferenceListExtractor;

    @Mock
    private ProvisionalBookingServiceAdapter provisionalBookingServiceAdapter;

    @Mock
    private OffenceToCommittingCourtConverter offenceToCommittingCourtConverter;

    @Test
    public void shouldReturnOneHearingNeedsWhenTwoHearingMatch() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
                ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().size(), is(2));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(1).getDefendants().size(), is(1));

        final Defendant defendant1 = hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0);
        final Defendant defendant2 = hearingListingNeedsList.get(0).getProsecutionCases().get(1).getDefendants().get(0);
        assertThat(defendant1.getOffences().size(), is(1));
        assertThat(defendant2.getOffences().size(), is(1));

        if (defendant1.getId().equals(DEFENDANT_ID_1)){
            assertThat(defendant2.getId(), equalTo(DEFENDANT_ID_2));
            assertThat(defendant1.getOffences().get(0).getId(), equalTo(OFFENCE_ID_1));
            assertThat(defendant2.getOffences().get(0).getId(), equalTo(OFFENCE_ID_2));
        } else {
            assertThat(defendant2.getId(), equalTo(DEFENDANT_ID_1));
            assertThat(defendant1.getOffences().get(0).getId(), equalTo(OFFENCE_ID_2));
            assertThat(defendant2.getOffences().get(0).getId(), equalTo(OFFENCE_ID_1));
        }
    }

    @Test
    public void shouldReturnTwoHearingNeedsWhenTwoHearingTypeNotMatch() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_2, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));

        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().size(), is(1));
        assertThat(hearingListingNeedsList.get(1).getProsecutionCases().size(), is(1));

        final ProsecutionCase prosecutionCase1 = hearingListingNeedsList.get(0).getProsecutionCases().get(0);
        final ProsecutionCase prosecutionCase2 = hearingListingNeedsList.get(1).getProsecutionCases().get(0);
        assertThat(prosecutionCase1.getDefendants().size(), is(1));
        assertThat(prosecutionCase2.getDefendants().size(), is(1));

        final Defendant defendant1;
        final Defendant defendant2;
        if (prosecutionCase1.getDefendants().get(0).getId().equals(DEFENDANT_ID_1)){
            defendant1 = prosecutionCase1.getDefendants().get(0);
            defendant2 = prosecutionCase2.getDefendants().get(0);
        } else {
            defendant1 = prosecutionCase2.getDefendants().get(0);
            defendant2 = prosecutionCase1.getDefendants().get(0);
        }

        assertThat(defendant1.getOffences().size(), is(1));
        assertThat(defendant1.getOffences().get(0).getId(), equalTo(OFFENCE_ID_1));

        assertThat(defendant2.getOffences().size(), is(1));
        assertThat(defendant2.getOffences().get(0).getId(), equalTo(OFFENCE_ID_2));
    }

    @Test
    public void shouldReturnTwoHearingNeedsWhenTwoHearingWeekCommenceDateNotMatch() {
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_2, null, COURT_LOCATION, WEEK_COMMENCING_DATE_2, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }

    @Test
    public void shouldReturnTwoHearingNeedsWhenTwoHearingListedStartDateNotMatch() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION,null,  LISTED_START_DATETIME_1)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_2, null, COURT_LOCATION,null, LISTED_START_DATETIME_2))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }

    @Test
    public void shouldReturnTwoHearingNeedsWhenTwoHearingDatesNotMatch() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_2, null, COURT_LOCATION,null, LISTED_START_DATETIME_1))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));
    }


    @Test
    public void shouldIntoSameHearingWhenSlotsAreSame() {
        final Map<UUID, Set<UUID>> slotsMap = new HashMap<>();
        slotsMap.put(BOOKING_REFERENCE_1, new HashSet<>(Arrays.asList(COURT_SCHEDULE_ID_1)));
        slotsMap.put(BOOKING_REFERENCE_2, new HashSet<>(Arrays.asList(COURT_SCHEDULE_ID_1)));

        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(slotsMap);
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_2, BOOKING_REFERENCE_1, COURT_LOCATION,null, LISTED_START_DATETIME_1)),
                buildProsecutionCase(CASE_ID_3, DEFENDANT_ID_3, OFFENCE_ID_3, buildNextHearing(HEARING_TYPE_2, BOOKING_REFERENCE_2, COURT_LOCATION,null, LISTED_START_DATETIME_1))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(2));

        final Optional<HearingListingNeeds> optionalHearingListingNeeds1 = hearingListingNeedsList.stream().filter(x -> x.getProsecutionCases().size() == 1).findFirst();
        final Optional<HearingListingNeeds> optionalHearingListingNeeds2 = hearingListingNeedsList.stream().filter(x -> x.getProsecutionCases().size() == 2).findFirst();
        assertThat(optionalHearingListingNeeds1.isPresent(), is(true));
        assertThat(optionalHearingListingNeeds2.isPresent(), is(true));

        final Optional<ProsecutionCase> optProsecutionCase2 = optionalHearingListingNeeds2.get().getProsecutionCases().stream().filter(ps -> ps.getId().equals(CASE_ID_2)).findFirst();
        final Optional<ProsecutionCase> optProsecutionCase3 = optionalHearingListingNeeds2.get().getProsecutionCases().stream().filter(ps -> ps.getId().equals(CASE_ID_3)).findFirst();
        assertThat(optProsecutionCase2.isPresent(), is(true));
        assertThat(optProsecutionCase3.isPresent(), is(true));
    }

    @Test
    public void shouldIntoDifferentHearingWhenSlotsAreDifferent() {
        final Map<UUID, Set<UUID>> slotsMap = new HashMap<>();
        slotsMap.put(BOOKING_REFERENCE_1, new HashSet<>(Arrays.asList(COURT_SCHEDULE_ID_1)));
        slotsMap.put(BOOKING_REFERENCE_2, new HashSet<>(Arrays.asList(COURT_SCHEDULE_ID_2)));

        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(slotsMap);
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_2, BOOKING_REFERENCE_1, COURT_LOCATION,null, LISTED_START_DATETIME_1)),
                buildProsecutionCase(CASE_ID_3, DEFENDANT_ID_3, OFFENCE_ID_3, buildNextHearing(HEARING_TYPE_2, BOOKING_REFERENCE_2, COURT_LOCATION,null, LISTED_START_DATETIME_1))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(3));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().size(), is(1));
        assertThat(hearingListingNeedsList.get(1).getProsecutionCases().size(), is(1));
        assertThat(hearingListingNeedsList.get(2).getProsecutionCases().size(), is(1));

    }

    @Test
    public void shouldPopulateCommittingCourtDetails() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        final CommittingCourt committingCourt = TestHelper.buildCommittingCourt();
        final Optional<CommittingCourt> committingCourtOptional = Optional.of(committingCourt);
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(committingCourtOptional);
        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing, true, null);
        assertThat(hearingListingNeedsList.size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().size(), is(2));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(1).getDefendants().size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(0).getCpsOrganisation(), is("A01"));


        final Defendant defendant1 = hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0);
        final Defendant defendant2 = hearingListingNeedsList.get(0).getProsecutionCases().get(1).getDefendants().get(0);
        assertThat(defendant1.getOffences().size(), is(1));
        assertThat(defendant2.getOffences().size(), is(1));
        assertThat(defendant1.getOffences().get(0).getCommittingCourt(), is(notNullValue()));
        assertThat(defendant1.getOffences().get(0).getCommittingCourt().getCourtHouseCode(), is("CCCODE"));
        assertThat(defendant1.getOffences().get(0).getCommittingCourt().getCourtHouseName(), is("Committing Court"));
        assertThat(defendant1.getOffences().get(0).getCommittingCourt().getCourtHouseType(), is(CourtHouseType.MAGISTRATES));
        assertThat(defendant2.getOffences().get(0).getCommittingCourt(), is(notNullValue()));
        assertThat(defendant2.getOffences().get(0).getCommittingCourt().getCourtHouseCode(), is("CCCODE"));
        assertThat(defendant2.getOffences().get(0).getCommittingCourt().getCourtHouseName(), is("Committing Court"));
        assertThat(defendant2.getOffences().get(0).getCommittingCourt().getCourtHouseType(), is(CourtHouseType.MAGISTRATES));

    }

    @Test
    public void shouldNotPopulateCommittingCourtDetails() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing, false, null);
        assertThat(hearingListingNeedsList.size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().size(), is(2));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(1).getDefendants().size(), is(1));

        final Defendant defendant1 = hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0);
        final Defendant defendant2 = hearingListingNeedsList.get(0).getProsecutionCases().get(1).getDefendants().get(0);
        assertThat(defendant1.getOffences().size(), is(1));
        assertThat(defendant2.getOffences().size(), is(1));
        assertThat(defendant1.getOffences().get(0).getCommittingCourt(), is(nullValue()));
        assertThat(defendant2.getOffences().get(0).getCommittingCourt(), is(nullValue()));

    }

}
