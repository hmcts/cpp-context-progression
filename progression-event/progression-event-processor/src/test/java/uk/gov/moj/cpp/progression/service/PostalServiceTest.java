package uk.gov.moj.cpp.progression.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.cpp.progression.utils.TestUtils.TEST_COMPANY_ADDRESS_LINE1;
import static uk.gov.moj.cpp.progression.utils.TestUtils.TEST_COMPANY_ADDRESS_POSTCODE;
import static uk.gov.moj.cpp.progression.utils.TestUtils.TEST_COMPANY_NAME;
import static uk.gov.moj.cpp.progression.utils.TestUtils.TEST_FIRST_NAME;
import static uk.gov.moj.cpp.progression.utils.TestUtils.TEST_LAST_NAME;
import static uk.gov.moj.cpp.progression.utils.TestUtils.TEST_PERSON_ADDRESS_LINE1;
import static uk.gov.moj.cpp.progression.utils.TestUtils.TEST_PERSON_ADDRESS_POSTCODE;
import static uk.gov.moj.cpp.progression.utils.TestUtils.TEST_PROSECUTION_AUTHORITY_ADDRESS_LINE1;
import static uk.gov.moj.cpp.progression.utils.TestUtils.TEST_PROSECUTION_AUTHORITY_ADDRESS_POSTCODE;
import static uk.gov.moj.cpp.progression.utils.TestUtils.TEST_PROSECUTION_AUTHORITY_NAME;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildCourtApplicationPartyWithLegalEntity;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildCourtApplicationPartyWithPersonDefendant;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildCourtApplicationPartyWithProsecutionAuthority;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildDefendantWithLegalEntity;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildDefendantWithPersonDefendant;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyCompanyAddress;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyCompanyName;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyPersonAddress;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyPersonName;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyProsecutionAuthorityAddress;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyProsecutionAuthorityName;

import org.powermock.reflect.Whitebox;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.progression.domain.PostalAddress;
import uk.gov.moj.cpp.progression.domain.PostalDefendant;

@RunWith(MockitoJUnitRunner.class)
public class PostalServiceTest {

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    private final ZonedDateTime hearingDateTime = ZonedDateTime.now(ZoneId.of("UTC"));

    private UUID applicationId;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private Sender sender;

    @InjectMocks
    private PostalService postalService;

    @Before
    public void setUp() {
        this.applicationId = randomUUID();
    }

    @Test
    public void sendPostToCourtApplicationParty() {

        when(documentGeneratorService.generateDocument(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(UUID.randomUUID());

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withDefendant(Defendant.defendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person()
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .build()).build()).build())
                        .build())
                .build();

        final String hearingDate = hearingDateTime.toLocalDate().toString();

        final String hearingTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(hearingDateTime);

