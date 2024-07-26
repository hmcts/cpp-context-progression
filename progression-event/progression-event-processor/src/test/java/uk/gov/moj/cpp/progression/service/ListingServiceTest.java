package uk.gov.moj.cpp.progression.service;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.listing.domain.CourtHouseType.MAGISTRATES;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildJsonEnvelope;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.core.courts.ListUnscheduledNextHearings;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.courts.ListNextHearings;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.HearingDay;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.progression.service.dto.HearingList;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings({"squid:S1607", "unused"})
@RunWith(MockitoJUnitRunner.class)
public class ListingServiceTest {

    private static final String LISTING_COMMAND_SEND_CASE_FOR_LISTING = "listing.command.list-court-hearing";
    private static final String LISTING_COMMAND_SEND_LIST_NEXT_HEARINGS = "listing.list-next-hearings-v2";
    private static final String LISTING_COMMAND_SEND_UNSCHEDULED_COURT_HEARING = "listing.command.list-unscheduled-court-hearing";
    private static final String LISTING_COMMAND_SEND_UNSCHEDULED_NEXT_COURT_HEARINGS = "listing.list-unscheduled-next-hearings";
    private static final String LISTING_SEARCH_HEARING = "listing.search.hearing";
    private static final String LISTING_ANY_ALLOCATION_SEARCH_HEARINGS = "listing.any-allocation.search.hearings";

    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Mock
    private Sender sender;
    @InjectMocks
    private ListingService listingService;
    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private Function<Object, JsonEnvelope> objectJsonEnvelopeFunction;
    @Mock
    private Requester requester;
    @Spy
    private UtcClock utcClock;
    @Mock
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldListCourtHearing() throws IOException {
        //given
        ListCourtHearing listCourtHearing = getListCourtHearing();

        final JsonObject listcourthearingjson = createObjectBuilder().build();

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("referral").build(),
                createObjectBuilder().build());

