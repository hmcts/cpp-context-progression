package uk.gov.moj.cpp.progression.transformer;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildNextHearing;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildNextHearingForApplication;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildProsecutionCase;
import static uk.gov.moj.cpp.progression.test.FileUtil.getPayload;

import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.NextHearingsRequested;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.helper.HearingBookingReferenceListExtractor;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.helper.TestHelper;
import uk.gov.moj.cpp.progression.service.ProvisionalBookingServiceAdapter;
import uk.gov.moj.cpp.progression.service.utils.OffenceToCommittingCourtConverter;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class HearingToHearingListingNeedsTransformerTest {
    private static final UUID CASE_ID_1 = randomUUID();
    private static final UUID DEFENDANT_ID_1 = randomUUID();
    private static final UUID OFFENCE_ID_1 = randomUUID();
    private static final UUID REPORTING_RESTRICTION_ID_1 = randomUUID();

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

    @Mock
    private Logger logger;

    @InjectMocks
    private HearingToHearingListingNeedsTransformer transformer;

    @Mock
    private HearingBookingReferenceListExtractor hearingBookingReferenceListExtractor;

    @Mock
    private ProvisionalBookingServiceAdapter provisionalBookingServiceAdapter;

    @Spy
    private HearingResultHelper hearingResultHelper;

    @Mock
    private OffenceToCommittingCourtConverter offenceToCommittingCourtConverter;

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(objectMapper);

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

        if (defendant1.getId().equals(DEFENDANT_ID_1)) {
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
        if (prosecutionCase1.getDefendants().get(0).getId().equals(DEFENDANT_ID_1)) {
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
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, null, LISTED_START_DATETIME_1)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_2, null, COURT_LOCATION, null, LISTED_START_DATETIME_2))
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
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_2, null, COURT_LOCATION, null, LISTED_START_DATETIME_1))
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
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_2, BOOKING_REFERENCE_1, COURT_LOCATION, null, LISTED_START_DATETIME_1)),
                buildProsecutionCase(CASE_ID_3, DEFENDANT_ID_3, OFFENCE_ID_3, buildNextHearing(HEARING_TYPE_2, BOOKING_REFERENCE_2, COURT_LOCATION, null, LISTED_START_DATETIME_1))
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
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_2, BOOKING_REFERENCE_1, COURT_LOCATION, null, LISTED_START_DATETIME_1)),
                buildProsecutionCase(CASE_ID_3, DEFENDANT_ID_3, OFFENCE_ID_3, buildNextHearing(HEARING_TYPE_2, BOOKING_REFERENCE_2, COURT_LOCATION, null, LISTED_START_DATETIME_1))
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
        assertThat(defendant1.getOffences().get(0).getCommittingCourt().getCourtHouseType(), is(JurisdictionType.MAGISTRATES));
        assertThat(defendant2.getOffences().get(0).getCommittingCourt(), is(notNullValue()));
        assertThat(defendant2.getOffences().get(0).getCommittingCourt().getCourtHouseCode(), is("CCCODE"));
        assertThat(defendant2.getOffences().get(0).getCommittingCourt().getCourtHouseName(), is("Committing Court"));
        assertThat(defendant2.getOffences().get(0).getCommittingCourt().getCourtHouseType(), is(JurisdictionType.MAGISTRATES));

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

    @Test
    public void shouldCarryReportingRestrictionDetails() {
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearing(Collections.singletonList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, REPORTING_RESTRICTION_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing, false, null);
        assertThat(hearingListingNeedsList.size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().size(), is(1));

        final Defendant defendant1 = hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(defendant1.getOffences().size(), is(1));

        final Offence offence1 = defendant1.getOffences().get(0);
        assertThat(offence1.getId(), is(OFFENCE_ID_1));
        assertThat(offence1.getCommittingCourt(), is(nullValue()));

        final ReportingRestriction reportingRestriction1 = offence1.getReportingRestrictions().get(0);
        assertThat(reportingRestriction1, is(notNullValue()));
        assertThat(reportingRestriction1.getId(), is(REPORTING_RESTRICTION_ID_1));
    }

    @Test
    public void shouldReturnOneHearingNeedsWithSeededHearingIdsOnOffencesWhenTwoHearingMatch() {
        final UUID seedingHearingId = randomUUID();
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final ZonedDateTime listedStartDateTime = ZonedDateTime.now();
        final Hearing hearing = TestHelper.buildHearingWithNextDayAsHearingDays(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, listedStartDateTime)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, listedStartDateTime))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), null);

        assertThat(hearingListingNeedsList.size(), is(1));

        final Defendant defendant1 = hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0);
        final Defendant defendant2 = hearingListingNeedsList.get(0).getProsecutionCases().get(1).getDefendants().get(0);
        assertThat(defendant1.getOffences().get(0).getSeedingHearing().getSeedingHearingId(), equalTo(seedingHearingId));
        assertThat(defendant2.getOffences().get(0).getSeedingHearing().getSeedingHearingId(), equalTo(seedingHearingId));
    }

    @Test
    public void shouldReturnHearingNeedsWithOneApplicationWhenMoreThanOneJudicialResultsPresentForSameApplication() {
        final NextHearingsRequested nextHearingsRequested = jsonObjectConverter.convert(getHearing("progression.event.next-hearings-requested.json"), NextHearingsRequested.class);
        final Hearing hearing = nextHearingsRequested.getHearing();
        final Optional<CommittingCourt> committingCourt = ofNullable(nextHearingsRequested.getCommittingCourt());


        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, committingCourt, hearing.getSeedingHearing()
                , null);
        assertThat(hearingListingNeedsList.size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getCourtApplications().size(), is(1));
    }



    @Test
    public void shouldReturnNoHearingNeedsWithSeededHearingIdsOnOffencesWhenListingStartDateOutsideOfMultiDaysHearing() {
        final UUID seedingHearingId = randomUUID();

        final ZonedDateTime listedStartDateTime = ZonedDateTime.now();
        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, listedStartDateTime)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, listedStartDateTime))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), null);

        assertThat(hearingListingNeedsList.size(), is(0));
    }

    @Test
    public void shouldReturnHearingNeedsWithSeededHearingIdsOnOffencesWhenSingleDayHearing() {
        final UUID seedingHearingId = randomUUID();
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final ZonedDateTime listedStartDateTime = ZonedDateTime.now();
        final Hearing hearing = TestHelper.buildSingleDayHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, listedStartDateTime)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, listedStartDateTime))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), null);

        assertThat(hearingListingNeedsList.size(), is(1));
    }

    @Test
    public void shouldReturnOneHearingNeedsWithSeededHearingIdsOnOffencesWhenOneListingStartDateWithInMultiDaysHearingAndAnotherOutside() {
        final UUID seedingHearingId = randomUUID();
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final ZonedDateTime listedStartDateTime = ZonedDateTime.now();
        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, listedStartDateTime)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, listedStartDateTime.plusDays(2)))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), null);

        assertThat(hearingListingNeedsList.size(), is(1));
        final Defendant defendant1 = hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(defendant1.getOffences().get(0).getSeedingHearing().getSeedingHearingId(), equalTo(seedingHearingId));
    }

    @Test
    public void shouldReturnOneHearingNeedsWithSeededHearingIdsOnOffencesWhenListingStartDateIsNull() {
        final UUID seedingHearingId = randomUUID();
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearingWithNextDayAsHearingDays(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), null);

        assertThat(hearingListingNeedsList.size(), is(1));

        final Defendant defendant1 = hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(defendant1.getOffences().get(0).getSeedingHearing().getSeedingHearingId(), equalTo(seedingHearingId));
    }

    @Test
    public void shouldNotReturnHearingNeedsWithSeedHearingWhenOneDefendantTwoOffencesBothApplication() {
        final UUID seedingHearingId = randomUUID();

        final Hearing hearing = TestHelper.buildHearingWithNextDayAsHearingDays(Arrays.asList(
                ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID_1)
                        .withCpsOrganisation("A01")
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                .withId(DEFENDANT_ID_1)
                                .withOffences(Arrays.asList(Offence.offence()
                                        .withId(OFFENCE_ID_1)
                                        .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                .withNextHearing(buildNextHearingForApplication(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
                                                .build()))
                                        .build(),
                                        Offence.offence()
                                                .withId(OFFENCE_ID_2)
                                                .withJudicialResults(Arrays.asList(
                                                        JudicialResult.judicialResult()
                                                                .withNextHearing(buildNextHearingForApplication(HEARING_TYPE_2, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
                                                                .build()))
                                                .build()))
                                .build()))
                        .build()
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), null);

        assertThat(hearingListingNeedsList.size(), is(0));
    }

    @Test
    public void shouldOnlyReturnOneHearingNeedsWithSeedHearingWhenOneDefendantTwoOffencesJustOneNonApplication() {
        final UUID seedingHearingId = randomUUID();
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearingWithNextDayAsHearingDays(Arrays.asList(
                ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID_1)
                        .withCpsOrganisation("A01")
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                .withId(DEFENDANT_ID_1)
                                .withOffences(Arrays.asList(Offence.offence()
                                                .withId(OFFENCE_ID_1)
                                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                        .withNextHearing(buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
                                                        .build()))
                                                .build(),
                                        Offence.offence()
                                                .withId(OFFENCE_ID_2)
                                                .withJudicialResults(Arrays.asList(
                                                        JudicialResult.judicialResult()
                                                                .withNextHearing(buildNextHearingForApplication(HEARING_TYPE_2, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
                                                                .build()))
                                                .build()))
                                .build()))
                        .build()
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), null);

        assertThat(hearingListingNeedsList.size(), is(1));

        assertThat(hearingListingNeedsList.get(0).getType().getId(), is(HEARING_TYPE_1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(OFFENCE_ID_1));
    }

    @Test
    public void shouldNotReturnHearingNeedsWithSeedHearingWhenTwoDefendantsAllOffencesBothApplication() {
        final UUID seedingHearingId = randomUUID();
        final Hearing hearing = TestHelper.buildHearingWithNextDayAsHearingDays(Arrays.asList(
                ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID_1)
                        .withCpsOrganisation("A01")
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                .withId(DEFENDANT_ID_1)
                                .withOffences(Arrays.asList(Offence.offence()
                                                .withId(OFFENCE_ID_1)
                                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                        .withNextHearing(buildNextHearingForApplication(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
                                                        .build()))
                                                .build()))
                                .build(),
                                Defendant.defendant()
                                        .withId(DEFENDANT_ID_2)
                                        .withOffences(Arrays.asList(Offence.offence()
                                                        .withId(OFFENCE_ID_2)
                                                        .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                                .withNextHearing(buildNextHearingForApplication(HEARING_TYPE_2, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
                                                                .build()))
                                                        .build()))
                                        .build()))
                        .build()
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), null);

        assertThat(hearingListingNeedsList.size(), is(0));
    }

    @Test
    public void shouldReturnHearingNeedsWithSeedHearingWhenTwoDefendantsOneOffenceIsNonApplication() {
        final UUID seedingHearingId = randomUUID();
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearingWithNextDayAsHearingDays(Arrays.asList(
                ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID_1)
                        .withCpsOrganisation("A01")
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(DEFENDANT_ID_1)
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(OFFENCE_ID_1)
                                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                        .withNextHearing(buildNextHearingForApplication(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
                                                        .build()))
                                                .build()))
                                        .build(),
                                Defendant.defendant()
                                        .withId(DEFENDANT_ID_2)
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(OFFENCE_ID_2)
                                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                        .withNextHearing(buildNextHearing(HEARING_TYPE_2, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build()
        ));

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), null);

        assertThat(hearingListingNeedsList.size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getType().getId(), is(HEARING_TYPE_2));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0).getId(), is(DEFENDANT_ID_2));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(OFFENCE_ID_2));
    }

    @Test
    public void givenOffenceAndApplicationHaveSameNextHearing_shouldHaveHearingNeedsWithNextHearingsThatIncludeOffenceAndApplicationForThatNextHearing() {
        final UUID seedingHearingId = randomUUID();
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = fromEventPayloadJson("next-hearings/progression.event.next-hearings-requested-2next-hearings-1offence1_application-1offence2.json");
        final Map<UUID, List<UUID>> alreadyExistingBookingReferenceAndCourtScheduleIds = new HashMap<>();
        final Map<UUID, Set<UUID>> combinedBookingReferencesAndCourtScheduleIds = transformer.getCombinedBookingReferencesAndCourtScheduleIds(hearing, alreadyExistingBookingReferenceAndCourtScheduleIds);

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), combinedBookingReferencesAndCourtScheduleIds);

        assertThat(hearingListingNeedsList.size(), is(2));

        final Defendant defendant1 = hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(defendant1.getOffences().get(0).getSeedingHearing().getSeedingHearingId(), equalTo(seedingHearingId));

        final Optional<JudicialResult> nextHearing = defendant1.getOffences().get(0).getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing())).findFirst();
        final Optional<JudicialResult> nextHearingOnApplication = hearingListingNeedsList.get(0).getCourtApplications().get(0).getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing())).findFirst();

        assertThat(nextHearing.get().getNextHearing(), is(nextHearingOnApplication.get().getNextHearing()));

        final Defendant defendant11 = hearingListingNeedsList.get(1).getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(defendant1.getOffences().get(0).getSeedingHearing().getSeedingHearingId(), equalTo(seedingHearingId));

        final Optional<JudicialResult> nextHearing1 = defendant11.getOffences().get(0).getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing())).findFirst();

        assertThat(nextHearing1.get().getNextHearing(), not(equalTo(nextHearing.get().getNextHearing())));

    }

    @Test
    public void givenMultipleOffencesAndApplicationHaveSameNextHearing_shouldHaveHearingNeedsWithNextHearingsThatIncludeOffencesAndApplicationForThatNextHearing() {
        final UUID seedingHearingId = randomUUID();
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = fromEventPayloadJson("next-hearings/progression.event.next-hearings-requested-1next-hearing-2offences_application.json");
        final Map<UUID, List<UUID>> alreadyExistingBookingReferenceAndCourtScheduleIds = new HashMap<>();
        final Map<UUID, Set<UUID>> combinedBookingReferencesAndCourtScheduleIds = transformer.getCombinedBookingReferencesAndCourtScheduleIds(hearing, alreadyExistingBookingReferenceAndCourtScheduleIds);

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), combinedBookingReferencesAndCourtScheduleIds);

        assertThat(hearingListingNeedsList.size(), is(1));

        final Defendant defendant1 = hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0);
        assertThat(defendant1.getOffences().size(), is(2));
        assertThat(defendant1.getOffences().stream().allMatch(o -> o.getSeedingHearing().getSeedingHearingId().equals(seedingHearingId)), is(true));

        final Optional<JudicialResult> nextHearingOfc1 = defendant1.getOffences().get(0).getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing())).findFirst();
        final Optional<JudicialResult> nextHearingOfc2 = defendant1.getOffences().get(1).getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing())).findFirst();
        final Optional<JudicialResult> nextHearingOnApplication = hearingListingNeedsList.get(0).getCourtApplications().get(0).getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing())).findFirst();

        assertThat(nextHearingOfc1.get().getNextHearing(), is(nextHearingOfc2.get().getNextHearing()));
        assertThat(nextHearingOfc1.get().getNextHearing(), is(nextHearingOnApplication.get().getNextHearing()));
        assertThat(nextHearingOfc2.get().getNextHearing(), is(nextHearingOnApplication.get().getNextHearing()));
    }

    @Test
    public void givenMultiDefendantsMultiOffencesAndApplicationHaveSameNextHearing_shouldHaveHearingNeedsWithNextHearingsThatIncludeOffencesAndApplicationForThatNextHearing() {
        final UUID seedingHearingId = randomUUID();
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = fromEventPayloadJson("next-hearings/progression.event.next-hearings-requested-2next-hearing-2offences_application_2offences.json");
        final Map<UUID, List<UUID>> alreadyExistingBookingReferenceAndCourtScheduleIds = new HashMap<>();
        final Map<UUID, Set<UUID>> combinedBookingReferencesAndCourtScheduleIds = transformer.getCombinedBookingReferencesAndCourtScheduleIds(hearing, alreadyExistingBookingReferenceAndCourtScheduleIds);

        final List<HearingListingNeeds> hearingListingNeedsList = transformer.transformWithSeedHearing(hearing, Optional.empty(), SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build(), combinedBookingReferencesAndCourtScheduleIds);

        assertThat(hearingListingNeedsList.size(), is(2));

        final Defendant hearing1defendant1 = hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(0);
        final Defendant hearing1defendant2 = hearingListingNeedsList.get(0).getProsecutionCases().get(0).getDefendants().get(1);
        assertThat(hearing1defendant1.getOffences().size(), is(1));
        assertThat(hearing1defendant2.getOffences().size(), is(1));

        final Optional<JudicialResult> nextHearingDef1Ofc1 = hearing1defendant1.getOffences().get(0).getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing())).findFirst();
        final Optional<JudicialResult> nextHearingDef2Ofc1 = hearing1defendant2.getOffences().get(0).getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing())).findFirst();
        final Optional<JudicialResult> nextHearingOnApplication1 = hearingListingNeedsList.get(0).getCourtApplications().get(0).getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing())).findFirst();
        assertThat(nextHearingDef1Ofc1.get().getNextHearing(), is(nextHearingOnApplication1.get().getNextHearing()));
        assertThat(nextHearingDef2Ofc1.get().getNextHearing(), is(nextHearingDef2Ofc1.get().getNextHearing()));

        final Defendant hearing2defendant1 = hearingListingNeedsList.get(1).getProsecutionCases().get(0).getDefendants().get(0);
        final Defendant hearing2defendant2 = hearingListingNeedsList.get(1).getProsecutionCases().get(0).getDefendants().get(1);
        assertThat(hearing2defendant1.getOffences().size(), is(1));
        assertThat(hearing2defendant2.getOffences().size(), is(1));

        final Optional<JudicialResult> nextHearing2Def1Ofc2 = hearing2defendant1.getOffences().get(0).getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing())).findFirst();
        final Optional<JudicialResult> nextHearing2Def2Ofc2 = hearing2defendant2.getOffences().get(0).getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing())).findFirst();
        assertThat(nextHearing2Def1Ofc2.get().getNextHearing(), is(nextHearing2Def2Ofc2.get().getNextHearing()));
    }

    private Hearing fromEventPayloadJson(final String nextHearingsRequestedJsonFile) {
        final NextHearingsRequested nextHearingsRequested = jsonObjectConverter.convert(getHearing(nextHearingsRequestedJsonFile), NextHearingsRequested.class);
        return nextHearingsRequested.getHearing();
    }

    @Test
    public void shouldReturnOneAlreadyExistingBookingReferenceWithCourtScheduleIds() {

        final UUID bookingReferenceId = randomUUID();
        final Map<UUID, List<UUID>> alreadyExistingBookingReferenceAndCourtScheduleIds = new HashMap<>();
        alreadyExistingBookingReferenceAndCourtScheduleIds.put(bookingReferenceId, Arrays.asList(randomUUID(), randomUUID()));

        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(new HashMap<>());

        final Hearing hearing = TestHelper.buildHearingWithNextDayAsHearingDays(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, null, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final Map<UUID, Set<UUID>> combinedBookingReferencesAndCourtScheduleIds = transformer.getCombinedBookingReferencesAndCourtScheduleIds(hearing, alreadyExistingBookingReferenceAndCourtScheduleIds);

        assertThat(combinedBookingReferencesAndCourtScheduleIds.size(), is(1));
        assertThat(combinedBookingReferencesAndCourtScheduleIds.containsKey(bookingReferenceId), is(true));
        assertThat(combinedBookingReferencesAndCourtScheduleIds.get(bookingReferenceId).size(), is(2));

    }

    @Test
    public void shouldReturnNewBookingReferenceWithCourtScheduleIds() {

        final UUID bookingReferenceId = randomUUID();

        final Map<UUID, List<UUID>> alreadyExistingBookingReferenceAndCourtScheduleIds = new HashMap<>();

        final Map<UUID, Set<UUID>> newBookingReferenceAndCourtScheduleIds = new HashMap<>();
        newBookingReferenceAndCourtScheduleIds.put(bookingReferenceId, new HashSet<>(Arrays.asList(randomUUID(), randomUUID())));

        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(newBookingReferenceAndCourtScheduleIds);

        final Hearing hearing = TestHelper.buildHearingWithNextDayAsHearingDays(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, bookingReferenceId, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final Map<UUID, Set<UUID>> combinedBookingReferencesAndCourtScheduleIds = transformer.getCombinedBookingReferencesAndCourtScheduleIds(hearing, alreadyExistingBookingReferenceAndCourtScheduleIds);

        assertThat(combinedBookingReferencesAndCourtScheduleIds.size(), is(1));
        assertThat(combinedBookingReferencesAndCourtScheduleIds.containsKey(bookingReferenceId), is(true));
        assertThat(combinedBookingReferencesAndCourtScheduleIds.get(bookingReferenceId).size(), is(2));

    }

    @Test
    public void shouldReturnOneAlreadyExistingBookingReferenceAndNewBookingReferenceWithCourtScheduleIds() {

        final UUID bookingReferenceId1 = randomUUID();
        final UUID bookingReferenceId2 = randomUUID();

        final Map<UUID, List<UUID>> alreadyExistingBookingReferenceAndCourtScheduleIds = new HashMap<>();
        alreadyExistingBookingReferenceAndCourtScheduleIds.put(bookingReferenceId1, Arrays.asList(randomUUID(), randomUUID()));

        final Map<UUID, Set<UUID>> newBookingReferenceAndCourtScheduleIds = new HashMap<>();
        newBookingReferenceAndCourtScheduleIds.put(bookingReferenceId2, new HashSet<>(Arrays.asList(randomUUID(), randomUUID())));

        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(newBookingReferenceAndCourtScheduleIds);

        final Hearing hearing = TestHelper.buildHearingWithNextDayAsHearingDays(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, bookingReferenceId2, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
        ));

        final Map<UUID, Set<UUID>> combinedBookingReferencesAndCourtScheduleIds = transformer.getCombinedBookingReferencesAndCourtScheduleIds(hearing, alreadyExistingBookingReferenceAndCourtScheduleIds);

        assertThat(combinedBookingReferencesAndCourtScheduleIds.size(), is(2));
        assertThat(combinedBookingReferencesAndCourtScheduleIds.containsKey(bookingReferenceId1), is(true));
        assertThat(combinedBookingReferencesAndCourtScheduleIds.containsKey(bookingReferenceId2), is(true));
        assertThat(combinedBookingReferencesAndCourtScheduleIds.get(bookingReferenceId1).size(), is(2));
        assertThat(combinedBookingReferencesAndCourtScheduleIds.get(bookingReferenceId2).size(), is(2));

    }

    @Test
    public void shouldAdjournmentSingleHearing() {
        final UUID bookingId = UUID.fromString("e42fc616-be2f-41a1-81ce-c04d8c0454a1");
        final Map<UUID, Set<UUID>> slotsMap = new HashMap<>();
        slotsMap.put(bookingId, new HashSet<>(Arrays.asList(COURT_SCHEDULE_ID_1)));

        when(hearingBookingReferenceListExtractor.extractBookingReferenceList(any())).thenReturn(Arrays.asList(bookingId));
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(slotsMap);
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = jsonObjectConverter.convert(getHearing("public.hearing.resulted-adjournment.json"), Hearing.class);
        List<HearingListingNeeds> hearingListingNeedsList = transformer.transform(hearing);
        assertThat(hearingListingNeedsList.size(), is(1));
        HearingListingNeeds hearingListingNeeds = hearingListingNeedsList.get(0);
        assertThat(hearingListingNeeds.getCourtApplications().size(), is(2));
        assertThat(hearingListingNeeds.getProsecutionCases().size(), is(1));
    }

    @Test
    public void shouldReturnTwoHearingNeedsWhenTwoHearingTypeNotMatchWithBookingReferenceIsPresent() {
        final UUID bookingReferenceId1 = randomUUID();
        final UUID bookingReferenceId2 = randomUUID();
        final Map<UUID, Set<UUID>> slotsMap = new HashMap<>();
        slotsMap.put(bookingReferenceId1, new HashSet<>(Arrays.asList(COURT_SCHEDULE_ID_1)));
        slotsMap.put(bookingReferenceId2, new HashSet<>(Arrays.asList(COURT_SCHEDULE_ID_1)));
        when(provisionalBookingServiceAdapter.getSlots(anyList())).thenReturn(slotsMap);
        when(offenceToCommittingCourtConverter.convert(any(), any(), any())).thenReturn(Optional.empty());

        final Hearing hearing = TestHelper.buildHearing(Arrays.asList(
                buildProsecutionCase(CASE_ID_1, DEFENDANT_ID_1, OFFENCE_ID_1, buildNextHearing(HEARING_TYPE_1, bookingReferenceId1, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null)),
                buildProsecutionCase(CASE_ID_2, DEFENDANT_ID_2, OFFENCE_ID_2, buildNextHearing(HEARING_TYPE_2, bookingReferenceId2, COURT_LOCATION, WEEK_COMMENCING_DATE_1, null))
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
        if (prosecutionCase1.getDefendants().get(0).getId().equals(DEFENDANT_ID_1)) {
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

    private JsonObject getHearing(final String path) {
        return stringToJsonObjectConverter.convert(getPayload(path));
    }
}