        postalService.sendPostToCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                courtApplication.getId(),
                courtApplication.getApplicationReference(),
                courtApplication.getType().getApplicationType(),
                courtApplication.getType().getApplicationLegislation(),
                courtApplication.getOrderingCourt(),
                courtApplication.getApplicant());

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("progression.command.create-court-document"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification"))
                        )))));
    }

    @Test
    public void sendPostNotification() {

        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(2019, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .build();

        when(documentGeneratorService.generateDocument(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(UUID.randomUUID());

        when(referenceDataService.getOrganisationUnitById(Mockito.any(), Mockito.any())).thenReturn(Optional.of(courtCentreJson));

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "008")
                        .add("name", "Manchester Courts")
                        .build())
                .build();

        when(referenceDataService.getEnforcementAreaByLjaCode(Mockito.any(), Mockito.any())).thenReturn(ljaDetails);

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withDefendant(Defendant.defendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
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

        final String hearingDate = hearingDateTime.toLocalDate().toString();

        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");

        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());

        System.out.println(hearingTime);
        postalService.sendPostToCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                applicationId,
                "05PP1000915-01",
                "Application to amend the requirements of a suspended sentence order",
                "In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
                CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withName("Lavendar Hill Magistrates' Court")
                        .withAddress(Address.address()
                                .withAddress1("Court Road")
                                .withAddress2("Court Town")
                                .withAddress3("Lavendar Hill, London")
                                .withPostcode("EA22 5TF")
                                .build())
                        .build(),
                courtApplication.getApplicant());

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("progression.command.create-court-document"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification"))
                        )))));
    }

    @Test
    public void getDefendantNameWithPersonDefendantTest() throws Exception {
        Defendant personDefendantMock = buildDefendantWithPersonDefendant();
        String resultName = Whitebox.invokeMethod(postalService, "getDefendantName", personDefendantMock);
        verifyPersonName(resultName);
    }

    @Test
    public void getDefendantNameWithLegalEntityDefendantTest() throws Exception {
        Defendant legalEntityDefendantMock = buildDefendantWithLegalEntity();
        String resultName = Whitebox.invokeMethod(postalService, "getDefendantName", legalEntityDefendantMock);
        verifyCompanyName(resultName);
    }

    @Test
    public void getDefendantPostalAddressWithPersonDefendantTest() throws Exception {
        Defendant personDefendantMock = buildDefendantWithPersonDefendant();
        PostalAddress resultAddress = Whitebox.invokeMethod(postalService, "getDefendantPostalAddress", personDefendantMock);
        verifyPersonAddress(resultAddress);
    }

    @Test
    public void getDefendantPostalAddressWithLegalEntityDefendantTest() throws Exception {
        Defendant legalEntityDefendantMock = buildDefendantWithLegalEntity();
        PostalAddress resultAddress = Whitebox.invokeMethod(postalService, "getDefendantPostalAddress", legalEntityDefendantMock);
        verifyCompanyAddress(resultAddress);
    }

    @Test
    public void buildDefendantWithLegalEntityTest() throws Exception {
        Defendant legalEntityDefendantMock = buildDefendantWithLegalEntity();
        PostalDefendant resultPostalDefendant = Whitebox.invokeMethod(postalService, "buildDefendant", legalEntityDefendantMock);
        verifyCompanyAddress(resultPostalDefendant.getAddress());
        verifyCompanyName(resultPostalDefendant.getName());
    }

    @Test
    public void getNameTest() throws Exception {
        CourtApplicationParty courtApplicationPartyMock = buildCourtApplicationPartyWithLegalEntity();
        String companyName  = Whitebox.invokeMethod(postalService, "getName", courtApplicationPartyMock);
        verifyCompanyName(companyName);

        CourtApplicationParty courtApplicationPartyMock1 = buildCourtApplicationPartyWithPersonDefendant();
        String personName  = Whitebox.invokeMethod(postalService, "getName", courtApplicationPartyMock1);
        verifyPersonName(personName);

        CourtApplicationParty courtApplicationPartyMock2 = buildCourtApplicationPartyWithProsecutionAuthority();
        String prosecutionAuthorityName = Whitebox.invokeMethod(postalService, "getName", courtApplicationPartyMock2);
        verifyProsecutionAuthorityName(prosecutionAuthorityName);
    }

    @Test
    public void getAddressTest() throws Exception {
        CourtApplicationParty courtApplicationPartyMock = buildCourtApplicationPartyWithLegalEntity();
        PostalAddress companyAddress  = Whitebox.invokeMethod(postalService, "getAddress", courtApplicationPartyMock);
        verifyCompanyAddress(companyAddress);

        CourtApplicationParty courtApplicationPartyMock1 = buildCourtApplicationPartyWithPersonDefendant();
        PostalAddress personAddress  = Whitebox.invokeMethod(postalService, "getAddress", courtApplicationPartyMock1);
        verifyPersonAddress(personAddress);

        CourtApplicationParty courtApplicationPartyMock2 = buildCourtApplicationPartyWithProsecutionAuthority();
        PostalAddress prosecutionAuthorityAddress = Whitebox.invokeMethod(postalService, "getAddress", courtApplicationPartyMock2);
        verifyProsecutionAuthorityAddress(prosecutionAuthorityAddress);
    }
}