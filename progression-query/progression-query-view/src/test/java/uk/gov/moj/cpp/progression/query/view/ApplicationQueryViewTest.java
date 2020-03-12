package uk.gov.moj.cpp.progression.query.view;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.ApplicantDetails;
import uk.gov.justice.progression.courts.ApplicationDetails;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.progression.domain.constant.NotificationStatus;
import uk.gov.moj.cpp.progression.domain.constant.NotificationType;
import uk.gov.moj.cpp.progression.query.ApplicationQueryView;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils.CourtApplicationSummary;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NotificationStatusRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ApplicationQueryViewTest {

    final static private UUID APPLICATION_ID = UUID.randomUUID();
    final static private String APPLICATION_ARN = new StringGenerator().next();
    final static private String APPLICANT_FIRST_NAME = new StringGenerator().next();
    final static private String APPLICANT_LAST_NAME = new StringGenerator().next();
    final static private String APPLICANT_MIDDLE_NAME = new StringGenerator().next();
    final static private String RESPONDENTS_ORG_NAME = new StringGenerator().next();
    final static private String APPLICATION_PROSECUTOR_NAME = new StringGenerator().next();
    @Mock
    private CourtApplicationRepository courtApplicationRepository;
    @Mock
    private CourtDocumentRepository courtDocumentRepository;
    @Mock
    private HearingApplicationRepository hearingApplicationRepository;
    @Mock
    private NotificationStatusRepository notificationStatusRepository;
    @InjectMocks
    private ApplicationQueryView applicationQueryView;
    @Mock
    private JsonObject applicationJson;
    @Mock
    private JsonObject courtDocumentJson;
    @Mock
    private JsonObject courtDocumentObject;
    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Spy
    private JsonObjectToObjectConverter realJsonObjectToObjectConverter;
    @Captor
    private ArgumentCaptor<Object> summaryToJsonCaptor;
    @Mock
    private ApplicationAtAGlanceHelper applicationAtAGlanceHelper;

    private static CourtApplication getCourtApplicationWithLegalEntityDefendant() {
        final Defendant defendant = Defendant.defendant().
                withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withName("ABC LTD").build()).build()).build();
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withType(CourtApplicationType.courtApplicationType().withApplicationType("Apil").build())
                .withApplicationReference(APPLICATION_ARN)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withDefendant(defendant)
                        .build())
                .withRespondents(Arrays.asList(CourtApplicationRespondent.courtApplicationRespondent()
                        .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                        .withName(APPLICATION_PROSECUTOR_NAME)
                                        .build())
                                .build())
                        .build(), CourtApplicationRespondent.courtApplicationRespondent()
                        .withPartyDetails(CourtApplicationParty.courtApplicationParty().withDefendant(defendant)
                                .withOrganisation(Organisation.organisation()
                                        .withName(RESPONDENTS_ORG_NAME)
                                        .build())
                                .build())
                        .build()))
                .build();
    }

    private static NotificationStatusEntity notificationStatusEntity(final UUID applicationId) {
        final NotificationStatusEntity entity = new NotificationStatusEntity();
        entity.setId(randomUUID());
        entity.setNotificationId(randomUUID());
        entity.setApplicationId(applicationId);
        entity.setNotificationStatus(NotificationStatus.NOTIFICATION_REQUEST);
        entity.setNotificationType(NotificationType.PRINT);
        entity.setStatusCode(1);
        entity.setErrorMessage("Test Error Message");
        entity.setUpdated(ZonedDateTime.now());
        return entity;
    }

    private static PersonDefendant createPersonDefendant() {
        return PersonDefendant.personDefendant()
                .withPersonDetails(Person.person()
                        .withFirstName("John")
                        .withMiddleName("Martin")
                        .withLastName("Williams")
                        .withDateOfBirth(LocalDate.of(2017, 02, 01))
                        .build())
                .build();
    }

    private static CourtApplication getCourtApplication() {
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withType(CourtApplicationType.courtApplicationType().withApplicationType("Apil").build())
                .withApplicationReference(APPLICATION_ARN)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person()
                                .withFirstName(APPLICANT_FIRST_NAME)
                                .withLastName(APPLICANT_LAST_NAME)
                                .withMiddleName(APPLICANT_MIDDLE_NAME)
                                .build())
                        .build())
                .withRespondents(Arrays.asList(CourtApplicationRespondent.courtApplicationRespondent()
                        .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                        .withName(APPLICATION_PROSECUTOR_NAME)
                                        .build())
                                .build())
                        .build(), CourtApplicationRespondent.courtApplicationRespondent()
                        .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                .withOrganisation(Organisation.organisation()
                                        .withName(RESPONDENTS_ORG_NAME)
                                        .build())
                                .build())
                        .build()))
                .build();
    }

    @Before
    public void setup() {
        setField(this.realJsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldFindApplicationById() {
        final UUID applicationId = randomUUID();
        final JsonObject jsonObject = createObjectBuilder()
                .add("applicationId", applicationId.toString()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.query.application").build(),
                jsonObject);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload("{}");
        courtApplicationEntity.setAssignedUserId(UUID.randomUUID());

        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey());
        hearingApplicationEntity.setHearing(new HearingEntity());

        CourtDocument courtDocument = CourtDocument.courtDocument()
                .build();
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setPayload("{}");
        final List<CourtDocumentEntity> courtDocumentEntities = Arrays.asList(courtDocumentEntity);

        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(courtApplicationEntity);
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(applicationJson);

        when(courtDocumentRepository.findByApplicationId(applicationId)).thenReturn(courtDocumentEntities);
        when(hearingApplicationRepository.findByApplicationId(applicationId)).thenReturn(Arrays.asList(hearingApplicationEntity));

        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(courtDocumentJson);
        when(jsonObjectToObjectConverter.convert(any(), eq(CourtDocument.class))).thenReturn(courtDocument);
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtDocument.class))).thenReturn(this.courtDocumentObject);

        final JsonEnvelope response = applicationQueryView.getApplication(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("courtApplication"), notNullValue());
        assertEquals(response.payloadAsJsonObject().getJsonObject("assignedUser").getString("userId"), courtApplicationEntity.getAssignedUserId().toString());
        assertThat(response.payloadAsJsonObject().get("courtDocuments"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("linkedApplicationsSummary"), notNullValue());

        final JsonObject assignedUserJson = response.payloadAsJsonObject().getJsonObject("assignedUser");
        assertThat(assignedUserJson.getString("userId"), is(courtApplicationEntity.getAssignedUserId().toString()));

    }

    @Test
    public void shouldGetApplicationAtAGlance() {
        final UUID applicationId = randomUUID();
        final JsonObject jsonObject = createObjectBuilder()
                .add("applicationId", applicationId.toString()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.query.application.aaag").build(),
                jsonObject);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload("{\"id\": \"9aec6dcc-564c-11ea-8e2d-0242ac130003\"}");

        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(courtApplicationEntity);

        final CourtApplicationEntity childCourtApplicationEntity = new CourtApplicationEntity();
        when(courtApplicationRepository.findByParentApplicationId(any())).thenReturn(singletonList(childCourtApplicationEntity));

        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(applicationJson);
        CourtApplication childCourtApplication = mock(CourtApplication.class);
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(mock(CourtApplication.class), childCourtApplication);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(applicationJson);
        when(childCourtApplication.getId()).thenReturn(UUID.randomUUID());
        when(childCourtApplication.getApplicant()).thenReturn(getCourtApplicant());

        when(applicationAtAGlanceHelper.getApplicationDetails(any(CourtApplication.class))).thenReturn(mock(ApplicationDetails.class));
        final JsonObject mockApplicationDetailsJson = mock(JsonObject.class);

        when(applicationAtAGlanceHelper.getApplicantDetails(any(CourtApplication.class))).thenReturn(mock(ApplicantDetails.class));
        final JsonObject mockApplicantDetailsJson = mock(JsonObject.class);

        when(objectToJsonObjectConverter.convert(any(ApplicationDetails.class))).thenReturn(mockApplicationDetailsJson).thenReturn(mockApplicantDetailsJson);

        final JsonEnvelope response = applicationQueryView.getCourtApplicationForApplicationAtAGlance(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getString("applicationId"), is(applicationId.toString()));
        assertThat(response.payloadAsJsonObject().getJsonObject("applicationDetails"), is(mockApplicationDetailsJson));
        assertThat(response.payloadAsJsonObject().getJsonObject("applicantDetails"), is(mockApplicantDetailsJson));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedApplications").size(), is(1));
    }

    @Test
    public void shouldReturnApplicationSummary() {
        final UUID applicationId = randomUUID();
        final JsonObject jsonObject = createObjectBuilder()
                .add("applicationId", applicationId.toString()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.query.application.summary").build(),
                jsonObject);

        final CourtApplication courtApplication = getCourtApplication();
        final JsonObject summaryJson = createObjectBuilder().build();
        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setAssignedUserId(UUID.randomUUID());
        courtApplicationEntity.setApplicationId(APPLICATION_ID);
        final JsonObject courtApplicationJson = createObjectBuilder().build();
        courtApplicationEntity.setPayload("xyz");

        when(stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload())).thenReturn(courtApplicationJson);
        when(jsonObjectToObjectConverter.convert(courtApplicationJson, CourtApplication.class)).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(summaryJson);


        when(courtApplicationRepository.findByParentApplicationId(applicationId)).thenReturn(Arrays.asList(courtApplicationEntity));
        //when(jsonObjectToObjectConverter.convert(this.applicationJson, CourtApplication.class)).thenReturn(courtApplication);
        final JsonEnvelope response = applicationQueryView.getApplicationSummary(jsonEnvelope);

        Mockito.verify(objectToJsonObjectConverter, times(1)).convert(summaryToJsonCaptor.capture());

        final JsonArray courtApplicationsJson = response.payloadAsJsonObject().getJsonArray("courtApplications");
        assertThat(courtApplicationsJson, notNullValue());

        final CourtApplicationSummary summary0 = (CourtApplicationSummary) summaryToJsonCaptor.getValue();
        Assert.assertEquals(summary0.getAssignedUserId(), courtApplicationEntity.getAssignedUserId());

    }

    @Test
    public void shouldReturnApplicationSummaryWithLegalEntityDefendant() {
        final UUID applicationId = randomUUID();
        final JsonObject jsonObject = createObjectBuilder()
                .add("applicationId", applicationId.toString()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.query.application.summary").build(),
                jsonObject);

        final CourtApplication courtApplication = getCourtApplicationWithLegalEntityDefendant();
        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setApplicationId(APPLICATION_ID);
        courtApplicationEntity.setPayload("{}");

        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(jsonObject);
        when(courtApplicationRepository.findByParentApplicationId(applicationId)).thenReturn(Arrays.asList(courtApplicationEntity));
        when(jsonObjectToObjectConverter.convert(jsonObject, CourtApplication.class)).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtDocument.class))).thenReturn(jsonObject);

        final JsonEnvelope response = applicationQueryView.getApplicationSummary(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("courtApplications"), notNullValue());
    }

    @Test
    public void shouldGetCourtDocumentNotificationStatus() {
        final UUID applicationId = UUID.randomUUID();

        final JsonObject jsonObject = createObjectBuilder()
                .add(ApplicationQueryView.APPLICATION_ID_SEARCH_PARAM, applicationId.toString())
                .build();

        final JsonEnvelope jsonEnvelopeIn = envelopeFrom(
                metadataBuilder().withId(randomUUID())
                        .withName("progression.query.application.notification-status").build(),
                jsonObject);

        final NotificationStatusEntity notificationStatusEntity = notificationStatusEntity(applicationId);

        when(notificationStatusRepository.findByApplicationId(applicationId)).thenReturn(asList(notificationStatusEntity));

        final JsonEnvelope jsonEnvelopeOut = applicationQueryView.getApplicationNotifications(jsonEnvelopeIn);

        System.out.println(jsonEnvelopeOut.payloadAsJsonObject());

        assertThat(jsonEnvelopeOut.payloadAsJsonObject().getJsonArray("notificationStatus").size(), is(1));
    }

    private ProsecutionCase createProsecutionCase(final UUID caseId) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("TFL")
                        .withCaseURN("xxxxx")
                        .build())
                .withDefendants(createDefendants())
                .build();
    }

    private ProsecutionCaseEntity createProsecutionCaseEntity(final ProsecutionCase prosecutionCase,
                                                              final UUID caseId) {

        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);
        prosecutionCaseEntity.setPayload(prosecutionCaseJson.toString());

        return prosecutionCaseEntity;
    }

    private List<Defendant> createDefendants() {
        final List<Defendant> defendants = new ArrayList<>();
        final Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withPersonDefendant(createPersonDefendant())
                .withOffences(createOffences())
                .build();
        defendants.add(defendant);
        return defendants;
    }

    private List<Offence> createOffences() {
        final Offence offence = Offence.offence()
                .withId(randomUUID())
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode(randomUUID().toString())
                .withOffenceTitle("Offence Title")
                .withOffenceTitleWelsh("Offence Title Welsh")
                .withOffenceLegislation("Offence Legislation")
                .withOffenceLegislationWelsh("Offence Legislation Welsh")
                .withWording("Wording")
                .withWordingWelsh("Wording Welsh")
                .withStartDate(LocalDate.of(2018, 01, 01))
                .withEndDate(LocalDate.of(2018, 01, 05))
                .withCount(5)
                .withConvictionDate(LocalDate.of(2018, 02, 02))
                .build();
        return Arrays.asList(offence);
    }

    private CourtApplicationParty getCourtApplicant() {
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withDefendant(Defendant.defendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person()
                        .withFirstName("firstName").withMiddleName("middleName").withLastName("lastName").build()).build()).build()).build();
    }

}