        final JsonEnvelope envelopeListCourtHearing = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(LISTING_COMMAND_SEND_CASE_FOR_LISTING).build(),
                listcourthearingjson);


        when(objectToJsonObjectConverter.convert(any(ListCourtHearing.class)))
                .thenReturn(listcourthearingjson);

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        listingService.listCourtHearing(jsonEnvelope, listCourtHearing);

        when(enveloper.withMetadataFrom(envelopeReferral, LISTING_COMMAND_SEND_CASE_FOR_LISTING)).thenReturn(objectJsonEnvelopeFunction);
        when(objectJsonEnvelopeFunction.apply(any(JsonObject.class))).thenReturn(envelopeListCourtHearing);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Metadata metadata = envelopeArgumentCaptor.getValue().metadata();
        assertThat(metadata.name(), is(LISTING_COMMAND_SEND_CASE_FOR_LISTING));


        verifyNoMoreInteractions(sender);
    }

    @Test
    public void shouldListUnscheduledHearings() {
        //given
        ListUnscheduledCourtHearing listCourtHearing = getListUnscheduledCourtHearing();

        final JsonObject listCourtHearingJson = Json.createObjectBuilder().build();

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final JsonEnvelope envelopeListCourtHearing = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(LISTING_COMMAND_SEND_UNSCHEDULED_COURT_HEARING).build(),
                listCourtHearingJson);


        when(objectToJsonObjectConverter.convert(any(ListCourtHearing.class)))
                .thenReturn(listCourtHearingJson);

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        listingService.listUnscheduledHearings(jsonEnvelope, listCourtHearing);

        when(enveloper.withMetadataFrom(envelopeReferral, LISTING_COMMAND_SEND_UNSCHEDULED_COURT_HEARING)).thenReturn(objectJsonEnvelopeFunction);
        when(objectJsonEnvelopeFunction.apply(any(JsonObject.class))).thenReturn(envelopeListCourtHearing);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Metadata metadata = envelopeArgumentCaptor.getValue().metadata();
        assertThat(metadata.name(), is(LISTING_COMMAND_SEND_UNSCHEDULED_COURT_HEARING));

        verifyNoMoreInteractions(sender);
    }

    @Test
    public void shouldListUnscheduledNextHearings() {
        final JsonObject unscheduledNextHearingsJson = createObjectBuilder().build();
        when(objectToJsonObjectConverter.convert(any(ListUnscheduledNextHearings.class)))
                .thenReturn(unscheduledNextHearingsJson);

        final ListUnscheduledNextHearings unscheduledNextHearings = getListUnscheduledNextHearings();
        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();
        listingService.listUnscheduledNextHearings(jsonEnvelope, unscheduledNextHearings);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Metadata metadata = envelopeArgumentCaptor.getValue().metadata();
        assertThat(metadata.name(), is(LISTING_COMMAND_SEND_UNSCHEDULED_NEXT_COURT_HEARINGS));
        assertThat(envelopeArgumentCaptor.getValue().payload(), is(unscheduledNextHearingsJson));

        verifyNoMoreInteractions(sender);
    }

    @Test
    public void shouldGetShadowListedOffenceIds() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        final Metadata metadata = JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(LISTING_SEARCH_HEARING).build();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withListedCases(getListedCases(Stream.of(offenceId1, offenceId2, offenceId3).collect(toList()), null))
                .build();

        when(envelope.metadata()).thenReturn(metadata);
        when(requester.requestAsAdmin(any(Envelope.class), eq(Hearing.class))).thenReturn(Envelope.envelopeFrom(metadata, hearing));
        final List<UUID> shadowListedOffenceIds = listingService.getShadowListedOffenceIds(envelope, hearingId);

        verify(requester, times(1)).requestAsAdmin(any(Envelope.class), eq(Hearing.class));
        assertThat(shadowListedOffenceIds.size(), is(2));
        assertThat(shadowListedOffenceIds, containsInAnyOrder(offenceId1, offenceId2));
    }

    @Test
    public void shouldGetFutureHearingsForCaseUrn() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        final Metadata metadata = JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(LISTING_ANY_ALLOCATION_SEARCH_HEARINGS).build();

        final Hearing hearing1 = Hearing.hearing()
                .withHearingDays(Arrays.asList(
                        HearingDay.hearingDay().withStartTime(ZonedDateTime.now().minusDays(2)).build(),
                        HearingDay.hearingDay().withStartTime(ZonedDateTime.now().plusDays(2)).build()
                ))
                .build();
        final Hearing hearing2 = Hearing.hearing()
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay().withStartTime(ZonedDateTime.now().plusDays(5)).build()
                ))
                .build();
        final Hearing hearing3 = Hearing.hearing()
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay().withStartTime(ZonedDateTime.now().minusWeeks(1)).build()
                ))
                .build();

        when(envelope.metadata()).thenReturn(metadata);
        when(requester.requestAsAdmin(any(Envelope.class), eq(HearingList.class))).thenReturn(Envelope.envelopeFrom(metadata, new HearingList(
                Arrays.asList(
                        hearing1,
                        hearing2,
                        hearing3
                )
        )));
        final List<Hearing> futureHearings = listingService.getFutureHearings(envelope, "caseUrnValue");

        verify(requester, times(1)).requestAsAdmin(any(Envelope.class), eq(HearingList.class));
        assertThat(futureHearings.size(), is(2));
        assertThat(futureHearings, containsInAnyOrder(hearing1, hearing2));
    }

    @Test
    public void shouldGetFutureHearingsForCaseUrnWithNoResults() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        final Metadata metadata = JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(LISTING_ANY_ALLOCATION_SEARCH_HEARINGS).build();

        when(envelope.metadata()).thenReturn(metadata);
        when(requester.requestAsAdmin(any(Envelope.class), eq(HearingList.class))).thenReturn(Envelope.envelopeFrom(metadata, new HearingList()));
        final List<Hearing> futureHearings = listingService.getFutureHearings(envelope, "caseUrnValue");

        verify(requester, times(1)).requestAsAdmin(any(Envelope.class), eq(HearingList.class));
        assertThat(futureHearings.size(), is(0));
    }

    @Test
    public void shouldListNextHearings() {
        final JsonObject listNextHearingsJson = createObjectBuilder().build();
        when(objectToJsonObjectConverter.convert(any(ListNextHearings.class)))
                .thenReturn(listNextHearingsJson);

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();
        final ListNextHearings listNextHearings = getListNextHearings();
        listingService.listNextCourtHearings(jsonEnvelope, listNextHearings);


        verify(sender).send(envelopeArgumentCaptor.capture());
        final Metadata metadata = envelopeArgumentCaptor.getValue().metadata();
        assertThat(metadata.name(), is(LISTING_COMMAND_SEND_LIST_NEXT_HEARINGS));
        assertThat(envelopeArgumentCaptor.getValue().payload(), is(listNextHearingsJson));

        verifyNoMoreInteractions(sender);
    }

    private List<ListedCase> getListedCases(List<UUID> offenceIds, uk.gov.moj.cpp.listing.domain.CommittingCourt committingCourt) {

        final List<ListedCase> listedCases = Stream.of(ListedCase.listedCase()
                .withDefendants(Stream.of(Defendant.defendant()
                        .withOffences(Stream.of(Offence.offence()
                                .withId(offenceIds.get(0))
                                .withShadowListed(of(Boolean.TRUE)).build()).collect(toList())).build()).collect(toList())).build())
                .collect(toList());

        listedCases.add(ListedCase.listedCase()
                .withShadowListed(of(Boolean.TRUE))
                .withDefendants(Stream.of(Defendant.defendant()
                        .withOffences(Stream.of(Offence.offence()
                                .withId(offenceIds.get(1))
                                .withCommittingCourt(Optional.ofNullable(committingCourt))
                                .build())
                                .collect(toList())).build())
                        .collect(toList())).build());

        listedCases.add(ListedCase.listedCase()
                .withDefendants(Stream.of(Defendant.defendant()
                        .withOffences(Stream.of(Offence.offence()
                                .withId(offenceIds.get(2)).build())
                                .collect(toList())).build())
                        .collect(toList())).build());

        return listedCases;
    }

    private ListCourtHearing getListCourtHearing() {
        return ListCourtHearing.listCourtHearing()
                .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                        .withId(randomUUID())
                        .withCourtCentre(createCourtCenter())
                        .withCourtApplications(createCourtApplications())
                        .withEstimatedMinutes(15)
                        .withEstimatedDuration("1 week")
                        .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                                .withJudicialId(randomUUID())
                                .build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(randomUUID())
                                .build()))
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("SENTENCING")
                                .build())

                        .build()))
                .build();
    }

    private ListNextHearings getListNextHearings() {
        return ListNextHearings.listNextHearings()
                .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                        .withId(randomUUID())
                        .withCourtCentre(createCourtCenter())
                        .withCourtApplications(createCourtApplications())
                        .withEstimatedMinutes(15)
                        .withEstimatedDuration("1 week")
                        .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                                .withJudicialId(randomUUID())
                                .build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(randomUUID())
                                .build()))
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("SENTENCING")
                                .build())

                        .build()))
                .build();
    }

    private ListUnscheduledNextHearings getListUnscheduledNextHearings() {
        return ListUnscheduledNextHearings.listUnscheduledNextHearings()
                .withHearingId(randomUUID())
                .withHearings(Arrays.asList(HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withId(randomUUID())
                        .withCourtCentre(createCourtCenter())
                        .withCourtApplications(createCourtApplications())
                        .withEstimatedMinutes(15)
                        .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                                .withJudicialId(randomUUID())
                                .build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(randomUUID())
                                .build()))
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("SENTENCING")
                                .build())

                        .build()))
                .build();
    }

    private List<CourtApplication> createCourtApplications() {
        List<CourtApplication> courtApplications = new ArrayList<>();
        courtApplications.add(CourtApplication.courtApplication()
                .withId(randomUUID())
                .withCourtApplicationCases(
                        singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(randomUUID()).build()))
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .withMasterDefendant(uk.gov.justice.core.courts.MasterDefendant.masterDefendant()
                                .withMasterDefendantId(UUID.randomUUID())
                                .build())
                        .build())
                .withRespondents(Arrays.asList(CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(randomUUID())

                                .build())
                        .build()))
                .build());
        return courtApplications;
    }

    private CourtCentre createCourtCenter() {
        return CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName("Court Name")
                .withRoomId(randomUUID())
                .withRoomName("Court Room Name")
                .withWelshName("Welsh Name")
                .withWelshRoomName("Welsh Room Name")
                .withAddress(Address.address()
                        .withAddress1("Address 1")
                        .withAddress2("Address 2")
                        .withAddress3("Address 3")
                        .withAddress4("Address 4")
                        .withAddress5("Address 5")
                        .withPostcode("DD4 4DD")
                        .build())
                .build();
    }

    private ListUnscheduledCourtHearing getListUnscheduledCourtHearing() {
        return ListUnscheduledCourtHearing.listUnscheduledCourtHearing()
                .withHearings(Arrays.asList(HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withId(randomUUID())
                        .withCourtCentre(createCourtCenter())
                        .withCourtApplications(createCourtApplications())
                        .withEstimatedMinutes(15)
                        .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                                .withJudicialId(randomUUID())
                                .build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(randomUUID())
                                .build()))
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("SENTENCING")
                                .build())

                        .build()))
                .build();
    }

    @Test
    public void shouldNotReturnACommittingCourtIfNotAvailableInPreviousHearing() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        final Metadata metadata = JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(LISTING_SEARCH_HEARING).build();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withListedCases(getListedCases(Stream.of(offenceId1, offenceId2, offenceId3).collect(toList()), null))
                .build();

        when(envelope.metadata()).thenReturn(metadata);
        when(requester.requestAsAdmin(any(Envelope.class), eq(Hearing.class))).thenReturn(Envelope.envelopeFrom(metadata, hearing));
        final Optional<CommittingCourt> committingCourt = listingService.getCommittingCourt(envelope, hearingId);

        assertFalse(committingCourt.isPresent());

    }

    @Test
    public void shouldReturnACommittingCourtIfAvailableInPreviousHearing() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        final Metadata metadata = JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(LISTING_SEARCH_HEARING).build();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final uk.gov.moj.cpp.listing.domain.CommittingCourt committingCourt = uk.gov.moj.cpp.listing.domain.CommittingCourt.committingCourt()
                .withCourtCentreId(randomUUID())
                .withCourtHouseName("CourtHouseName")
                .withCourtHouseType(MAGISTRATES)
                .withCourtHouseShortName("CourtHouseShortName")
                .withCourtHouseCode("CourtHouseShortCode")
                .build();

        final Hearing hearing = Hearing.hearing()
                .withListedCases(getListedCases(Stream.of(offenceId1, offenceId2, offenceId3).collect(toList()), committingCourt))
                .build();
        when(envelope.metadata()).thenReturn(metadata);
        when(requester.requestAsAdmin(any(Envelope.class), eq(Hearing.class))).thenReturn(Envelope.envelopeFrom(metadata, hearing));
        final Optional<CommittingCourt> committingCourtToCheck = listingService.getCommittingCourt(envelope, hearingId);

        assertTrue(committingCourtToCheck.isPresent());

    }
}
