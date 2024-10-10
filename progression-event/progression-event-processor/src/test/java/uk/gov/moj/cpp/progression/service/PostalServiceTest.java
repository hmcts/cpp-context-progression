package uk.gov.moj.cpp.progression.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildCourtApplicationPartyWithLegalEntity;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildCourtApplicationPartyWithPersonDefendant;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildCourtApplicationPartyWithProsecutionAuthority;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildDefendantWithLegalEntity;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildDefendantWithPersonDefendant;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyCompanyAddress;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyCompanyName;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyCrownCourt;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyCrownCourtAmended;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyMagistratesCourt;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyMagistratesCourtAmended;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyPersonAddress;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyPersonName;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyProsecutionAuthorityAddress;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyProsecutionAuthorityName;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.domain.PostalAddress;
import uk.gov.moj.cpp.progression.domain.PostalAddressee;
import uk.gov.moj.cpp.progression.domain.PostalDefendant;
import uk.gov.moj.cpp.progression.domain.PostalNotification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

@ExtendWith(MockitoExtension.class)
public class PostalServiceTest {

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    private final ZonedDateTime hearingDateTime = ZonedDateTime.now(ZoneId.of("UTC"));
    public static final UUID APPLICATION_DOCUMENT_TYPE_ID = UUID.fromString("460fa7ce-c002-11e8-a355-529269fb1459");
    private final String PROGRESSION_CREATE_DOCUMENT =  "progression.command.create-court-document";

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();
    private UUID applicationId;
    private UUID caseId;
    private UUID materialId;
    @Mock
    private Requester requester;
    @Mock
    private RefDataService referenceDataService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private Sender sender;

    @InjectMocks
    private PostalService postalService;

    @BeforeEach
    public void setUp() {
        this.applicationId = randomUUID();
        this.caseId = randomUUID();
    }

