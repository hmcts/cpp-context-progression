package uk.gov.moj.cpp.progression.service;

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
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildJsonEnvelope;
import static uk.gov.moj.cpp.progression.service.ListingService.LISTING_COMMAND_SEND_CASE_FOR_LISTING;
import static uk.gov.moj.cpp.progression.service.ListingService.LISTING_COMMAND_SEND_UNSCHEDULED_COURT_HEARING;

@SuppressWarnings({"squid:S1607","unused"})
@RunWith(MockitoJUnitRunner.class)
public class ListingServiceTest {

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

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldListCourtHearing() throws IOException {
        //given
        ListCourtHearing listCourtHearing = getListCourtHearing();

        final JsonObject ListCourtHearingJson = Json.createObjectBuilder().build();

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final JsonEnvelope envelopeListCourtHearing = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName(LISTING_COMMAND_SEND_CASE_FOR_LISTING).build(),
                ListCourtHearingJson);


        when(objectToJsonObjectConverter.convert(any(ListCourtHearing.class)))
                .thenReturn(ListCourtHearingJson);

        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        listingService.listCourtHearing(jsonEnvelope,listCourtHearing);

        when(enveloper.withMetadataFrom(envelopeReferral, LISTING_COMMAND_SEND_CASE_FOR_LISTING)).thenReturn(objectJsonEnvelopeFunction);
        when(objectJsonEnvelopeFunction.apply(any(JsonObject.class))).thenReturn(envelopeListCourtHearing);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Metadata metadata = envelopeArgumentCaptor.getValue().metadata();
        assertThat(metadata.name(), is(LISTING_COMMAND_SEND_CASE_FOR_LISTING));


        verifyNoMoreInteractions(sender);
    }

    @Test
    public void shouldListUnscheduledHearings(){
        //given
        ListUnscheduledCourtHearing listCourtHearing = getListUnscheduledCourtHearing();

        final JsonObject listCourtHearingJson = Json.createObjectBuilder().build();

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final JsonEnvelope envelopeListCourtHearing = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName(LISTING_COMMAND_SEND_UNSCHEDULED_COURT_HEARING).build(),
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

    private ListCourtHearing getListCourtHearing() {
        return ListCourtHearing.listCourtHearing()
                .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                        .withId(UUID.randomUUID())
                        .withCourtCentre(createCourtCenter())
                        .withCourtApplications(createCourtApplications())
                        .withEstimatedMinutes(15)
                        .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                                .withJudicialId(UUID.randomUUID())
                                .build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(UUID.randomUUID())
                                .build()))
                        .withType(HearingType.hearingType()
                                .withId(UUID.randomUUID())
                                .withDescription("SENTENCING")
                                .build())

                        .build()))
                .build();
    }

    private List<CourtApplication> createCourtApplications() {
        List<CourtApplication> courtApplications = new ArrayList<>();
        courtApplications.add(CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withLinkedCaseId(UUID.randomUUID())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withId(UUID.randomUUID())
                        .withDefendant(Defendant.defendant()
                                .withId(UUID.randomUUID())
                                .build())
                        .build())
                .withRespondents(Arrays.asList(CourtApplicationRespondent.courtApplicationRespondent()
                        .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                .withId(UUID.randomUUID())
                                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                        .withProsecutionAuthorityId(UUID.randomUUID())

                                        .build())
                                .build())

                        .build()))
                .build());
        return courtApplications;
    }

    private CourtCentre createCourtCenter() {
        return CourtCentre.courtCentre()
                .withId(UUID.randomUUID())
                .withName("Court Name")
                .withRoomId(UUID.randomUUID())
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
                        .withId(UUID.randomUUID())
                        .withCourtCentre(createCourtCenter())
                        .withCourtApplications(createCourtApplications())
                        .withEstimatedMinutes(15)
                        .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                                .withJudicialId(UUID.randomUUID())
                                .build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(UUID.randomUUID())
                                .build()))
                        .withType(HearingType.hearingType()
                                .withId(UUID.randomUUID())
                                .withDescription("SENTENCING")
                                .build())

                        .build()))
                .build();
    }
}
