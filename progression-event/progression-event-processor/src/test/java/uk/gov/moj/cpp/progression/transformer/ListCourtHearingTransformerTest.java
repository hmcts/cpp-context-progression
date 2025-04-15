package uk.gov.moj.cpp.progression.transformer;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.ReferredDefendant;
import uk.gov.justice.core.courts.ReferredHearingType;
import uk.gov.justice.core.courts.ReferredListHearingRequest;
import uk.gov.justice.core.courts.ReferredOffence;
import uk.gov.justice.core.courts.ReferredPerson;
import uk.gov.justice.core.courts.ReferredPersonDefendant;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.core.courts.ReferringJudicialDecision;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.justice.core.courts.SjpReferral;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.model.HearingListing;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListCourtHearingTransformerTest {

    final private String postcode = "CR11111";
    final private String prosecutingAuth = "CPS";
    final private UUID prosecutionCaseId = UUID.randomUUID();
    final private UUID defendantId = UUID.randomUUID();
    final private UUID masterDefendantId = UUID.randomUUID();
    final private UUID offenceId = UUID.randomUUID();
    final private UUID courtCenterId = UUID.randomUUID();
    final private ZonedDateTime courtProceedingsInitiated = ZonedDateTime.now(ZoneId.of("UTC"));
    final private int estimateMinutes = 15;
    final private String estimateDuration = "1 week";
    final private String referralDate = "2018-02-15";
    final private String expectedDate = "2018-03-01";
    final private ZonedDateTime listedStartDateTime = ZonedDateTime.parse("2019-06-30T18:32:04.238Z");
    final private ZonedDateTime earliestStartDateTime = ZonedDateTime.parse("2019-05-30T18:32:04.238Z");
    private static final String AUTOMATIC_ANONYMITY = "Automatic anonymity";
    private static final UUID MARKER_TYPE_ID = UUID.randomUUID();
    private static final String MARKER_TYPE_CODE = "MarkerTypeCode";
    private static final String MARKER_TYPE_DESCRIPTION = "MarkerTypeDescription";
    private static final String cpsOrganisation = "A01";
    private static final String TRANSFER = "Transfer";
    final private UUID prosecutorId = UUID.randomUUID();
    final private String prosecutorCode = "CPS-SW";

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @InjectMocks
    private ListCourtHearingTransformer listCourtHearingTransformer;
    @Mock
    private RefDataService referenceDataService;
    @Mock
    private ProgressionService progressionService;


    @Test
    void shouldTransformToListCourtHearing() {

        final SjpCourtReferral courtReferral = getCourtReferral();
        final JsonObject payload = createPayloadForOrgUnits(randomUUID().toString());


        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final JsonObject jsonObject = Json.createObjectBuilder().add("hearingDescription", "British").build();

        when(referenceDataService.getHearingType(any(), any(UUID.class), any())).thenReturn(Optional.of(jsonObject));
        when(referenceDataService.getCourtsByPostCodeAndProsecutingAuthority(any(), any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder()
                        .add("courts", createArrayBuilder()
                                .add(Json.createObjectBuilder().add("oucode", "Redditch").add("oucodeL3Code", "B22KS00").build())
                                .build())
                        .build()));
        when(referenceDataService.getCourtCentre("Redditch", envelopeReferral,requester))
                .thenReturn(CourtCentre.courtCentre()
                        .withId(courtCenterId)
                        .withName("South Western (Lavender Hill)")
                        .withWelshName("welshName_Test").build());
        when(referenceDataService.getReferralReasonByReferralReasonId(any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("reason", "reason for referral").build()));

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral, List.of(getProsecutionCase()), courtReferral.getSjpReferral(), courtReferral.getListHearingRequests(), UUID.randomUUID());

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
        assertThat(listCourtHearing.getHearings().get(0).getEarliestStartDateTime().toLocalDate().minusDays(14).toString(), is(referralDate));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        validateDefendant(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is(postcode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getCpsOrganisation(), is(cpsOrganisation));
    }


    @Test
    void shouldTransformSJPReferToListCourtHearing() {

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final CourtCentre courtCentre = createCourtCentre();
        when(progressionService.transformCourtCentre(any(), any())).thenReturn(courtCentre);
        when(referenceDataService.getReferralReasonByReferralReasonId(any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("reason", "reason for referral").build()));

        final NextHearing nextHearing = createNextHearing();
        final List<ListDefendantRequest> listDefendantRequests = List.of(ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(prosecutionCaseId)
                .withDefendantOffences(List.of(offenceId))
                .withHearingLanguageNeeds(HearingLanguage.ENGLISH)
                .withReferralReason(ReferralReason.referralReason().withDefendantId(defendantId)
                        .withDescription("not guilty for pcnr").build())
                .build());
        final ReferredListHearingRequest listHearingRequest = ReferredListHearingRequest.referredListHearingRequest()
                .withListDefendantRequests(listDefendantRequests)
                .build();
        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transformSjpReferralNextHearing(envelopeReferral, List.of(getProsecutionCase()), UUID.randomUUID(), nextHearing, List.of(listHearingRequest));

        final HearingListingNeeds hearingListingNeeds = listCourtHearing.getHearings().get(0);
        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(hearingListingNeeds.getEstimatedMinutes(), is(nextHearing.getEstimatedMinutes()));
        assertThat(hearingListingNeeds.getListedStartDateTime(), is(nextHearing.getListedStartDateTime()));
        assertThat(hearingListingNeeds.getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(hearingListingNeeds.getJurisdictionType(), is(MAGISTRATES));

        assertThat(hearingListingNeeds.getType().getId(), is(nextHearing.getType().getId()));
        assertThat(hearingListingNeeds.getType().getDescription(), is(nextHearing.getType().getDescription()));

        assertThat(hearingListingNeeds.getCourtCentre().getId(), is(courtCentre.getId()));
        assertThat(hearingListingNeeds.getCourtCentre().getName(), is(courtCentre.getName()));
        assertThat(hearingListingNeeds.getCourtCentre().getRoomName(), is(courtCentre.getRoomName()));
        assertThat(hearingListingNeeds.getCourtCentre().getAddress().getAddress1(), is(courtCentre.getAddress().getAddress1()));
        assertThat(hearingListingNeeds.getCourtCentre().getAddress().getPostcode(), is(courtCentre.getAddress().getPostcode()));

        assertThat(hearingListingNeeds.getDefendantListingNeeds().get(0).getHearingLanguageNeeds(), is(HearingLanguage.ENGLISH));

        final RotaSlot actualRotaSlot = nextHearing.getHmiSlots().get(0);
        final RotaSlot transformedRotaSlot = hearingListingNeeds.getBookedSlots().get(0);
        assertThat(transformedRotaSlot.getRoomId(), is(actualRotaSlot.getRoomId()));
        assertThat(transformedRotaSlot.getCourtCentreId(), is(actualRotaSlot.getCourtCentreId()));
        assertThat(transformedRotaSlot.getOucode(), is(actualRotaSlot.getOucode()));
        assertThat(transformedRotaSlot.getSession(), is(actualRotaSlot.getSession()));
        assertThat(transformedRotaSlot.getStartTime(), is(actualRotaSlot.getStartTime()));
        assertThat(transformedRotaSlot.getCourtRoomId(), is(actualRotaSlot.getCourtRoomId()));
    }

    @Test
    void shouldTransformToListCourtHearingWithReferralReason() {

        final List<CourtHearingRequest> courtHearingRequests = getCourtHearingRequest();

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral, List.of(getProsecutionCase()), courtHearingRequests.get(0), UUID.randomUUID());

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEarliestStartDateTime().toString(), is(earliestStartDateTime.toString()));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        validateDefendant(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is(postcode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getCpsOrganisation(), is(cpsOrganisation));
        assertThat(listCourtHearing.getHearings().get(0).getBookedSlots().get(0)
                .getRoomId(), is("roomId1"));
        assertThat(listCourtHearing.getHearings().get(0).getBookingType(), is("Video"));
        assertThat(listCourtHearing.getHearings().get(0).getPriority(), is("High"));
        assertThat(listCourtHearing.getHearings().get(0).getSpecialRequirements().size(), is(2));
        assertThat(listCourtHearing.getHearings().get(0).getSpecialRequirements(), hasItems("RSZ", "CELL"));
        assertThat(listCourtHearing.getHearings().get(0).getWeekCommencingDate().getStartDate(), is(LocalDate.now()));
        assertThat(listCourtHearing.getHearings().get(0).getWeekCommencingDate().getDuration(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getNonDefaultDays().size(), is(1));
    }

    @Test
    void shouldTransformToListCourtHearingWhenNullPostCode() {

        // Given
        final SjpCourtReferral courtReferral = getCourtReferralWithPostCode(null);
        final JsonEnvelope envelopeReferral = createReferralEnvelope();

        final JsonObject hearingDescription = Json.createObjectBuilder().add("hearingDescription", "British").build();

        final JsonObject payload = createPayloadForOrgUnits(randomUUID().toString());

        when(referenceDataService.getHearingType(any(), any(UUID.class), any())).thenReturn(Optional.of(hearingDescription));
        when(referenceDataService.getCourtCentre("B01LY00", envelopeReferral,requester))
                .thenReturn(CourtCentre.courtCentre()
                        .withId(UUID.fromString(((JsonObject) payload.getJsonArray("organisationunits").get(0)).getString("id")))
                        .withName("South Western (Lavender Hill)")
                        .withWelshName("welshName_Test").build());

        when(referenceDataService.getReferralReasonByReferralReasonId(any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("reason", "reason for referral").build()));

        //When
        final ListCourtHearing actual = listCourtHearingTransformer
                .transform(envelopeReferral, List.of(getProsecutionCaseWithPostCode(null)), courtReferral.getSjpReferral(), courtReferral.getListHearingRequests(), UUID.randomUUID());

        assertThat(actual.getHearings(), hasSize(1));
        HearingListingNeeds hearing = actual.getHearings().get(0);

        //Then
        assertThat(hearing.getCourtCentre().getId(), is(UUID.fromString(((JsonObject) payload.getJsonArray("organisationunits").get(0)).getString("id"))));
        assertThat(hearing.getEstimatedMinutes(), is(estimateMinutes));
        assertThat(hearing.getEarliestStartDateTime().toLocalDate().toString(), is(expectedDate));
        assertThat(hearing.getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        validateDefendant(hearing.getProsecutionCases().get(0).getDefendants().get(0));
        assertThat(hearing.getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), nullValue());
        assertThat(hearing.getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(hearing.getCourtCentre().getName(), is("South Western (Lavender Hill)"));

    }

    @Test
    void shouldTransformToListCourtHearingWhenBlankPostCode() {

        //Given
        final SjpCourtReferral courtReferral = getCourtReferralWithPostCode("");
        final JsonEnvelope envelopeReferral = createReferralEnvelope();

        final JsonObject jsonObject = Json.createObjectBuilder().add("hearingDescription", "British").build();

        final JsonObject payload = createPayloadForOrgUnits(randomUUID().toString());

        when(referenceDataService.getHearingType(any(), any(UUID.class), any())).thenReturn(Optional.of(jsonObject));
        when(referenceDataService.getCourtsByPostCodeAndProsecutingAuthority(any(), any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder()
                        .add("courts", createArrayBuilder()
                                .add(Json.createObjectBuilder().add("oucode", "Redditch").add("oucodeL3Code", "B22KS00").build())
                                .build())
                        .build()));
        when(referenceDataService.getCourtCentre("Redditch", envelopeReferral,requester))
                .thenReturn(CourtCentre.courtCentre()
                        .withId(UUID.fromString(((JsonObject) payload.getJsonArray("organisationunits").get(0)).getString("id")))
                        .withName("South Western (Lavender Hill)")
                        .withWelshName("welshName_Test").build());

        when(referenceDataService.getReferralReasonByReferralReasonId(any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("reason", "reason for referral").build()));

        final ListCourtHearing actual = listCourtHearingTransformer
                .transform(envelopeReferral, List.of(getProsecutionCaseWithPostCode("")), courtReferral.getSjpReferral(), courtReferral.getListHearingRequests(), UUID.randomUUID());

        assertThat(actual.getHearings(), hasSize(1));
        HearingListingNeeds hearing = actual.getHearings().get(0);

        //Then
        assertThat(hearing.getCourtCentre().getId(), is(UUID.fromString(((JsonObject) payload.getJsonArray("organisationunits").get(0)).getString("id"))));
        assertThat(hearing.getEstimatedMinutes(), is(estimateMinutes));
        assertThat(hearing.getEarliestStartDateTime().toLocalDate().toString(), is(expectedDate));
        assertThat(hearing.getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        validateDefendant(hearing.getProsecutionCases().get(0).getDefendants().get(0));
        assertThat(hearing.getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is(""));
        assertThat(hearing.getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(hearing.getCourtCentre().getName(), is("South Western (Lavender Hill)"));

    }

    @Test
    void shouldTransformToListCourtHearingWhenInvalidPostCode() {

        //Given
        final SjpCourtReferral courtReferral = getCourtReferralWithPostCode("xxxxxxxxx");
        final JsonEnvelope envelopeReferral = createReferralEnvelope();

        final JsonObject jsonObject = Json.createObjectBuilder().add("hearingDescription", "British").build();
        final JsonObject payload = createPayloadForOrgUnits(randomUUID().toString());
        when(referenceDataService.getHearingType(any(), any(UUID.class), any())).thenReturn(Optional.of(jsonObject));
        when(referenceDataService.getCourtsByPostCodeAndProsecutingAuthority(any(), any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder()
                        .add("courts", createArrayBuilder()
                                .add(Json.createObjectBuilder().add("oucode", "Redditch").add("oucodeL3Code", "B22KS00").build())
                                .build())
                        .build()));
        when(referenceDataService.getCourtCentre("Redditch", envelopeReferral,requester))
                .thenReturn(CourtCentre.courtCentre()
                        .withId(UUID.fromString(((JsonObject) payload.getJsonArray("organisationunits").get(0)).getString("id")))
                        .withName("South Western (Lavender Hill)")
                        .withWelshName("welshName_Test").build());

        when(referenceDataService.getReferralReasonByReferralReasonId(any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("reason", "reason for referral").build()));

        //When
        final ListCourtHearing actual = listCourtHearingTransformer
                .transform(envelopeReferral, List.of(getProsecutionCaseWithPostCode("xxxxxxxxx")), courtReferral.getSjpReferral(), courtReferral.getListHearingRequests(), UUID.randomUUID());

        assertThat(actual.getHearings(), hasSize(1));
        HearingListingNeeds hearing = actual.getHearings().get(0);

        //Then
        assertThat(hearing.getCourtCentre().getId(), is(UUID.fromString(((JsonObject) payload.getJsonArray("organisationunits").get(0)).getString("id"))));
        assertThat(hearing.getEstimatedMinutes(), is(estimateMinutes));
        assertThat(hearing.getEarliestStartDateTime().toLocalDate().toString(), is(expectedDate));
        assertThat(hearing.getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        validateDefendant(hearing.getProsecutionCases().get(0).getDefendants().get(0));
        assertThat(hearing.getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is("xxxxxxxxx"));
        assertThat(hearing.getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(hearing.getCourtCentre().getName(), is("South Western (Lavender Hill)"));

    }

    void validateDefendant(final Defendant defendant) {
        assertThat(defendant.getId(), is(defendantId));
        assertThat(defendant.getMasterDefendantId(), is(masterDefendantId));
        assertThat(defendant.getCourtProceedingsInitiated(),
                is(courtProceedingsInitiated));
        assertThat(defendant
                .getOffences().get(0).getId(), is(offenceId));
    }

    @Test
    void shouldTransformToListCourtHearingWithLegalEntityDefendant() {

        final SjpCourtReferral courtReferral = getCourtReferralWithLegalDefendant();
        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final JsonObject jsonObject = Json.createObjectBuilder().add("hearingDescription", "British").build();

        when(referenceDataService.getHearingType(any(), any(UUID.class), any())).thenReturn(Optional.of(jsonObject));
        when(referenceDataService.getReferralReasonByReferralReasonId(any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("reason", "reason for referral").build()));

        when(referenceDataService.getCourtsByPostCodeAndProsecutingAuthority(any(), any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder()
                        .add("courts", createArrayBuilder()
                                .add(Json.createObjectBuilder().add("oucode", "Redditch").add("oucodeL3Code", "B22KS00").build())
                                .build())
                        .build()));
        when(referenceDataService.getCourtCentre("Redditch", envelopeReferral,requester))
                .thenReturn(CourtCentre.courtCentre()
                        .withId(courtCenterId)
                        .withName("South Western (Lavender Hill)")
                        .withWelshName("welshName_Test").build());

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral, List.of(getProsecutionCaseWithLegalDefendantEntity()), courtReferral.getSjpReferral(), courtReferral.getListHearingRequests(), UUID.randomUUID());

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
        assertThat(listCourtHearing.getHearings().get(0).getEarliestStartDateTime().toLocalDate().minusDays(14).toString(), is(referralDate));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        validateDefendant(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0));

        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getLegalEntityDefendant().getOrganisation().getAddress().getPostcode(), is(postcode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getOffences().get(0).getId(), is(offenceId));

    }

    @Test
    void shouldTransformApplicationToListCourtHearing() {

        final ApplicationReferredToCourt applicationReferredToCourt = ApplicationReferredToCourt.applicationReferredToCourt()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withId(UUID.randomUUID())
                        .withCourtCentre(createCourtCenter())
                        .withCourtApplications(createCourtApplications())
                        .withEstimatedDuration(estimateDuration)
                        .withEstimatedMinutes(estimateMinutes)
                        .withJudiciary(List.of(JudicialRole.judicialRole()
                                .withJudicialId(UUID.randomUUID())
                                .build()))
                        .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                                .withId(UUID.randomUUID())
                                .build()))
                        .withType(HearingType.hearingType()
                                .withId(UUID.randomUUID())
                                .withDescription("SENTENCING")
                                .build())
                        .withBookedSlots(createRotaSlot())
                        .build())
                .build();

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer.transform(applicationReferredToCourt);

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
    }

//    @Test
//    void shouldCalculateEarliestHearingDate() {
//        //When noticeDate and referralDate is same date
//        assertThat(listCourtHearingTransformer.calculateEarliestHearingDate(parse("2018-01-01"), parse("2018-01-01")), is(parse("2018-01-01").plusDays(28)));
//        // When referralDate is greater
//        assertThat(listCourtHearingTransformer.calculateEarliestHearingDate(parse("2018-01-01"), parse("2018-01-20")), is(parse("2018-01-20").plusDays(14)));
//        // When referralDate is greater but not after noticeDate+28
//        assertThat(listCourtHearingTransformer.calculateEarliestHearingDate(parse("2018-01-01"), parse("2018-01-10")), is(parse("2018-01-01").plusDays(28)));
//
//    }

    @Test
    void shouldTransformSPICaseToListCourtHearing() {
        //given
        final List<ListHearingRequest> listHearingRequest = getListHearingRequest(false);

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral, List.of(getProsecutionCase()), listHearingRequest, UUID.randomUUID(), null);

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
        assertThat(listCourtHearing.getHearings().get(0).getReportingRestrictionReason(), is(AUTOMATIC_ANONYMITY));
        assertThat(listCourtHearing.getHearings().get(0).getEarliestStartDateTime().toString(), is(earliestStartDateTime.toString()));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getCpsOrganisation(), is(cpsOrganisation));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is(postcode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getOffences().get(0).getId(), is(offenceId));
        assertThat(listCourtHearing.getHearings().get(0).getDefendantListingNeeds().get(0).getDefendantId(), is(defendantId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getCaseMarkers()
                .get(0).getMarkerTypeCode(), is(MARKER_TYPE_CODE));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getCaseMarkers()
                .get(0).getMarkerTypeDescription(), is(MARKER_TYPE_DESCRIPTION));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getCaseMarkers()
                .get(0).getMarkerTypeid(), is(MARKER_TYPE_ID));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getTrialReceiptType(), is(TRANSFER));
        assertFalse(listCourtHearing.getHearings().get(0).getDefendantListingNeeds().get(0).getIsYouth());

    }

    @Test
    void shouldTransformSPICaseToListCourtHearingDefendantIsYouth() {
        //given
        final List<ListHearingRequest> listHearingRequest = getListHearingRequest(true);

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral, List.of(getProsecutionCase(LocalDate.now().minusYears(15))), listHearingRequest, UUID.randomUUID(), null);

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
        assertThat(listCourtHearing.getHearings().get(0).getReportingRestrictionReason(), is(AUTOMATIC_ANONYMITY));
        assertThat(listCourtHearing.getHearings().get(0).getListedStartDateTime().toString(), is(listedStartDateTime.toString()));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is(postcode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getOffences().get(0).getId(), is(offenceId));
        assertThat(listCourtHearing.getHearings().get(0).getDefendantListingNeeds().get(0).getDefendantId(), is(defendantId));
        assertTrue(listCourtHearing.getHearings().get(0).getDefendantListingNeeds().get(0).getIsYouth());

    }

    @Test
    void shouldTransformSPICaseToListCourtHearingDefendantIsYouthWithWeekCommencingDate() {
        //given
        final List<ListHearingRequest> listHearingRequest = getListHearingRequestWithWeekCommencingDate();

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral, List.of(getProsecutionCase(LocalDate.now().minusYears(15))), listHearingRequest, UUID.randomUUID(), null);

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
        assertThat(listCourtHearing.getHearings().get(0).getReportingRestrictionReason(), is(AUTOMATIC_ANONYMITY));
        assertThat(listCourtHearing.getHearings().get(0).getWeekCommencingDate().getStartDate(), is(LocalDate.now(UTC)));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is(postcode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getOffences().get(0).getId(), is(offenceId));
        assertThat(listCourtHearing.getHearings().get(0).getDefendantListingNeeds().get(0).getDefendantId(), is(defendantId));
        assertTrue(listCourtHearing.getHearings().get(0).getDefendantListingNeeds().get(0).getIsYouth());

    }

    @Test
    void shouldCalculateExpectedStartDate() {
        final LocalDate now = LocalDate.now();
        final CourtHearingRequest courtHearingRequest = CourtHearingRequest.
                courtHearingRequest().
                withWeekCommencingDate(
                        WeekCommencingDate.
                                weekCommencingDate().
                                withStartDate(now).
                                build()
                ).
                build();

        final ZonedDateTime actual = listCourtHearingTransformer.calculateExpectedStartDate(courtHearingRequest);

        assertThat(actual, is(now.atStartOfDay(UTC)));
    }

    @Test
    void shouldTransformToListCourtHearingWithReferralReasonAndProsecutor() {

        final List<CourtHearingRequest> courtHearingRequests = getCourtHearingRequest();

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral, List.of(getProsecutionCaseWithProsecutor()), courtHearingRequests.get(0), UUID.randomUUID());

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEarliestStartDateTime().toString(), is(earliestStartDateTime.toString()));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        validateDefendant(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is(postcode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getCpsOrganisation(), is(cpsOrganisation));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutor().getProsecutorId(), is(prosecutorId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutor().getProsecutorCode(), is(prosecutorCode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getCpsOrganisation(), is(cpsOrganisation));

        assertThat(listCourtHearing.getHearings().get(0).getBookedSlots().get(0)
                .getRoomId(), is("roomId1"));
        assertThat(listCourtHearing.getHearings().get(0).getBookingType(), is("Video"));
        assertThat(listCourtHearing.getHearings().get(0).getPriority(), is("High"));
        assertThat(listCourtHearing.getHearings().get(0).getSpecialRequirements().size(), is(2));
        assertThat(listCourtHearing.getHearings().get(0).getSpecialRequirements(), hasItems("RSZ", "CELL"));
        assertThat(listCourtHearing.getHearings().get(0).getWeekCommencingDate().getStartDate(), is(LocalDate.now()));
        assertThat(listCourtHearing.getHearings().get(0).getWeekCommencingDate().getDuration(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getNonDefaultDays().size(), is(1));
    }

    @Test
    void shouldTransformToListCourtHearingForMultiHearing() {

        //given
        final String key = "key";

        final List<ListHearingRequest> listHearingRequest = getListHearingRequest(true);

        final List<HearingListing> hearingListings = List.of(new HearingListing(randomUUID(), key,
                listHearingRequest,
                List.of(ListDefendantRequest.listDefendantRequest().build())));

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral,
                        List.of(getProsecutionCase(LocalDate.now().minusYears(15))),
                        hearingListings, false);

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
        assertThat(listCourtHearing.getHearings().get(0).getReportingRestrictionReason(), is(AUTOMATIC_ANONYMITY));
        assertThat(listCourtHearing.getHearings().get(0).getListedStartDateTime().toString(), is(listedStartDateTime.toString()));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is(postcode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getOffences().get(0).getId(), is(offenceId));
        assertThat(listCourtHearing.getHearings().get(0).getDefendantListingNeeds().get(0).getDefendantId(), is(defendantId));
        assertTrue(listCourtHearing.getHearings().get(0).getDefendantListingNeeds().get(0).getIsYouth());
    }

    @Test
    void shouldTransformToListCourtHearingWhenNoHearingExist() {

        final List<HearingListing> hearingListings = List.of();

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral,
                        List.of(getProsecutionCase(LocalDate.now().minusYears(15))),
                        hearingListings, false);

        assertThat(listCourtHearing.getHearings().size(), is(0));
    }

    @Test
    void shouldTransformToListCourtHearingForMultiHearingWithWeekCommencingDate() {

        //given
        final String key = "key";

        final List<ListHearingRequest> listHearingRequest = getListHearingRequestWithWeekCommencingDate();

        final List<HearingListing> hearingListings = List.of(new HearingListing(randomUUID(), key,
                listHearingRequest,
                List.of(ListDefendantRequest.listDefendantRequest().build())));

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral,
                        List.of(getProsecutionCase(LocalDate.now().minusYears(15))),
                        hearingListings, false);

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
        assertThat(listCourtHearing.getHearings().get(0).getReportingRestrictionReason(), is(AUTOMATIC_ANONYMITY));
        assertThat(listCourtHearing.getHearings().get(0).getWeekCommencingDate().getStartDate(), is(LocalDate.now(UTC)));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is(postcode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getOffences().get(0).getId(), is(offenceId));
        assertThat(listCourtHearing.getHearings().get(0).getDefendantListingNeeds().get(0).getDefendantId(), is(defendantId));
        assertTrue(listCourtHearing.getHearings().get(0).getDefendantListingNeeds().get(0).getIsYouth());
    }

    private List<CourtApplication> createCourtApplications() {
        final List<CourtApplication> courtApplications = new ArrayList<>();
        courtApplications.add(CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withCourtApplicationCases(
                        singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(randomUUID()).build()))
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withId(UUID.randomUUID())
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withMasterDefendantId(UUID.randomUUID())
                                .build())
                        .build())
                .withRespondents(List.of(CourtApplicationParty.courtApplicationParty()
                        .withId(UUID.randomUUID())
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(UUID.randomUUID())

                                .build())
                        .build()))
                .build());
        return courtApplications;
    }

    private CourtCentre createCourtCenter() {
        return CourtCentre.courtCentre()
                .withId(courtCenterId)
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

    private ProsecutionCase getProsecutionCase() {
        return getProsecutionCase(null);
    }

    private ProsecutionCase getProsecutionCaseWithProsecutor() {
        final Prosecutor prosecutor = Prosecutor.prosecutor().withProsecutorId(prosecutorId)
                .withProsecutorCode(prosecutorCode).build();
        return ProsecutionCase.prosecutionCase().withValuesFrom(getProsecutionCase(null)).withProsecutor(prosecutor).build();
    }

    private ProsecutionCase getProsecutionCase(final LocalDate birthDate) {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withCpsOrganisation(cpsOrganisation)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(prosecutingAuth).build())
                .withDefendants(List.of(Defendant.defendant()
                        .withId(defendantId)
                        .withMasterDefendantId(masterDefendantId)
                        .withCourtProceedingsInitiated(courtProceedingsInitiated)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address().withPostcode(postcode).build())
                                        .withDateOfBirth(birthDate)
                                        .build()).build())
                        .withOffences(List.of(Offence.offence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .withCaseMarkers(List.of(Marker.marker()
                        .withMarkerTypeid(MARKER_TYPE_ID)
                        .withMarkerTypeCode(MARKER_TYPE_CODE)
                        .withMarkerTypeDescription(MARKER_TYPE_DESCRIPTION)
                        .build()))
                .withTrialReceiptType(TRANSFER)
                .build();
    }

    private ProsecutionCase getProsecutionCaseWithPostCode(final String postCode) {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(prosecutingAuth).build())
                .withDefendants(List.of(Defendant.defendant()
                        .withId(defendantId)
                        .withMasterDefendantId(masterDefendantId)
                        .withCourtProceedingsInitiated(courtProceedingsInitiated)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address().withPostcode(postCode).build())
                                        .withDateOfBirth(null)
                                        .build()).build())
                        .withOffences(List.of(Offence.offence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .withCaseMarkers(List.of(Marker.marker()
                        .withMarkerTypeid(MARKER_TYPE_ID)
                        .withMarkerTypeCode(MARKER_TYPE_CODE)
                        .withMarkerTypeDescription(MARKER_TYPE_DESCRIPTION)
                        .build()))
                .build();
    }

    private ProsecutionCase getProsecutionCaseWithLegalDefendantEntity() {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(prosecutingAuth).build())
                .withDefendants(List.of(Defendant.defendant()
                        .withId(defendantId)
                        .withMasterDefendantId(masterDefendantId)
                        .withCourtProceedingsInitiated(courtProceedingsInitiated)
                        .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                .withOrganisation(Organisation.organisation()
                                        .withAddress(Address.address()
                                                .withPostcode(postcode).build()).build()).build())
                        .withOffences(List.of(Offence.offence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();
    }

    private SjpCourtReferral getCourtReferral() {
        final SjpReferral sjpReferral = SjpReferral.sjpReferral()
                .withNoticeDate(LocalDate.of(2018, Month.JANUARY, 1))
                .withReferralDate(LocalDate.of(2018, Month.FEBRUARY, 15)).build();

        final ReferredListHearingRequest listHearingRequest = ReferredListHearingRequest.referredListHearingRequest()
                .withHearingType(ReferredHearingType.referredHearingType().withId(UUID.randomUUID()).build())
                .withEstimateMinutes(15)
                .withEstimatedDuration("1 week")
                .withListDefendantRequests(List.of(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(List.of(offenceId))
                        .withReferralReason(ReferralReason.referralReason().withDefendantId(defendantId)
                                .withDescription("not guilty for pcnr").build())
                        .build()))
                .build();

        final ReferredProsecutionCase referredProsecutionCase = ReferredProsecutionCase.referredProsecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(prosecutingAuth).build())
                .withDefendants(List.of(ReferredDefendant.referredDefendant()
                        .withId(defendantId)
                        .withPersonDefendant(ReferredPersonDefendant.referredPersonDefendant()
                                .withPersonDetails(ReferredPerson.referredPerson()
                                        .withAddress(Address.address().withPostcode(postcode).build()).build()).build())
                        .withOffences(List.of(ReferredOffence.referredOffence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();



        return SjpCourtReferral.sjpCourtReferral()
                .withSjpReferral(sjpReferral)
                .withProsecutionCases(List.of(referredProsecutionCase))
                .withListHearingRequests(List.of(listHearingRequest)).build();
    }

    private SjpCourtReferral getCourtReferralWithPostCode(String postcode) {
        final SjpReferral sjpReferral = SjpReferral.sjpReferral()
                .withNoticeDate(LocalDate.of(2018, Month.JANUARY, 1))
                .withReferralDate(LocalDate.of(2018, Month.FEBRUARY, 15))
                .withReferringJudicialDecision(ReferringJudicialDecision.referringJudicialDecision()
                        .withCourtHouseCode("B01LY00")
                        .withLocation("Referring Court Location")
                        .build())
                .build();

        final ReferredListHearingRequest listHearingRequest = ReferredListHearingRequest.referredListHearingRequest()
                .withHearingType(ReferredHearingType.referredHearingType().withId(UUID.randomUUID()).build())
                .withEstimateMinutes(15)
                .withEstimatedDuration("1 week")
                .withListDefendantRequests(List.of(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(List.of(offenceId))
                        .withReferralReason(ReferralReason.referralReason().withDefendantId(defendantId)
                                .withDescription("not guilty for pcnr").build())
                        .build()))
                .build();

        final ReferredProsecutionCase referredProsecutionCase = ReferredProsecutionCase.referredProsecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(prosecutingAuth).build())
                .withDefendants(List.of(ReferredDefendant.referredDefendant()
                        .withId(defendantId)
                        .withPersonDefendant(ReferredPersonDefendant.referredPersonDefendant()
                                .withPersonDetails(ReferredPerson.referredPerson()
                                        .withAddress(Address.address().withPostcode(postcode).build()).build()).build())
                        .withOffences(List.of(ReferredOffence.referredOffence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();



        return SjpCourtReferral.sjpCourtReferral()
                .withSjpReferral(sjpReferral)
                .withProsecutionCases(List.of(referredProsecutionCase))
                .withListHearingRequests(List.of(listHearingRequest)).build();
    }

    private SjpCourtReferral getCourtReferralWithLegalDefendant() {
        final SjpReferral sjpReferral = SjpReferral.sjpReferral()
                .withNoticeDate(LocalDate.of(2018, Month.JANUARY, 1))
                .withReferralDate(LocalDate.of(2018, Month.FEBRUARY, 15)).build();

        final ReferredListHearingRequest listHearingRequest = ReferredListHearingRequest.referredListHearingRequest()
                .withHearingType(ReferredHearingType.referredHearingType().withId(UUID.randomUUID()).build())
                .withEstimateMinutes(15)
                .withEstimatedDuration("1 week")
                .withListDefendantRequests(List.of(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(List.of(offenceId))
                        .withReferralReason(ReferralReason.referralReason().withDefendantId(defendantId)
                                .withDescription("not guilty for pcnr").build())
                        .build()))
                .build();

        final ReferredProsecutionCase referredProsecutionCase = ReferredProsecutionCase.referredProsecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(prosecutingAuth).build())
                .withDefendants(List.of(ReferredDefendant.referredDefendant()
                        .withId(defendantId)
                        .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                .withOrganisation(Organisation.organisation()
                                        .withAddress(Address.address()
                                                .withPostcode(postcode)
                                                .build())
                                        .build())
                                .build())
                        .withOffences(List.of(ReferredOffence.referredOffence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();

        return SjpCourtReferral.sjpCourtReferral()
                .withSjpReferral(sjpReferral)
                .withProsecutionCases(List.of(referredProsecutionCase))
                .withListHearingRequests(List.of(listHearingRequest)).build();
    }

    private List<ListHearingRequest> getListHearingRequest(final boolean isListedStartDateTimePresent) {
        //Either EarliestStartDateTime or ListedStartDateTime can be present. Not both.
        return List.of(ListHearingRequest.listHearingRequest()
                .withCourtCentre(createCourtCenter())
                .withEarliestStartDateTime(!isListedStartDateTimePresent ? earliestStartDateTime : null)
                .withEstimateMinutes(15)
                .withHearingType(HearingType.hearingType().withId(UUID.randomUUID()).build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListDefendantRequests(List.of(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(List.of(offenceId))
                        .withDefendantId(defendantId)
                        .build()))
                .withListedStartDateTime(isListedStartDateTimePresent ? listedStartDateTime : null)
                .withListingDirections("wheelchair access required")
                .withProsecutorDatesToAvoid("Thursdays")
                .withReportingRestrictionReason(AUTOMATIC_ANONYMITY)
                .build());
    }

    private List<ListHearingRequest> getListHearingRequestWithWeekCommencingDate() {
        //Either EarliestStartDateTime or ListedStartDateTime can be present. Not both.
        return List.of(ListHearingRequest.listHearingRequest()
                .withCourtCentre(createCourtCenter())
//                .withEarliestStartDateTime(!isListedStartDateTimePresent ? earliestStartDateTime : null)
                .withEstimateMinutes(15)
                .withHearingType(HearingType.hearingType().withId(randomUUID()).build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListDefendantRequests(List.of(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(List.of(offenceId))
                        .withDefendantId(defendantId)
                        .build()))
//                .withListedStartDateTime(isListedStartDateTimePresent ? listedStartDateTime : null)
                .withListingDirections("wheelchair access required")
                .withProsecutorDatesToAvoid("Thursdays")
                .withReportingRestrictionReason(AUTOMATIC_ANONYMITY)
                        .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate().withStartDate(LocalDate.now(UTC)).build())
                .build());
    }

    private List<CourtHearingRequest> getCourtHearingRequest() {
        final uk.gov.justice.core.courts.NonDefaultDay nonDefaultDay = uk.gov.justice.core.courts.NonDefaultDay.nonDefaultDay()
                .withDuration(1)
                .withStartTime(ZonedDateTime.now())
                .withCourtCentreId("courtCentreId")
                .withCourtRoomId(1)
                .withCourtScheduleId("courtScheduleId")
                .withOucode("oucode")
                .withSession("PM")
                .withRoomId("roomId")
                .build();
        final List<uk.gov.justice.core.courts.NonDefaultDay> nonDefaultDays = new ArrayList<>();
        nonDefaultDays.add(nonDefaultDay);

        //Either EarliestStartDateTime or ListedStartDateTime can be present. Not both.
        return List.of(CourtHearingRequest.courtHearingRequest()
                .withCourtCentre(createCourtCenter())
                .withEarliestStartDateTime( earliestStartDateTime )
                .withHearingType(HearingType.hearingType().withId(randomUUID()).build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListDefendantRequests(List.of(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(defendantId)
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(List.of(offenceId))
                        .build()))
                .withListedStartDateTime(listedStartDateTime)
                .withListingDirections("wheelchair access required")
                .withBookedSlots(singletonList(RotaSlot.rotaSlot().withRoomId("roomId1").build()))
                .withBookingType("Video")
                .withPriority("High")
                .withSpecialRequirements(List.of("RSZ", "CELL"))
                .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                        .withStartDate(LocalDate.now())
                        .withDuration(1)
                        .build())
                .withNonDefaultDays(nonDefaultDays)
                .build());
    }

    private JsonObject createPayloadForOrgUnits(final String id) {
        return Json.createObjectBuilder()
                .add("organisationunits", createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", id)
                                .add("oucodeL3Name", "South Western (Lavender Hill)")
                                .add("oucodeL3WelshName", "welshName_Test")
                                .build())
                        .build())
                .build();
    }

    private JsonEnvelope createReferralEnvelope() {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());
    }

    private List<RotaSlot> createRotaSlot() {
        final RotaSlot slot1 = RotaSlot.rotaSlot()
                .withCourtCentreId(UUID.randomUUID().toString())
                .withRoomId(UUID.randomUUID().toString())
                .build();

        final RotaSlot slot2 = RotaSlot.rotaSlot()
                .withCourtCentreId(UUID.randomUUID().toString())
                .withRoomId(UUID.randomUUID().toString())
                .build();

        return List.of(slot1, slot2);
    }

    private NextHearing createNextHearing(){
        return new NextHearing.Builder()
                .withHmiSlots(List.of(createHmiSlot()))
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("TRT")
                        .build())
                .withListedStartDateTime(now())
                .withEstimatedMinutes(10)
                .build();
    }

    private RotaSlot createHmiSlot(){
        return new RotaSlot.Builder()
                        .withRoomId(randomUUID().toString())
                        .withCourtCentreId(randomUUID().toString())
                        .withCourtRoomId(12)
                        .withOucode("MAGOX")
                        .withSession("session")
                        .withCourtScheduleId(randomUUID().toString())
                        .withDuration(10)
                        .withStartTime(now())
                        .build();
    }

    private CourtCentre createCourtCentre(){
        return new CourtCentre.Builder()
                .withId(courtCenterId)
                .withName("Lavender Hill")
                .withAddress(Address.address()
                        .withAddress1("address1")
                        .withPostcode("CB2 5MN")
                        .build())
                .withRoomName("CourtRoom 04")
                .build();
    }

}