    @Test
    public void sendPostToCourtApplicationParty() {

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));


        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
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
        final LocalDate orderedDate = LocalDate.now();

        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                courtApplication.getApplicationReference(),
                courtApplication.getType().getType(),
                courtApplication.getType().getTypeWelsh(),
                courtApplication.getType().getLegislation(),
                courtApplication.getType().getLegislationWelsh(),
                null,
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                false, false, orderedDate);
        postalService.sendPostalNotification(envelope, courtApplication.getId(), postalNotification, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.prosecutionCaseId", equalTo(caseId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));
    }

    @Test
    public void sendPostToCourtApplicationPartyForManualApplication() {

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));


        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
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
        final LocalDate orderedDate = null;

        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                courtApplication.getApplicationReference(),
                courtApplication.getType().getType(),
                courtApplication.getType().getTypeWelsh(),
                courtApplication.getType().getLegislation(),
                courtApplication.getType().getLegislationWelsh(),
                null,
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                false, false, orderedDate);

        assertThat(postalNotification.getIssueDate(), is(LocalDate.now()));

        postalService.sendPostalNotification(envelope, courtApplication.getId(), postalNotification, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.prosecutionCaseId", equalTo(caseId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));
    }

    @Test
    public void sendPostToCourtRespondentAaag() {

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));


        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
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
        final LocalDate orderedDate = LocalDate.now();

        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                courtApplication.getApplicationReference(),
                courtApplication.getType().getType(),
                courtApplication.getType().getTypeWelsh(),
                courtApplication.getType().getLegislation(),
                courtApplication.getType().getLegislationWelsh(),
                null,
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                false, false, orderedDate);
        postalService.sendPostalNotificationAaag(envelope, courtApplication.getId(), null, materialId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));
    }

    @Test
    public void sendPostToCourtApplicationPartyAmended() {

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));


        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
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
        final LocalDate orderedDate = LocalDate.now();

        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                courtApplication.getApplicationReference(),
                courtApplication.getType().getType(),
                courtApplication.getType().getTypeWelsh(),
                courtApplication.getType().getLegislation(),
                courtApplication.getType().getLegislationWelsh(),
                null,
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                true, false, orderedDate);

        assertThat(postalNotification.getAmendmentDate(), notNullValue());

        postalService.sendPostalNotification(envelope, courtApplication.getId(), postalNotification, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.prosecutionCaseId", equalTo(caseId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));
    }


    @Test
    public void sendNoPostToCourtApplicationParty() {
        ArgumentCaptor<JsonObject> argumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));

        final String organisationName = "test name";
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().build())
                        .withOrganisation(Organisation.organisation()
                                .withName(organisationName)
                                .build())
                        .build())
                .build();

        final String hearingDate = hearingDateTime.toLocalDate().toString();

        final String hearingTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(hearingDateTime);
        final LocalDate orderedDate = LocalDate.now();

        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                courtApplication.getApplicationReference(),
                courtApplication.getType().getType(),
                courtApplication.getType().getTypeWelsh(),
                courtApplication.getType().getLegislation(),
                courtApplication.getType().getLegislationWelsh(),
                null,
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                false, false, orderedDate);
        postalService.sendPostalNotification(envelope, courtApplication.getId(), postalNotification, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.prosecutionCaseId", equalTo(caseId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));

        verify(documentGeneratorService).generateDocument(eq(envelope), argumentCaptor.capture(), eq("PostalNotification"), eq(sender), any(), eq(applicationId), eq(false));

        assertThat(argumentCaptor.getValue().getString("applicantName"), is(organisationName));
    }

    @Test
    public void buildPostalNotificationWithJurisdictionTypeMag() {
        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "008")
                        .add("name", "Manchester Courts")
                        .build())
                .build();

        JsonObject localJusticeArea = ljaDetails.getJsonObject("localJusticeArea");

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
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

        final String hearingDate = hearingDateTime.toLocalDate().toString();
        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");
        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final String applicationParticulars = "testing";
        final String applicant = "Test";
        final Boolean isApplicant = false;
        final LocalDate orderedDate = LocalDate.now();

       PostalNotification postalNotification=  postalService.buildPostalNotification(
                hearingDate,
                hearingTime,
                courtApplication.getApplicationReference(),
               courtApplication.getType().getType(),
               courtApplication.getType().getTypeWelsh(),
               courtApplication.getType().getLegislation(),
               courtApplication.getType().getLegislationWelsh(),
                null,
                null,
                null,
                localJusticeArea,
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, applicationParticulars, courtApplication, applicant, EMPTY,
               PostalAddressee.builder().build(), null, orderedDate);

       verifyMagistratesCourt(postalNotification.getLjaCode(), postalNotification.getLjaName());
    }

    @Test
    public void buildPostalNotificationWithJurisdictionTypeMagAmended() {

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .build();

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "008")
                        .add("name", "Manchester Courts")
                        .build())
                .build();

        JsonObject localJusticeArea = ljaDetails.getJsonObject("localJusticeArea");

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
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

        final String hearingDate = hearingDateTime.toLocalDate().toString();
        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");
        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final String applicationParticulars = "testing";
        final String applicant = "Test";
        final Boolean isApplicant = false;
        final LocalDate orderedDate = LocalDate.now();

        PostalNotification postalNotification=  postalService.buildPostalNotification(
                hearingDate,
                hearingTime,
                courtApplication.getApplicationReference(),
                courtApplication.getType().getType(),
                courtApplication.getType().getTypeWelsh(),
                courtApplication.getType().getLegislation(),
                courtApplication.getType().getLegislationWelsh(),
                null,
                null,
                null,
                localJusticeArea,
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, applicationParticulars, courtApplication, applicant, EMPTY,
                PostalAddressee.builder().build(), true, orderedDate);

        verifyMagistratesCourtAmended(postalNotification.getLjaCode(), postalNotification.getLjaName(), postalNotification.getAmendmentDate());
    }

    @Test
    public void buildPostalNotificationWithJurisdictionTypeMagNotAmended() {

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .build();

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "008")
                        .add("name", "Manchester Courts")
                        .build())
                .build();

        JsonObject localJusticeArea = ljaDetails.getJsonObject("localJusticeArea");

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
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

        final String hearingDate = hearingDateTime.toLocalDate().toString();
        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");
        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final String applicationParticulars = "testing";
        final String applicant = "Test";
        final Boolean isApplicant = false;
        final LocalDate orderedDate = LocalDate.now();

        PostalNotification postalNotification=  postalService.buildPostalNotification(
                hearingDate,
                hearingTime,
                courtApplication.getApplicationReference(),
                courtApplication.getType().getType(),
                courtApplication.getType().getTypeWelsh(),
                courtApplication.getType().getLegislation(),
                courtApplication.getType().getLegislationWelsh(),
                null,
                null,
                null,
                localJusticeArea,
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, applicationParticulars, courtApplication, applicant, EMPTY
                , PostalAddressee.builder().build(), false, orderedDate);

        assertNull(postalNotification.getAmendmentDate());
        assertThat(postalNotification.getIssueDate(), is(orderedDate));
        verifyMagistratesCourt(postalNotification.getLjaCode(), postalNotification.getLjaName());
    }

    @Test
    public void buildPostalNotificationWithJurisdictionTypeCrown() {

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .build();

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "008")
                        .add("name", "Manchester Courts")
                        .build())
                .build();

        JsonObject localJusticeArea = ljaDetails.getJsonObject("localJusticeArea");

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
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

        final String hearingDate = hearingDateTime.toLocalDate().toString();
        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");
        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final String applicationParticulars = "testing";
        final String applicant = "test";
        final Boolean isApplicant = false;
        final LocalDate orderedDate = LocalDate.now();

        PostalNotification postalNotification=  postalService.buildPostalNotification(
                hearingDate,
                hearingTime,
                courtApplication.getApplicationReference(),
                courtApplication.getType().getType(),
                courtApplication.getType().getTypeWelsh(),
                courtApplication.getType().getLegislation(),
                courtApplication.getType().getLegislationWelsh(),
                null,
                null,
                null,
                localJusticeArea,
                courtApplication.getApplicant(),
                JurisdictionType.CROWN, applicationParticulars, courtApplication, applicant, EMPTY,
                PostalAddressee.builder().build(), null, orderedDate);

        verifyCrownCourt(postalNotification.getLjaCode(), postalNotification.getLjaName());

    }

    @Test
    public void buildPostalNotificationWithJurisdictionTypeCrownAmended() {

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .build();

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "008")
                        .add("name", "Manchester Courts")
                        .build())
                .build();

        JsonObject localJusticeArea = ljaDetails.getJsonObject("localJusticeArea");

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
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

        final String hearingDate = hearingDateTime.toLocalDate().toString();
        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");
        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final String applicationParticulars = "testing";
        final String applicant = "test";
        final Boolean isApplicant = false;
        final LocalDate orderedDate = LocalDate.now();

        PostalNotification postalNotification=  postalService.buildPostalNotification(
                hearingDate,
                hearingTime,
                courtApplication.getApplicationReference(),
                courtApplication.getType().getType(),
                courtApplication.getType().getTypeWelsh(),
                courtApplication.getType().getLegislation(),
                courtApplication.getType().getLegislationWelsh(),
                null,
                null,
                null,
                localJusticeArea,
                courtApplication.getApplicant(),
                JurisdictionType.CROWN, applicationParticulars, courtApplication, applicant, EMPTY,
                PostalAddressee.builder().build(), true, orderedDate);

        verifyCrownCourtAmended(postalNotification.getLjaCode(), postalNotification.getLjaName(), postalNotification.getAmendmentDate());
    }

    @Test
    public void buildPostalNotificationWithJurisdictionTypeCrownNotAmended() {

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .build();

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "008")
                        .add("name", "Manchester Courts")
                        .build())
                .build();

        JsonObject localJusticeArea = ljaDetails.getJsonObject("localJusticeArea");

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
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

        final String hearingDate = hearingDateTime.toLocalDate().toString();
        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");
        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final String applicationParticulars = "testing";
        final String applicant = "test";
        final Boolean isApplicant = false;
        final LocalDate orderedDate = LocalDate.now();

        PostalNotification postalNotification=  postalService.buildPostalNotification(
                hearingDate,
                hearingTime,
                courtApplication.getApplicationReference(),
                courtApplication.getType().getType(),
                courtApplication.getType().getTypeWelsh(),
                courtApplication.getType().getLegislation(),
                courtApplication.getType().getLegislationWelsh(),
                null,
                null,
                null,
                localJusticeArea,
                courtApplication.getApplicant(),
                JurisdictionType.CROWN, applicationParticulars, courtApplication, applicant, EMPTY,
                PostalAddressee.builder().build(), false, orderedDate);

        assertNull(postalNotification.getAmendmentDate());
        assertThat(postalNotification.getIssueDate(), is(orderedDate));
        verifyCrownCourt(postalNotification.getLjaCode(), postalNotification.getLjaName());
    }

    @Test
    public void sendPostNotification_courtNotInWelsh_applicationParticularTranslationNotRequired() {

        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(2019, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .build();

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());
        when(referenceDataService.getCourtCentreWithCourtRoomsById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));
        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "008")
                        .add("name", "Manchester Courts")
                        .build())
                .build();

        when(referenceDataService.getEnforcementAreaByLjaCode(any(), any(), any())).thenReturn(ljaDetails);

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));

        final CourtApplication courtApplication = getCourtApplication(false);

        final String hearingDate = hearingDateTime.toLocalDate().toString();

        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");

        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final LocalDate orderedDate = LocalDate.now();

        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                "05PP1000915-01",
                "Application to amend the requirements of a suspended sentence order",
                "welsh - Application to amend the requirements of a suspended sentence order",
                "In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
                "welsh - In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
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
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                false, false, orderedDate);

        assertThat(postalNotification.getIsWelsh(), is(false));
        assertThat(postalNotification.getIssueDate(), is(orderedDate));
        postalService.sendPostalNotification(envelope, courtApplication.getId(), postalNotification, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.prosecutionCaseId", equalTo(caseId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));
    }

    @Test
    public void sendPostNotification_courtInWelsh_applicationParticularTranslationNotRequired() {

        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(2019, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .add("isWelsh", true)
                .add("oucodeL3WelshName", "Caerdydd")
                .build();

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());

        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        when(referenceDataService.getCourtCentreWithCourtRoomsById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "3190")
                        .add("name", "Cardiff Magistrates' Court")
                        .add("welshName", "Caerdydd")
                        .build())
                .build();

        when(referenceDataService.getEnforcementAreaByLjaCode(any(), any(), any())).thenReturn(ljaDetails);

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));

        final CourtApplication courtApplication = getCourtApplication(false);

        final String hearingDate = hearingDateTime.toLocalDate().toString();

        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");

        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final LocalDate orderedDate = LocalDate.now();

        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                "05PP1000915-01",
                "Application to amend the requirements of a suspended sentence order",
                "welsh - Application to amend the requirements of a suspended sentence order",
                "In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
                "welsh - In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
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
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                false, false, orderedDate);
        postalService.sendPostalNotification(envelope, courtApplication.getId(), postalNotification, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.prosecutionCaseId", equalTo(caseId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));
    }

    @Test
    public void sendPostNotification_courtInWelsh_applicationParticularTranslationRequired() {

        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(2019, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .add("isWelsh", true)
                .add("oucodeL3WelshName", "Caerdydd")
                .build();

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());

        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        when(referenceDataService.getCourtCentreWithCourtRoomsById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "3190")
                        .add("name", "Cardiff Magistrates' Court")
                        .add("welshName", "Caerdydd")
                        .build())
                .build();

        when(referenceDataService.getEnforcementAreaByLjaCode(any(), any(), any())).thenReturn(ljaDetails);

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));

        final CourtApplication courtApplication = getCourtApplication(false);

        final String hearingDate = hearingDateTime.toLocalDate().toString();

        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");

        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final LocalDate orderedDate = LocalDate.now();

        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                "05PP1000915-01",
                "Application to amend the requirements of a suspended sentence order",
                "welsh - Application to amend the requirements of a suspended sentence order",
                "In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
                "welsh - In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
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
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                false, true, orderedDate);

        assertThat(postalNotification.getIsWelsh(), is(true));
        assertThat(postalNotification.getIssueDate(), is(orderedDate));

        postalService.sendPostalNotification(envelope, courtApplication.getId(), postalNotification, caseId);

        verify(sender, times(1)).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.prosecutionCaseId", equalTo(caseId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));
    }

    @Test
    public void shouldCreatePostalNotificationForDefendantWithForeignAddressWithoutPostCode() {

        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(2019, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .add("isWelsh", true)
                .add("oucodeL3WelshName", "Caerdydd")
                .build();

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());

        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        when(referenceDataService.getCourtCentreWithCourtRoomsById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "3190")
                        .add("name", "Cardiff Magistrates' Court")
                        .add("welshName", "Caerdydd")
                        .build())
                .build();

        when(referenceDataService.getEnforcementAreaByLjaCode(any(), any(), any())).thenReturn(ljaDetails);

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));

        final CourtApplication courtApplication = getCourtApplication(true);

        final String hearingDate = hearingDateTime.toLocalDate().toString();

        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");

        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final LocalDate orderedDate = LocalDate.now();

        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                "05PP1000915-01",
                "Application to amend the requirements of a suspended sentence order",
                "welsh - Application to amend the requirements of a suspended sentence order",
                "In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
                "welsh - In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
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
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                false, false, orderedDate);
        postalService.sendPostalNotification(envelope, courtApplication.getId(), postalNotification, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.prosecutionCaseId", equalTo(caseId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));
    }


    @Test
    public void shouldCreatePostalNotificationForOrganisation() {

        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(2019, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .add("isWelsh", true)
                .add("oucodeL3WelshName", "Caerdydd")
                .build();

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());

        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        when(referenceDataService.getCourtCentreWithCourtRoomsById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "3190")
                        .add("name", "Cardiff Magistrates' Court")
                        .add("welshName", "Caerdydd")
                        .build())
                .build();

        when(referenceDataService.getEnforcementAreaByLjaCode(any(), any(), any())).thenReturn(ljaDetails);

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));

        final CourtApplication courtApplication = getCourtApplicationWithOrganisation(true);

        final String hearingDate = hearingDateTime.toLocalDate().toString();

        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");

        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final LocalDate orderedDate = LocalDate.now();
        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                "05PP1000915-01",
                "Application to amend the requirements of a suspended sentence order",
                "welsh - Application to amend the requirements of a suspended sentence order",
                "In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
                "welsh - In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
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
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                false, false, orderedDate);
        postalService.sendPostalNotification(envelope, courtApplication.getId(), postalNotification, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.prosecutionCaseId", equalTo(caseId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));
    }

    @Test
    public void shouldCreatePostalNotificationForIndividual() {

        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(2019, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .add("isWelsh", true)
                .add("oucodeL3WelshName", "Caerdydd")
                .build();

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());

        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        when(referenceDataService.getCourtCentreWithCourtRoomsById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "3190")
                        .add("name", "Cardiff Magistrates' Court")
                        .add("welshName", "Caerdydd")
                        .build())
                .build();

        when(referenceDataService.getEnforcementAreaByLjaCode(any(), any(), any())).thenReturn(ljaDetails);

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));

        final CourtApplication courtApplication = getCourtApplicationWithIndividual(true);

        final String hearingDate = hearingDateTime.toLocalDate().toString();

        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");

        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final LocalDate orderedDate = LocalDate.now();
        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                "05PP1000915-01",
                "Application to amend the requirements of a suspended sentence order",
                "welsh - Application to amend the requirements of a suspended sentence order",
                "In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
                "welsh - In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
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
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                false, false, orderedDate);
        postalService.sendPostalNotification(envelope, courtApplication.getId(), postalNotification, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.prosecutionCaseId", equalTo(caseId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));
    }

    @Test
    public void shouldCreatePostalNotificationForOrganisationWhenLegalEntityNotNullInMasterDefendant() {

        final ZonedDateTime hearingDateTime = ZonedDateTime.of(
                LocalDate.of(2019, 4, 19),
                LocalTime.of(10, 0),
                ZoneId.of("UTC"));

        final JsonObject courtCentreJson = createObjectBuilder()
                .add("lja", "1234")
                .add("isWelsh", true)
                .add("oucodeL3WelshName", "Caerdydd")
                .build();

        when(documentGeneratorService.generateDocument(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(UUID.randomUUID());

        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        when(referenceDataService.getCourtCentreWithCourtRoomsById(any(), any(), any())).thenReturn(Optional.of(courtCentreJson));

        final JsonObject ljaDetails = createObjectBuilder()
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", "3190")
                        .add("name", "Cardiff Magistrates' Court")
                        .add("welshName", "Caerdydd")
                        .build())
                .build();

        when(referenceDataService.getEnforcementAreaByLjaCode(any(), any(), any())).thenReturn(ljaDetails);

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(generateDocumentTypeAccessForApplication(APPLICATION_DOCUMENT_TYPE_ID)));

        final CourtApplication courtApplication = getCourtApplicationWithLegalEntityOrganisation();

        final String hearingDate = hearingDateTime.toLocalDate().toString();

        final DateTimeFormatter dTF = DateTimeFormatter.ofPattern("HH:mm a");

        final String hearingTime = dTF.format(hearingDateTime.toLocalTime());
        final LocalDate orderedDate = LocalDate.now();
        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(
                envelope,
                hearingDate,
                hearingTime,
                "05PP1000915-01",
                "Application to amend the requirements of a suspended sentence order",
                "welsh - Application to amend the requirements of a suspended sentence order",
                "In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
                "welsh - In accordance with Part 3 of Schedule 12 to the Criminal Justice Act 2003",
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
                courtApplication.getApplicant(),
                JurisdictionType.MAGISTRATES, courtApplication.getApplicationParticulars(), courtApplication, EMPTY,
                false, false, orderedDate);
        postalService.sendPostalNotification(envelope, courtApplication.getId(), postalNotification, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(PROGRESSION_CREATE_DOCUMENT),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.courtDocument.documentCategory.applicationDocument.prosecutionCaseId", equalTo(caseId.toString())),
                                withJsonPath("$.courtDocument.name", equalTo("PostalNotification")),
                                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf"))
                        )))));
    }

    private CourtApplication getCourtApplication(boolean isForeignAddress) {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
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
                                                .withPostcode(isForeignAddress? null : "AC1 4AC")
                                                .build()
                                        )
                                        .build()).build()).build())
                        .build())
                .build();
        return courtApplication;
    }

    @Test
    public void getDefendantNameWithPersonDefendantTest() throws Exception {
        final MasterDefendant personDefendantMock = buildDefendantWithPersonDefendant();
        final String resultName = Whitebox.invokeMethod(postalService, "getDefendantName", personDefendantMock);
        verifyPersonName(resultName);
    }

    @Test
    public void getDefendantNameWithLegalEntityDefendantTest() throws Exception {
        final MasterDefendant legalEntityDefendantMock = buildDefendantWithLegalEntity();
        final String resultName = Whitebox.invokeMethod(postalService, "getDefendantName", legalEntityDefendantMock);
        verifyCompanyName(resultName);
    }

    @Test
    public void getDefendantPostalAddressWithPersonDefendantTest() throws Exception {
        final MasterDefendant personDefendantMock = buildDefendantWithPersonDefendant();
        final PostalAddress resultAddress = Whitebox.invokeMethod(postalService, "getDefendantPostalAddress", personDefendantMock);
        verifyPersonAddress(resultAddress);
    }

    @Test
    public void getDefendantPostalAddressWithLegalEntityDefendantTest() throws Exception {
        final MasterDefendant legalEntityDefendantMock = buildDefendantWithLegalEntity();
        final PostalAddress resultAddress = Whitebox.invokeMethod(postalService, "getDefendantPostalAddress", legalEntityDefendantMock);
        verifyCompanyAddress(resultAddress);
    }

    @Test
    public void buildDefendantWithLegalEntityTest() throws Exception {
        final MasterDefendant legalEntityDefendantMock = buildDefendantWithLegalEntity();
        final PostalDefendant resultPostalDefendant = Whitebox.invokeMethod(postalService, "buildDefendant", legalEntityDefendantMock);
        verifyCompanyAddress(resultPostalDefendant.getAddress());
        verifyCompanyName(resultPostalDefendant.getName());
    }

    @Test
    public void getNameTest() throws Exception {
        final CourtApplicationParty courtApplicationPartyMock = buildCourtApplicationPartyWithLegalEntity();
        final String companyName = Whitebox.invokeMethod(postalService, "getName", courtApplicationPartyMock);
        verifyCompanyName(companyName);

        final CourtApplicationParty courtApplicationPartyMock1 = buildCourtApplicationPartyWithPersonDefendant();
        final String personName = Whitebox.invokeMethod(postalService, "getName", courtApplicationPartyMock1);
        verifyPersonName(personName);

        final CourtApplicationParty courtApplicationPartyMock2 = buildCourtApplicationPartyWithProsecutionAuthority();
        final String prosecutionAuthorityName = Whitebox.invokeMethod(postalService, "getName", courtApplicationPartyMock2);
        verifyProsecutionAuthorityName(prosecutionAuthorityName);
    }

    @Test
    public void getAddressTest() throws Exception {
        final CourtApplicationParty courtApplicationPartyMock = buildCourtApplicationPartyWithLegalEntity();
        final PostalAddress companyAddress = Whitebox.invokeMethod(postalService, "getAddress", courtApplicationPartyMock);
        verifyCompanyAddress(companyAddress);

        final CourtApplicationParty courtApplicationPartyMock1 = buildCourtApplicationPartyWithPersonDefendant();
        final PostalAddress personAddress = Whitebox.invokeMethod(postalService, "getAddress", courtApplicationPartyMock1);
        verifyPersonAddress(personAddress);

        final CourtApplicationParty courtApplicationPartyMock2 = buildCourtApplicationPartyWithProsecutionAuthority();
        final PostalAddress prosecutionAuthorityAddress = Whitebox.invokeMethod(postalService, "getAddress", courtApplicationPartyMock2);
        verifyProsecutionAuthorityAddress(prosecutionAuthorityAddress);
    }


    private static JsonObject generateDocumentTypeAccessForApplication(UUID id) {
        return createObjectBuilder()
                .add("id", id.toString())
                .add("section", "Applications")
                .build();
    }


    private CourtApplication getCourtApplicationWithOrganisation(boolean isForeignAddress) {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withOrganisation(Organisation.organisation()
                                .withName("Organisation 1")
                                .withAddress(Address.address()
                                        .withAddress1("22 Acacia Avenue")
                                        .withAddress2("Acacia Town")
                                        .withAddress3("Acacia City")
                                        .withAddress4("Test")
                                        .withPostcode(isForeignAddress? null : "AC1 4AC")
                                        .build())
                                .build())
                        .build())
                .build();
        return courtApplication;
    }

    private CourtApplication getCourtApplicationWithIndividual(boolean isForeignAddress) {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person()
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
                                        .withPostcode(isForeignAddress? null : "AC1 4AC")
                                        .build()
                                )
                                .build()).build())
                .build();
        return courtApplication;
    }

    private CourtApplication getCourtApplicationWithLegalEntityOrganisation() {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                        .withOrganisation(Organisation.organisation()
                                                .withName("Organisation 1")
                                                .withAddress(Address.address()
                                                        .withAddress1("22 Acacia Avenue")
                                                        .withAddress2("Acacia Town")
                                                        .withAddress3("Acacia City")
                                                        .withAddress4("Test")
                                                        .build())
                                                .build())
                                        .build())
                                .build())

                        .build())
                .build();
        return courtApplication;
    }
}
