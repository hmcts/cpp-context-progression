package uk.gov.moj.cpp.progression.transformer;

import static java.time.LocalDate.parse;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.ReferredDefendant;
import uk.gov.justice.core.courts.ReferredHearingType;
import uk.gov.justice.core.courts.ReferredListHearingRequest;
import uk.gov.justice.core.courts.ReferredOffence;
import uk.gov.justice.core.courts.ReferredPerson;
import uk.gov.justice.core.courts.ReferredPersonDefendant;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.justice.core.courts.SjpReferral;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class ListCourtHearingTransformerTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();
    final private String postcode = "CR11111";
    final private String prosecutingAuth = "CPS";
    final private UUID prosecutionCaseId = UUID.randomUUID();
    final private UUID defendantId = UUID.randomUUID();
    final private UUID offenceId = UUID.randomUUID();
    final private UUID courtCenterId = UUID.randomUUID();
    final private int estimateMinutes = 15;
    final private String referralDate = "2018-02-15";
    final private ZonedDateTime listedStartDateTime = ZonedDateTime.parse("2019-06-30T18:32:04.238Z");
    final private ZonedDateTime earliestStartDateTime = ZonedDateTime.parse("2019-05-30T18:32:04.238Z");
    private static final String AUTOMATIC_ANONYMITY = "Automatic anonymity";
    private static final UUID MARKER_TYPE_ID = UUID.randomUUID();
    private static final String MARKER_TYPE_CODE = "MarkerTypeCode";
    private static final String MARKER_TYPE_DESCRIPTION = "MarkerTypeDescription";

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @InjectMocks
    private ListCourtHearingTransformer listCourtHearingTransformer;
    @Mock
    private ReferenceDataService referenceDataService;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private Function<Object, JsonEnvelope> objectJsonEnvelopeFunction;


    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void shouldTransformToListCourtHearing() throws IOException {
        //given

        final SjpCourtReferral courtReferral = getCourtReferral();


        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final JsonObject jsonObject = Json.createObjectBuilder().add("hearingDescription", "British").build();

        when(referenceDataService.getHearingType(any(), any(), any())).thenReturn(Optional.of(jsonObject));
        when(referenceDataService.getCourtCentre(envelopeReferral, postcode, prosecutingAuth, requester))
                .thenReturn(CourtCentre.courtCentre().withId(courtCenterId).build());
        when(referenceDataService.getReferralReasonById(any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("reason", "reason for referral").build()));

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral, Arrays.asList(getProsecutionCase()), courtReferral.getSjpReferral(), courtReferral.getListHearingRequests(), UUID.randomUUID());

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
        assertThat(listCourtHearing.getHearings().get(0).getEarliestStartDateTime().toLocalDate().minusDays(14).toString(), is(referralDate));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getPersonDefendant().getPersonDetails().getAddress().getPostcode(), is(postcode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getOffences().get(0).getId(), is(offenceId));

    }

    @Test
    public void shouldTransformToListCourtHearingWithLegalEntityDefendant() throws IOException {
        //given

        final SjpCourtReferral courtReferral = getCourtReferralWithLegalDefendant();

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        final JsonObject jsonObject = Json.createObjectBuilder().add("hearingDescription", "British").build();

        when(referenceDataService.getHearingType(any(), any(), any())).thenReturn(Optional.of(jsonObject));
        when(referenceDataService.getCourtCentre(envelopeReferral, postcode, prosecutingAuth, requester))
                .thenReturn(CourtCentre.courtCentre().withId(courtCenterId).build());
        when(referenceDataService.getReferralReasonById(any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("reason", "reason for referral").build()));

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral, Arrays.asList(getProsecutionCaseWithLegalDefendantEntity()), courtReferral.getSjpReferral(), courtReferral.getListHearingRequests(), UUID.randomUUID());

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
        assertThat(listCourtHearing.getHearings().get(0).getEarliestStartDateTime().toLocalDate().minusDays(14).toString(), is(referralDate));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getLegalEntityDefendant().getOrganisation().getAddress().getPostcode(), is(postcode));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0)
                .getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), is(prosecutingAuth));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getDefendants().get(0)
                .getOffences().get(0).getId(), is(offenceId));

    }

    @Test
    public void shouldTransformApplicationToListCourtHearing() {

        final ApplicationReferredToCourt applicationReferredToCourt = ApplicationReferredToCourt.applicationReferredToCourt()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withId(UUID.randomUUID())
                        .withCourtCentre(createCourtCenter())
                        .withCourtApplications(createCourtApplications())
                        .withEstimatedMinutes(estimateMinutes)
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

                        .build())
                .build();

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer.transform(applicationReferredToCourt);

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
    }

    private List<CourtApplication> createCourtApplications() {
        final List<CourtApplication> courtApplications = new ArrayList<>();
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

    @Test
    public void shouldCalculateEarliestHearingDate() throws IOException {
        //When noticeDate and referralDate is same date
        assertThat(ListCourtHearingTransformer.calculateEarliestHearingDate(parse("2018-01-01"), parse("2018-01-01")), is(parse("2018-01-01").plusDays(28)));
        // When referralDate is greater
        assertThat(ListCourtHearingTransformer.calculateEarliestHearingDate(parse("2018-01-01"), parse("2018-01-20")), is(parse("2018-01-20").plusDays(14)));
        // When referralDate is greater but not after noticeDate+28
        assertThat(ListCourtHearingTransformer.calculateEarliestHearingDate(parse("2018-01-01"), parse("2018-01-10")), is(parse("2018-01-01").plusDays(28)));

    }

    private ProsecutionCase getProsecutionCase() {
        return getProsecutionCase(null);
    }

    private ProsecutionCase getProsecutionCase(final LocalDate birthDate) {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(prosecutingAuth).build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address().withPostcode(postcode).build())
                                        .withDateOfBirth(birthDate)
                                        .build()).build())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .withCaseMarkers(Arrays.asList(Marker.marker()
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
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId)
                        .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                .withOrganisation(Organisation.organisation()
                                        .withAddress(Address.address()
                                                .withPostcode(postcode).build()).build()).build())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();
    }

    private SjpCourtReferral getCourtReferral() {
        final SjpReferral sjpReferral = SjpReferral.sjpReferral()
                .withNoticeDate(LocalDate.of(2018, 01, 01))
                .withReferralDate(LocalDate.of(2018, 02, 15)).build();

        final ReferredListHearingRequest listHearingRequest = ReferredListHearingRequest.referredListHearingRequest()
                .withHearingType(ReferredHearingType.referredHearingType().withId(UUID.randomUUID()).build())
                .withEstimateMinutes(Integer.valueOf(15))
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(Arrays.asList(offenceId))
                        .withReferralReason(ReferralReason.referralReason().withDefendantId(defendantId)
                                .withDescription("not guilty for pcnr").build())
                        .build()))
                .build();

        final ReferredProsecutionCase referredProsecutionCase = ReferredProsecutionCase.referredProsecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(prosecutingAuth).build())
                .withDefendants(Arrays.asList(ReferredDefendant.referredDefendant()
                        .withId(defendantId)
                        .withPersonDefendant(ReferredPersonDefendant.referredPersonDefendant()
                                .withPersonDetails(ReferredPerson.referredPerson()
                                        .withAddress(Address.address().withPostcode(postcode).build()).build()).build())
                        .withOffences(Arrays.asList(ReferredOffence.referredOffence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();

        return SjpCourtReferral.sjpCourtReferral()
                .withSjpReferral(sjpReferral)
                .withProsecutionCases(Arrays.asList(referredProsecutionCase))
                .withListHearingRequests(Arrays.asList(listHearingRequest)).build();
    }

    private SjpCourtReferral getCourtReferralWithLegalDefendant() {
        final SjpReferral sjpReferral = SjpReferral.sjpReferral()
                .withNoticeDate(LocalDate.of(2018, 01, 01))
                .withReferralDate(LocalDate.of(2018, 02, 15)).build();

        final ReferredListHearingRequest listHearingRequest = ReferredListHearingRequest.referredListHearingRequest()
                .withHearingType(ReferredHearingType.referredHearingType().withId(UUID.randomUUID()).build())
                .withEstimateMinutes(Integer.valueOf(15))
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(Arrays.asList(offenceId))
                        .withReferralReason(ReferralReason.referralReason().withDefendantId(defendantId)
                                .withDescription("not guilty for pcnr").build())
                        .build()))
                .build();

        final ReferredProsecutionCase referredProsecutionCase = ReferredProsecutionCase.referredProsecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(prosecutingAuth).build())
                .withDefendants(Arrays.asList(ReferredDefendant.referredDefendant()
                        .withId(defendantId)
                        .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                .withOrganisation(Organisation.organisation()
                                        .withAddress(Address.address()
                                                .withPostcode(postcode)
                                                .build())
                                        .build())
                                .build())
                        .withOffences(Arrays.asList(ReferredOffence.referredOffence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();

        return SjpCourtReferral.sjpCourtReferral()
                .withSjpReferral(sjpReferral)
                .withProsecutionCases(Arrays.asList(referredProsecutionCase))
                .withListHearingRequests(Arrays.asList(listHearingRequest)).build();
    }

    @Test
    public void shouldTransformSPICaseToListCourtHearing() throws IOException {
        //given
        final List<ListHearingRequest> listHearingRequest = getListHearingRequest(false);

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        when(referenceDataService.getReferralReasonById(any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("reason", "reason for referral").build()));

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral, Arrays.asList(getProsecutionCase()), listHearingRequest, UUID.randomUUID());

        assertThat(listCourtHearing.getHearings().size(), is(1));
        assertThat(listCourtHearing.getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
        assertThat(listCourtHearing.getHearings().get(0).getEstimatedMinutes(), is(estimateMinutes));
        assertThat(listCourtHearing.getHearings().get(0).getReportingRestrictionReason(), is(AUTOMATIC_ANONYMITY));
        assertThat(listCourtHearing.getHearings().get(0).getEarliestStartDateTime().toString(), is(earliestStartDateTime.toString()));
        assertThat(listCourtHearing.getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));
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
        assertFalse(listCourtHearing.getHearings().get(0).getDefendantListingNeeds().get(0).getIsYouth());

    }

    @Test
    public void shouldTransformSPICaseToListCourtHearingDefendantIsYouth() throws IOException {
        //given
        final List<ListHearingRequest> listHearingRequest = getListHearingRequest(true);

        final JsonEnvelope envelopeReferral = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(),
                Json.createObjectBuilder().build());

        when(referenceDataService.getReferralReasonById(any(), any(), any()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("reason", "reason for referral").build()));

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer
                .transform(envelopeReferral, Arrays.asList(getProsecutionCase(LocalDate.now().minusYears(15))), listHearingRequest, UUID.randomUUID());

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

    private List<ListHearingRequest> getListHearingRequest(final boolean isListedStartDateTimePresent) {
        //Either EarliestStartDateTime or ListedStartDateTime can be present. Not both.
        return Arrays.asList(ListHearingRequest.listHearingRequest()
                .withCourtCentre(createCourtCenter())
                .withEarliestStartDateTime(!isListedStartDateTimePresent ? earliestStartDateTime : null)
                .withEstimateMinutes(Integer.valueOf(15))
                .withHearingType(HearingType.hearingType().withId(UUID.randomUUID()).build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(Arrays.asList(offenceId))
                        .withDefendantId(defendantId)
                        .build()))
                .withListedStartDateTime(isListedStartDateTimePresent ? listedStartDateTime : null)
                .withListingDirections("wheelchair access required")
                .withProsecutorDatesToAvoid("Thursdays")
                .withReportingRestrictionReason(AUTOMATIC_ANONYMITY)
                .build());
    }


}
