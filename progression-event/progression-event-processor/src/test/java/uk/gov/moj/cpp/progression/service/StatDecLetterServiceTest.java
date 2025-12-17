package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.service.payloads.StatDacAppointmentLetterPayload;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StatDecLetterServiceTest {

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    private static final String STAT_DEC_VIRTUAL_HEARING = "NPE_StatutoryDeclarationVirtualHearing";
    private static final String STAT_DEC_COURT_HEARING = "NPE_StatutoryDeclarationHearing";

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    private UUID applicationId;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;


    @InjectMocks
    private StatDecLetterService statDecLetterService;

    @BeforeEach
    public void setUp() {
        this.applicationId = randomUUID();
    }


    @Test
    public void testGenerateAppointmentLetterDocument() {
        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(20121, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .build();

        final UUID materialId = UUID.randomUUID();

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(materialId);

        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "008")
                        .add("name", "Manchester Courts")
                        .build())
                .build();

        when(referenceDataService.getEnforcementAreaByLjaCode(any(), any(), any())).thenReturn(ljaDetails);

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicationReference("05PP1000915-01")
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person()
                                        .withFirstName("John")
                                        .withLastName("Edward")
                                        .withDateOfBirth(LocalDate.of(1998, 8, 10))
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .withAddress(Address.address()
                                                .withAddress1("22 Acacia Avenue")
                                                .withAddress2("Acacia Town")
                                                .withAddress3("Acacia City")
                                                .withAddress4("Test")
                                                .withPostcode("AC1 4AC")
                                                .build()
                                        )
                                        .build()).build()).build())
                        .build())
                .build();

        assertThat(statDecLetterService.generateAppointmentLetterDocument(envelope, hearingDateTime,  courtApplication, CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withName("Lavendar Hill Magistrates' Court")
                        .withAddress(Address.address()
                                .withAddress1("Court Road")
                                .withAddress2("Court Town")
                                .withAddress3("Lavendar Hill, London")
                                .withPostcode("EA22 5TF")
                                .build())
                        .build(),
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, STAT_DEC_VIRTUAL_HEARING), is(materialId));


    }


    @Test
    public void testGetStatDecAppointmentLetterPayloadWhenDocumentTemplateIsStatDecCourtHearing() {

        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(2021, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .add("address1", "Court Road")
                .add("address2", "Court Town")
                .add("address3", "Lavendar Hill, London")
                .add("postcode", "EA22 5TF")
                .build();


        final JsonObject localJusticeArea = createObjectBuilder()
                .add("nationalCourtCode", "008")
                .add("name", "Manchester Courts")
                .build();


        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicationReference("05PP1000915-01")
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person()
                                        .withFirstName("John")
                                        .withLastName("Edward")
                                        .withDateOfBirth(LocalDate.of(1998, 8, 10))
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .withAddress(Address.address()
                                                .withAddress1("22 Acacia Avenue")
                                                .withAddress2("Acacia Town")
                                                .withAddress3("Acacia City")
                                                .withAddress4("Test")
                                                .withPostcode("AC1 4AC")
                                                .build()
                                        )
                                        .build()).build()).build())
                        .build())
                .build();
        final CourtCentre courtCentre = CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName("Lavendar Hill Magistrates' Court")
                .withRoomName("Room 1")
                .withAddress(Address.address()
                        .withAddress1("Court Road")
                        .withAddress2("Court Town")
                        .withAddress3("Lavendar Hill, London")
                        .withPostcode("EA22 5TF")
                        .build())
                .build();
        final StatDacAppointmentLetterPayload statDacAppointmentLetterPayload = statDecLetterService.getStatDecAppointmentLetterPayload(hearingDateTime, courtApplication.getApplicationReference(),
                courtCentre, courtCentreJson, localJusticeArea, courtApplication.getApplicant(), JurisdictionType.MAGISTRATES, STAT_DEC_COURT_HEARING);

        assertThat(statDacAppointmentLetterPayload.getCaseApplicationReferences().get(0), is (courtApplication.getApplicationReference()));
        assertThat(statDacAppointmentLetterPayload.getCourtAddress(), notNullValue());
        assertThat(statDacAppointmentLetterPayload.getCourtAddress().getAddress1(), is("Court Road"));
        assertThat(statDacAppointmentLetterPayload.getCourtAddress().getAddress2(), is("Court Town"));
        assertThat(statDacAppointmentLetterPayload.getCourtAddress().getAddress3(), is("Lavendar Hill, London"));
        assertThat(statDacAppointmentLetterPayload.getCourtAddress().getPostCode(), is("EA22 5TF"));
        assertThat(statDacAppointmentLetterPayload.getOrderingCourt().getCourtCentreName(), is(courtCentre.getName()));
        assertThat(statDacAppointmentLetterPayload.getOrderingCourt().getLjaCode(), is("008"));
        assertThat(statDacAppointmentLetterPayload.getOrderingCourt().getLjaName(), is("Manchester Courts"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getName(), is("John Edward"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getAddress().getLine1(), is("22 Acacia Avenue"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getAddress().getLine2(), is("Acacia Town"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getAddress().getLine3(), is("Acacia City"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getAddress().getLine4(), is("Test"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getAddress().getPostCode(), is("AC1 4AC"));

    }

    @Test
    public void testGetStatDecAppointmentLetterPayloadWhenDocumentTemplateIsNotStatDecCourtHearing() {

        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(2021, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .add("address1", "Court Road")
                .add("address2", "Court Town")
                .add("address3", "Lavendar Hill, London")
                .add("postcode", "EA22 5TF")
                .build();

        final UUID materialId = UUID.randomUUID();

        final JsonObject localJusticeArea = createObjectBuilder()
                .add("nationalCourtCode", "008")
                .add("name", "Manchester Courts")
                .build();


        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicationReference("05PP1000915-01")
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person()
                                        .withFirstName("John")
                                        .withLastName("Edward")
                                        .withDateOfBirth(LocalDate.of(1998, 8, 10))
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .withAddress(Address.address()
                                                .withAddress1("22 Acacia Avenue")
                                                .withAddress2("Acacia Town")
                                                .withAddress3("Acacia City")
                                                .withAddress4("Test")
                                                .withPostcode("AC1 4AC")
                                                .build()
                                        )
                                        .build()).build()).build())
                        .build())
                .build();
        final CourtCentre courtCentre = CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName("Lavendar Hill Magistrates' Court")
                .withRoomName("Room 1")
                .withAddress(Address.address()
                        .withAddress1("Court Road")
                        .withAddress2("Court Town")
                        .withAddress3("Lavendar Hill, London")
                        .withPostcode("EA22 5TF")
                        .build())
                .build();
        final StatDacAppointmentLetterPayload statDacAppointmentLetterPayload = statDecLetterService.getStatDecAppointmentLetterPayload(hearingDateTime, courtApplication.getApplicationReference(),
                courtCentre, courtCentreJson, localJusticeArea, courtApplication.getApplicant(), JurisdictionType.MAGISTRATES, STAT_DEC_VIRTUAL_HEARING);

        assertThat(statDacAppointmentLetterPayload.getCaseApplicationReferences().get(0), is (courtApplication.getApplicationReference()));
        assertThat(statDacAppointmentLetterPayload.getCourtAddress(), notNullValue());
        assertThat(statDacAppointmentLetterPayload.getOrderingCourt().getCourtCentreName(), is(courtCentre.getName()));
        assertThat(statDacAppointmentLetterPayload.getOrderingCourt().getLjaCode(), is("008"));
        assertThat(statDacAppointmentLetterPayload.getOrderingCourt().getLjaName(), is("Manchester Courts"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getName(), is("John Edward"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getAddress().getLine1(), is("22 Acacia Avenue"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getAddress().getLine2(), is("Acacia Town"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getAddress().getLine3(), is("Acacia City"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getAddress().getLine4(), is("Test"));
        assertThat(statDacAppointmentLetterPayload.getOrderAddressee().getAddress().getPostCode(), is("AC1 4AC"));

    }

    @Test
    public void testGenerateAppointmentLetterDocumentForDefendantWithForeignAddressWithoutPostCode() {
        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(20121, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .build();

        final UUID materialId = UUID.randomUUID();

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(materialId);

        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "008")
                        .add("name", "Manchester Courts")
                        .build())
                .build();

        when(referenceDataService.getEnforcementAreaByLjaCode(any(), any(), any())).thenReturn(ljaDetails);

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicationReference("05PP1000915-01")
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person()
                                        .withFirstName("John")
                                        .withLastName("Edward")
                                        .withDateOfBirth(LocalDate.of(1998, 8, 10))
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .withAddress(Address.address()
                                                .withAddress1("22 Acacia Avenue")
                                                .withAddress2("Acacia Town")
                                                .withAddress3("Acacia City")
                                                .withAddress4("Test")
                                                .build()
                                        )
                                        .build()).build()).build())
                        .build())
                .build();

        assertThat(statDecLetterService.generateAppointmentLetterDocument(envelope, hearingDateTime,  courtApplication, CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withName("Lavendar Hill Magistrates' Court")
                        .withAddress(Address.address()
                                .withAddress1("Court Road")
                                .withAddress2("Court Town")
                                .withAddress3("Lavendar Hill, London")
                                .withPostcode("EA22 5TF")
                                .build())
                        .build(),
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, STAT_DEC_VIRTUAL_HEARING), is(materialId));
    }


}
