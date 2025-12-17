package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.query.view.utils.SchemaValidator.validateObjectAgainstSchema;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.courts.progression.query.ApplicationDetails;
import uk.gov.justice.progression.courts.ApplicantDetails;
import uk.gov.justice.progression.query.laa.ApplicationLaa;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.progression.domain.constant.NotificationStatus;
import uk.gov.moj.cpp.progression.domain.constant.NotificationType;
import uk.gov.moj.cpp.progression.query.utils.converters.laa.ApplicationLaaConverter;
import uk.gov.moj.cpp.progression.query.view.ApplicationAtAGlanceHelper;
import uk.gov.moj.cpp.progression.query.view.UserDetailsLoader;
import uk.gov.moj.cpp.progression.query.view.service.DefenceQueryService;
import uk.gov.moj.cpp.progression.query.view.utils.FileUtil;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils.CourtApplicationSummary;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NotificationStatusRepository;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.persistence.NoResultException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ApplicationQueryViewTest {

    final static private UUID APPLICATION_ID = UUID.randomUUID();
    final static private String APPLICATION_ARN = new StringGenerator().next();
    final static private String APPLICANT_FIRST_NAME = new StringGenerator().next();
    final static private String APPLICANT_LAST_NAME = new StringGenerator().next();
    final static private String APPLICANT_MIDDLE_NAME = new StringGenerator().next();
    final static private String RESPONDENTS_ORG_NAME = new StringGenerator().next();
    final static private String APPLICATION_PROSECUTOR_NAME = new StringGenerator().next();
    final static private String INACTIVE = "INACTIVE";

    @Mock
    private CourtApplicationRepository courtApplicationRepository;
    @Mock
    private CourtApplicationCaseRepository courtApplicationCaseRepository;
    @Mock
    private CourtDocumentRepository courtDocumentRepository;
    @Mock
    private HearingApplicationRepository hearingApplicationRepository;
    @Mock
    private NotificationStatusRepository notificationStatusRepository;

    @Mock
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

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
    private JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    @Spy
    private ObjectToJsonObjectConverter objectToJsonConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
    @Captor
    private ArgumentCaptor<Object> summaryToJsonCaptor;
    @Mock
    private ApplicationAtAGlanceHelper applicationAtAGlanceHelper;
    @Mock
    private ApplicationLaaConverter applicationLaaConverter;
    @Mock
    private SystemIdMapperClient systemIdMapperClient;
    @Mock
    private SystemUserProvider systemUserProvider;
    @Mock
    private DefenceQueryService defenceQueryService;

    private static final UUID SYSTEM_USER_ID = UUID.randomUUID();
    private static final String TARGET_TYPE_APPLICATION = "APPLICATION_ID_LAA";
    private static final String SOURCE_TYPE_APPLICATION = "LAA_APP_SHORT_ID";
    private static final String LAA_APPLICATION_SHORTID = "A23ABCDEFGH";
    @Mock
    private Requester requester;
    @Mock
    private UserDetailsLoader userDetailsLoader;

    @BeforeEach
    void setUp() {
        lenient().when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(SYSTEM_USER_ID));
    }

    private static CourtApplication getCourtApplicationWithLegalEntityDefendant() {
        final MasterDefendant masterDefendant = MasterDefendant.masterDefendant().
                withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withName("ABC LTD").build()).build()).build();
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withType(CourtApplicationType.courtApplicationType().withType("Apil").build())
                .withApplicationReference(APPLICATION_ARN)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(masterDefendant)
                        .build())
                .withRespondents(Arrays.asList(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withName(APPLICATION_PROSECUTOR_NAME)
                                .build())
                        .build(), CourtApplicationParty.courtApplicationParty()
                        .withOrganisation(Organisation.organisation()
                                .withName(RESPONDENTS_ORG_NAME)
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
                .withType(CourtApplicationType.courtApplicationType().withType("Apil").build())
                .withApplicationReference(APPLICATION_ARN)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person()
                                .withFirstName(APPLICANT_FIRST_NAME)
                                .withLastName(APPLICANT_LAST_NAME)
                                .withMiddleName(APPLICANT_MIDDLE_NAME)
                                .build())
                        .build())
                .withRespondents(Arrays.asList(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withName(APPLICATION_PROSECUTOR_NAME)
                                .build())
                        .build(), CourtApplicationParty.courtApplicationParty()
                        .withOrganisation(Organisation.organisation()
                                .withName(RESPONDENTS_ORG_NAME)
                                .build())
                        .build()))
                .build();
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
        when(courtDocumentRepository.findByApplicationId(applicationId)).thenReturn(courtDocumentEntities);

        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(courtDocumentJson);
        when(jsonObjectToObjectConverter.convert(any(), eq(CourtDocument.class))).thenReturn(courtDocument);
        when(objectToJsonObjectConverter.convert(any(CourtDocument.class))).thenReturn(this.courtDocumentObject);

        final JsonEnvelope response = applicationQueryView.getApplication(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("courtApplication"), notNullValue());
        assertEquals(response.payloadAsJsonObject().getJsonObject("assignedUser").getString("userId"), courtApplicationEntity.getAssignedUserId().toString());
        assertThat(response.payloadAsJsonObject().get("courtDocuments"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("linkedApplicationsSummary"), notNullValue());

        final JsonObject assignedUserJson = response.payloadAsJsonObject().getJsonObject("assignedUser");
        assertThat(assignedUserJson.getString("userId"), is(courtApplicationEntity.getAssignedUserId().toString()));
    }

    @Test
    public void shouldFindApplicationOnlyById() {
        final UUID applicationId = randomUUID();
        final JsonObject jsonObject = createObjectBuilder()
                .add("applicationId", applicationId.toString()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.query.application-only").build(),
                jsonObject);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload("{}");
        courtApplicationEntity.setAssignedUserId(UUID.randomUUID());

        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey());
        hearingApplicationEntity.setHearing(new HearingEntity());

        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(courtApplicationEntity);
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(applicationJson);

        final JsonEnvelope response = applicationQueryView.getApplicationOnly(jsonEnvelope);

        assertThat(response.payloadAsJsonObject().get("courtApplication"), notNullValue());
    }

    @Test
    public void shouldThrowForbiddenRequestExceptionWhenGetApplicationAtAGlanceIsRestrictedForTheUser() {
        final UUID applicationId = randomUUID();
        final JsonObject jsonObject = createObjectBuilder()
                .add("applicationId", applicationId.toString()).build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.query.application.aaag").build(),
                jsonObject);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload("{\"id\": \"9aec6dcc-564c-11ea-8e2d-0242ac130003\"}");
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(courtApplicationEntity);
        when(stringToJsonObjectConverter.convert(any())).thenReturn(applicationJson);
        CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().withCode("PL02134").build()).build();
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(courtApplication);
        when(userDetailsLoader.isUserHasPermissionForApplicationTypeCode(any(), any())).thenReturn(false);

        assertThrows(ForbiddenRequestException.class, () -> applicationQueryView.getCourtApplicationForApplicationAtAGlance(jsonEnvelope));
    }


    @Test
    public void shouldFindApplicationStatusForGivenApplicationIdList() {
        final UUID applicationId = randomUUID();
        final UUID applicationId2 = randomUUID();
        final JsonObject jsonObject = createObjectBuilder()
                .add("applicationIds", applicationId + "," + applicationId2).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.query.application-only").build(),
                jsonObject);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload("{\"applicationStatus\":\"LISTED\",\"id\":\"" + applicationId + "\"}");
        courtApplicationEntity.setAssignedUserId(UUID.randomUUID());

        final CourtApplicationEntity courtApplicationEntity2 = new CourtApplicationEntity();
        courtApplicationEntity2.setPayload("{\"applicationStatus\":\"FINALISED\",\"id\":\"" + applicationId2 + "\"}");
        courtApplicationEntity2.setAssignedUserId(UUID.randomUUID());

        when(courtApplicationRepository.findByApplicationIds(List.of(applicationId, applicationId2))).thenReturn(List.of(courtApplicationEntity, courtApplicationEntity2));
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(applicationJson);
        when(applicationJson.getString("id")).thenReturn(applicationId.toString(), applicationId2.toString());
        when(applicationJson.getString("applicationStatus")).thenReturn("LISTED", "FINALISED");

        final JsonEnvelope response = applicationQueryView.getApplicationStatus(jsonEnvelope);

        assertThat(response.payloadAsJsonObject().getJsonArray("applicationsWithStatus").size(), is(2));
        assertThat(response.payloadAsJsonObject().getJsonArray("applicationsWithStatus").getJsonObject(0).getString("applicationId"), is(applicationId.toString()));
        assertThat(response.payloadAsJsonObject().getJsonArray("applicationsWithStatus").getJsonObject(0).getString("applicationStatus"), is("LISTED"));
    }

    @Test
    public void shouldGetApplicationAtAGlance() {
        final UUID applicationId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final JsonObject jsonObject = createObjectBuilder()
                .add("applicationId", applicationId.toString()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.query.application.aaag").build(),
                jsonObject);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload("{\"id\": \"9aec6dcc-564c-11ea-8e2d-0242ac130003\"}");

        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence()
                .withId(randomUUID())
                .build());

        final CourtApplicationCase courtApplicationCase = CourtApplicationCase.courtApplicationCase()
                .withCaseStatus("INACTIVE")
                .withIsSJP(true)
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(offences)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .build();

        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(courtApplicationEntity);

        final CourtApplicationEntity childCourtApplicationEntity = new CourtApplicationEntity();
        when(courtApplicationRepository.findByParentApplicationId(any())).thenReturn(singletonList(childCourtApplicationEntity));

        when(stringToJsonObjectConverter.convert(any())).thenReturn(applicationJson);
        CourtApplication courtApplication = mock(CourtApplication.class);
        CourtApplication childCourtApplication = mock(CourtApplication.class);
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(courtApplication, childCourtApplication);
        when(courtApplication.getCourtApplicationCases()).thenReturn(singletonList(courtApplicationCase));
        when(childCourtApplication.getId()).thenReturn(UUID.randomUUID());
        when(childCourtApplication.getApplicant()).thenReturn(getCourtApplicant());

        final ApplicationDetails applicationDetailsMock = mock(ApplicationDetails.class);
        when(applicationAtAGlanceHelper.getApplicationDetails(any(CourtApplication.class))).thenReturn(applicationDetailsMock);
        final JsonObject mockApplicationDetailsJson = mock(JsonObject.class);

        when(applicationAtAGlanceHelper.getApplicantDetails(any(CourtApplication.class), any(JsonEnvelope.class), eq(false))).thenReturn(mock(ApplicantDetails.class));
        final JsonObject mockApplicantDetailsJson = mock(JsonObject.class);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(mockApplicationDetailsJson).thenReturn(mockApplicantDetailsJson);

        final ProsecutionCase prosecutionCaseMock = mock(ProsecutionCase.class);
        when(applicationAtAGlanceHelper.getProsecutionCase(eq(prosecutionCaseId))).thenReturn(prosecutionCaseMock);

        final JsonEnvelope response = applicationQueryView.getCourtApplicationForApplicationAtAGlance(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getString("applicationId"), is(applicationId.toString()));
        assertThat(response.payloadAsJsonObject().getJsonObject("applicationDetails"), is(mockApplicationDetailsJson));
        assertThat(response.payloadAsJsonObject().getJsonObject("applicantDetails"), is(mockApplicantDetailsJson));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedApplications").size(), is(1));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedCases").size(), is(1));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedCases").getJsonObject(0), is(notNullValue()));
        verify(prosecutionCaseMock, atMostOnce()).getCaseStatus();
        verify(prosecutionCaseMock, atMostOnce()).getInitiationCode();
        verify(prosecutionCaseMock, atMostOnce()).getInitiationCode();
        verify(applicationDetailsMock, atMostOnce()).getLinkType();
    }

    @Test
    void shouldGetApplicationAtAGlanceHideAddressForDefenceQueryAndDefendant() {
        final UUID applicationId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final JsonObject jsonObject = createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .add("isDefenceQuery", true)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.query.application.aaag").build(),
                jsonObject);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload("{\"id\": \"9aec6dcc-564c-11ea-8e2d-0242ac130003\"}");

        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence()
                .withId(randomUUID())
                .build());

        final CourtApplicationCase courtApplicationCase = CourtApplicationCase.courtApplicationCase()
                .withCaseStatus("INACTIVE")
                .withIsSJP(true)
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(offences)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .build();

        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(courtApplicationEntity);

        final CourtApplicationEntity childCourtApplicationEntity = new CourtApplicationEntity();
        when(courtApplicationRepository.findByParentApplicationId(any())).thenReturn(singletonList(childCourtApplicationEntity));

        when(stringToJsonObjectConverter.convert(any())).thenReturn(applicationJson);
        CourtApplication courtApplication = mock(CourtApplication.class);
        CourtApplication childCourtApplication = mock(CourtApplication.class);
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(courtApplication, childCourtApplication);
        when(courtApplication.getCourtApplicationCases()).thenReturn(singletonList(courtApplicationCase));
        when(childCourtApplication.getId()).thenReturn(UUID.randomUUID());
        when(childCourtApplication.getApplicant()).thenReturn(getCourtApplicant());


        final ApplicationDetails applicationDetailsMock = mock(ApplicationDetails.class);
        when(applicationAtAGlanceHelper.getApplicationDetails(any(CourtApplication.class))).thenReturn(applicationDetailsMock);
        final JsonObject mockApplicationDetailsJson = mock(JsonObject.class);

        when(applicationAtAGlanceHelper.getApplicantDetails(any(CourtApplication.class), any(JsonEnvelope.class), eq(true))).thenReturn(mock(ApplicantDetails.class));
        final JsonObject mockApplicantDetailsJson = mock(JsonObject.class);

        when(objectToJsonObjectConverter.convert(any()))
                .thenReturn(mockApplicationDetailsJson)
                .thenReturn(Json.createObjectBuilder().add("prosecutionCaseId", prosecutionCaseId.toString()).build())
                .thenReturn(mockApplicantDetailsJson);

        final ProsecutionCase prosecutionCaseMock = mock(ProsecutionCase.class);
        when(applicationAtAGlanceHelper.getProsecutionCase(eq(prosecutionCaseId))).thenReturn(prosecutionCaseMock);
        when(defenceQueryService.isUserOnlyDefendingCase(jsonEnvelope, prosecutionCaseId.toString())).thenReturn(true);

        final JsonEnvelope response = applicationQueryView.getCourtApplicationForApplicationAtAGlance(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getString("applicationId"), is(applicationId.toString()));
        assertThat(response.payloadAsJsonObject().getJsonObject("applicationDetails"), is(mockApplicationDetailsJson));
        assertThat(response.payloadAsJsonObject().getJsonObject("applicantDetails"), is(mockApplicantDetailsJson));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedApplications").size(), is(1));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedCases").size(), is(1));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedCases").getJsonObject(0), is(notNullValue()));
        verify(prosecutionCaseMock, atMostOnce()).getCaseStatus();
        verify(prosecutionCaseMock, atMostOnce()).getInitiationCode();
        verify(prosecutionCaseMock, atMostOnce()).getInitiationCode();
        verify(applicationDetailsMock, atMostOnce()).getLinkType();
    }

    @Test
    void shouldGetApplicationAtAGlanceNotHideAddressForDefenceQueryAndProsecutor() {
        final UUID applicationId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final JsonObject jsonObject = createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .add("isDefenceQuery", true)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.query.application.aaag").build(),
                jsonObject);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload("{\"id\": \"9aec6dcc-564c-11ea-8e2d-0242ac130003\"}");

        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence()
                .withId(randomUUID())
                .build());

        final CourtApplicationCase courtApplicationCase = CourtApplicationCase.courtApplicationCase()
                .withCaseStatus("INACTIVE")
                .withIsSJP(true)
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(offences)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .build();

        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(courtApplicationEntity);

        final CourtApplicationEntity childCourtApplicationEntity = new CourtApplicationEntity();
        when(courtApplicationRepository.findByParentApplicationId(any())).thenReturn(singletonList(childCourtApplicationEntity));

        when(stringToJsonObjectConverter.convert(any())).thenReturn(applicationJson);
        CourtApplication courtApplication = mock(CourtApplication.class);
        CourtApplication childCourtApplication = mock(CourtApplication.class);
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(courtApplication, childCourtApplication);
        when(courtApplication.getCourtApplicationCases()).thenReturn(singletonList(courtApplicationCase));
        when(childCourtApplication.getId()).thenReturn(UUID.randomUUID());
        when(childCourtApplication.getApplicant()).thenReturn(getCourtApplicant());


        final ApplicationDetails applicationDetailsMock = mock(ApplicationDetails.class);
        when(applicationAtAGlanceHelper.getApplicationDetails(any(CourtApplication.class))).thenReturn(applicationDetailsMock);
        final JsonObject mockApplicationDetailsJson = mock(JsonObject.class);

        when(applicationAtAGlanceHelper.getApplicantDetails(any(CourtApplication.class), any(JsonEnvelope.class), eq(false))).thenReturn(mock(ApplicantDetails.class));
        final JsonObject mockApplicantDetailsJson = mock(JsonObject.class);

        when(objectToJsonObjectConverter.convert(any()))
                .thenReturn(mockApplicationDetailsJson)
                .thenReturn(Json.createObjectBuilder().add("prosecutionCaseId", prosecutionCaseId.toString()).build())
                .thenReturn(mockApplicantDetailsJson);

        final ProsecutionCase prosecutionCaseMock = mock(ProsecutionCase.class);
        when(applicationAtAGlanceHelper.getProsecutionCase(eq(prosecutionCaseId))).thenReturn(prosecutionCaseMock);
        when(defenceQueryService.isUserOnlyDefendingCase(jsonEnvelope, prosecutionCaseId.toString())).thenReturn(false);

        final JsonEnvelope response = applicationQueryView.getCourtApplicationForApplicationAtAGlance(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getString("applicationId"), is(applicationId.toString()));
        assertThat(response.payloadAsJsonObject().getJsonObject("applicationDetails"), is(mockApplicationDetailsJson));
        assertThat(response.payloadAsJsonObject().getJsonObject("applicantDetails"), is(mockApplicantDetailsJson));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedApplications").size(), is(1));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedCases").size(), is(1));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedCases").getJsonObject(0), is(notNullValue()));
        verify(prosecutionCaseMock, atMostOnce()).getCaseStatus();
        verify(prosecutionCaseMock, atMostOnce()).getInitiationCode();
        verify(prosecutionCaseMock, atMostOnce()).getInitiationCode();
        verify(applicationDetailsMock, atMostOnce()).getLinkType();
    }



    @Test
    public void shouldGetApplicationAtAGlanceWithChildApplication() {
        final UUID applicationId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final JsonObject jsonObject = createObjectBuilder()
                .add("applicationId", applicationId.toString()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.query.application.aaag").build(),
                jsonObject);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload("{\"id\": \"9aec6dcc-564c-11ea-8e2d-0242ac130003\"}");
        courtApplicationEntity.setParentApplicationId(randomUUID());

        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence()
                .withId(randomUUID())
                .build());

        final CourtApplicationCase courtApplicationCase = CourtApplicationCase.courtApplicationCase()
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(offences)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .build();

        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(courtApplicationEntity);

        final CourtApplicationEntity childCourtApplicationEntity = new CourtApplicationEntity();
        when(courtApplicationRepository.findByParentApplicationId(any())).thenReturn(singletonList(childCourtApplicationEntity));

        when(stringToJsonObjectConverter.convert(any())).thenReturn(applicationJson);
        CourtApplication courtApplication = mock(CourtApplication.class);
        CourtApplication childCourtApplication = mock(CourtApplication.class);
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(courtApplication, childCourtApplication);
        when(courtApplication.getCourtApplicationCases()).thenReturn(singletonList(courtApplicationCase));
        final UUID parentApplicationId = randomUUID();
        when(courtApplication.getParentApplicationId()).thenReturn(parentApplicationId);
        when(childCourtApplication.getId()).thenReturn(UUID.randomUUID());
        when(childCourtApplication.getApplicant()).thenReturn(getCourtApplicant());
        when(courtApplicationRepository.findByApplicationId(parentApplicationId)).thenReturn(courtApplicationEntity);

        when(applicationAtAGlanceHelper.getApplicationDetails(any(CourtApplication.class))).thenReturn(mock(ApplicationDetails.class));
        final JsonObject mockApplicationDetailsJson = mock(JsonObject.class);

        when(applicationAtAGlanceHelper.getApplicantDetails(any(CourtApplication.class), any(JsonEnvelope.class), eq(false))).thenReturn(mock(ApplicantDetails.class));
        final JsonObject mockApplicantDetailsJson = mock(JsonObject.class);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(mockApplicationDetailsJson).thenReturn(mockApplicantDetailsJson);

        final JsonEnvelope response = applicationQueryView.getCourtApplicationForApplicationAtAGlance(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getString("applicationId"), is(applicationId.toString()));
        assertThat(response.payloadAsJsonObject().getJsonObject("applicationDetails"), is(mockApplicationDetailsJson));
        assertThat(response.payloadAsJsonObject().getJsonObject("applicantDetails"), is(mockApplicantDetailsJson));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedApplications").size(), is(1));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedCases").size(), is(1));
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedCases").getJsonObject(0), is(notNullValue()));
        assertThat(response.payloadAsJsonObject().getJsonObject("parentApplication"), is(notNullValue()));
    }

    @Test
    void testGetApplicationForLaa() throws IOException {
        final UUID hearingId = randomUUID();

        final JsonObject incomingPayload = createObjectBuilder()
                .add("applicationId", APPLICATION_ID.toString()).build();
        final ApplicationLaa applicationLaa = jsonToObjectConverter.convert(FileUtil.getJsonPayload("stub-data/mock-laa-query-response.json"), ApplicationLaa.class);

        final JsonObject courtApplicationJson = createObjectBuilder().build();
        final CourtApplication courtApplication = getCourtApplication();

        final JsonEnvelope incomingEnvelope = mock(JsonEnvelope.class);
        final CourtApplicationEntity courtApplicationEntity = mock(CourtApplicationEntity.class);
        final HearingApplicationEntity hearingApplicationEntity = mock(HearingApplicationEntity.class);
        final HearingEntity hearingEntity = mock(HearingEntity.class);

        when(hearingApplicationEntity.getId()).thenReturn(new HearingApplicationKey(APPLICATION_ID, hearingId));
        when(hearingApplicationEntity.getHearing()).thenReturn(hearingEntity);
        when(hearingEntity.getPayload()).thenReturn("{}");
        when(incomingEnvelope.payloadAsJsonObject()).thenReturn(incomingPayload);
        when(courtApplicationEntity.getPayload()).thenReturn("{}");
        when(courtApplicationRepository.findByApplicationId(APPLICATION_ID)).thenReturn(courtApplicationEntity);
        when(hearingApplicationRepository.findByApplicationId(APPLICATION_ID)).thenReturn(singletonList(hearingApplicationEntity));
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(courtApplicationJson);
        when(jsonObjectToObjectConverter.convert(courtApplicationJson, CourtApplication.class)).thenReturn(courtApplication);

        // Mock system user and mapper client
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(SYSTEM_USER_ID));
        final SystemIdMapping mapping = new SystemIdMapping(SYSTEM_USER_ID, LAA_APPLICATION_SHORTID, SOURCE_TYPE_APPLICATION, APPLICATION_ID, TARGET_TYPE_APPLICATION, null);
        when(systemIdMapperClient.findBy(APPLICATION_ID, TARGET_TYPE_APPLICATION, SYSTEM_USER_ID))
                .thenReturn(Optional.of(mapping));

        lenient().when(applicationLaaConverter.convert(any(CourtApplication.class), anyList(), eq(LAA_APPLICATION_SHORTID))).thenReturn(applicationLaa);

        final Envelope<ApplicationLaa> result = applicationQueryView.getApplicationForLaa(incomingEnvelope);

        assertNotNull(result, "Result envelope should not be null");
        assertNotNull(result.payload(), "Result payload should not be null");
        assertEquals(applicationLaa, result.payload());

        verify(courtApplicationRepository).findByApplicationId(APPLICATION_ID);
        verify(hearingApplicationRepository).findByApplicationId(APPLICATION_ID);
        verify(applicationLaaConverter).convert(any(CourtApplication.class), anyList(), eq(LAA_APPLICATION_SHORTID));
        verify(jsonObjectToObjectConverter).convert(courtApplicationJson, CourtApplication.class);
        verify(systemUserProvider).getContextSystemUserId();
        verify(systemIdMapperClient).findBy(APPLICATION_ID, TARGET_TYPE_APPLICATION, SYSTEM_USER_ID);

        validateObjectAgainstSchema(objectToJsonConverter.convert(result.payload()), "progression.query.application-laa-response-schema.json");
    }

    @Test
    void shouldReturnLaaApplicationShortIdWhenExists() {
        final UUID applicationId = randomUUID();
        final String expectedLaaId = "A23ABCDEFGH";

        final SystemIdMapping mapping = new SystemIdMapping(
                SYSTEM_USER_ID,
                expectedLaaId,
                SOURCE_TYPE_APPLICATION,
                applicationId,
                TARGET_TYPE_APPLICATION,
                null
        );

        when(systemIdMapperClient.findBy(applicationId, TARGET_TYPE_APPLICATION, SYSTEM_USER_ID))
                .thenReturn(Optional.of(mapping));

        final String result = applicationQueryView.retrieveLaaApplicationShortIdFromSystemIdMapper(applicationId);

        assertNotNull(result);
        assertEquals(expectedLaaId, result);
    }

    @Test
    void shouldReturnEmptyWhenLaaApplicationShortIdNotFound() {
        final UUID applicationId = randomUUID();

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(SYSTEM_USER_ID));
        when(systemIdMapperClient.findBy(applicationId, TARGET_TYPE_APPLICATION, SYSTEM_USER_ID))
                .thenReturn(Optional.empty());

        assertThat(applicationQueryView.retrieveLaaApplicationShortIdFromSystemIdMapper(applicationId), nullValue());
    }

    @Test
    void getApplicationForLaaShouldReturnEmptyBodyWhenApplicationIdNotFound() throws IOException {
        final JsonObject incomingPayload = createObjectBuilder()
                .add("applicationId", APPLICATION_ID.toString()).build();

        final JsonEnvelope incomingEnvelope = mock(JsonEnvelope.class);

        when(incomingEnvelope.payloadAsJsonObject()).thenReturn(incomingPayload);
        when(courtApplicationRepository.findByApplicationId(APPLICATION_ID)).thenThrow(new NoResultException());

        final Envelope<ApplicationLaa> result = applicationQueryView.getApplicationForLaa(incomingEnvelope);

        assertNotNull(result);
        assertThat(objectToJsonConverter.convert(result.payload()).toString(), is("{}"));
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
        assertEquals(summary0.getAssignedUserId(), courtApplicationEntity.getAssignedUserId());

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

        when(stringToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        when(courtApplicationRepository.findByParentApplicationId(applicationId)).thenReturn(Arrays.asList(courtApplicationEntity));
        when(jsonObjectToObjectConverter.convert(jsonObject, CourtApplication.class)).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);

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

        assertThat(jsonEnvelopeOut.payloadAsJsonObject().getJsonArray("notificationStatus").size(), is(1));
    }

    @Test
    public void shouldGetApplicationHearings() {
        final UUID applicationId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID courtCentreId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID courtCentreId2 = randomUUID();

        final JsonObject hearing1 = createHearingPayload(hearingId1, courtCentreId1);
        final JsonObject hearing2 = createHearingPayload(hearingId2, courtCentreId2);

        when(hearingApplicationRepository.findByApplicationId(applicationId))
                .thenReturn(
                        asList(
                                createHearingApplicationEntity(hearing1),
                                createHearingApplicationEntity(hearing2)
                        ));

        when(stringToJsonObjectConverter.convert(hearing1.toString())).thenReturn(hearing1);
        when(stringToJsonObjectConverter.convert(hearing2.toString())).thenReturn(hearing2);

        final JsonObject jsonObject = createObjectBuilder()
                .add(ApplicationQueryView.APPLICATION_ID_SEARCH_PARAM, applicationId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(randomUUID())
                        .withName("progression.query.applicationhearings").build(),
                jsonObject);

        final JsonEnvelope response = applicationQueryView.getApplicationHearings(envelope);
        final JsonObject payload = response.payloadAsJsonObject();
        final JsonArray hearings = payload.getJsonArray("hearings");
        assertThat(hearings.size(), is(2));

        final Optional<JsonObject> actualHearing1 = hearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId1.toString().equals(h.getString("hearingId")))
                .findFirst();
        assertThat(actualHearing1.isPresent(), is(true));
        final String actualCourtCentre1 = actualHearing1.get().getJsonObject("courtCentre").getString("id");
        assertThat(actualCourtCentre1, is(courtCentreId1.toString()));

        final Optional<JsonObject> actualHearing2 = hearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId2.toString().equals(h.getString("hearingId")))
                .findFirst();
        assertThat(actualHearing2.isPresent(), is(true));
        final String actualCourtCentre2 = actualHearing2.get().getJsonObject("courtCentre").getString("id");
        assertThat(actualCourtCentre2, is(courtCentreId2.toString()));
    }

    private JsonObject createHearingPayload(final UUID hearingId, final UUID courtCentreId) {
        return createObjectBuilder()
                .add("id", hearingId.toString())
                .add("courtCentre", createObjectBuilder()
                        .add("id", courtCentreId.toString())
                        .add("name", "name")
                        .add("roomId", randomUUID().toString())
                        .add("roomName", "Court Room 1")
                ).build();
    }

    private HearingApplicationEntity createHearingApplicationEntity(final JsonObject hearingPayload) {
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setPayload(hearingPayload.toString());

        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey());
        hearingApplicationEntity.setHearing(hearingEntity);
        return hearingApplicationEntity;
    }

    @Test
    public void shouldGetCourtProceedingsForApplication() {
        final UUID applicationId = UUID.randomUUID();

        final JsonObject jsonObject = createObjectBuilder()
                .add(ApplicationQueryView.APPLICATION_ID_SEARCH_PARAM, applicationId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = envelopeFrom(
                metadataBuilder().withId(randomUUID())
                        .withName("progression.query.court-proceedings-for-application").build(),
                jsonObject);
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = new InitiateCourtApplicationEntity();
        final JsonObject jsonObject1 = createObjectBuilder().add("courtApplication", createObjectBuilder().add("id", randomUUID().toString()).build()).build();
        initiateCourtApplicationEntity.setPayload(jsonObject1.toString());

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        final JsonObject jsonObject2 = createObjectBuilder().add("courtApplication", createObjectBuilder().add("id", randomUUID().toString()).build()).build();
        courtApplicationEntity.setPayload(jsonObject2.toString());

        when(initiateCourtApplicationRepository.findBy(applicationId)).thenReturn(initiateCourtApplicationEntity);
        when(courtApplicationRepository.findByParentApplicationId(applicationId)).thenReturn(List.of(courtApplicationEntity));
        when(stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload())).thenReturn(jsonObject1);

        final JsonEnvelope jsonEnvelopeOut = applicationQueryView.getCourtProceedingsForApplication(jsonEnvelopeIn);
        assertThat(jsonEnvelopeOut.payloadAsJsonObject().getJsonObject("courtApplication"), is(notNullValue()));
        assertThat(jsonEnvelopeOut.payloadAsJsonObject().getJsonArray("linkedApplications"), is(notNullValue()));
    }

    @Test
    public void shouldGetCaseStatusForApplicationID() {
        final UUID applicationId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();

        final JsonObject jsonObject = createObjectBuilder()
                .add(ApplicationQueryView.APPLICATION_ID_SEARCH_PARAM, applicationId.toString())
                .add(ApplicationQueryView.CASEID_SEARCH_PARAM, caseId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = envelopeFrom(
                metadataBuilder().withId(randomUUID())
                        .withName("progression.query.case.status-for-application").build(),
                jsonObject);
        final JsonObject jsonObject1 = createObjectBuilder().add("caseStatus", INACTIVE).build();

        when(courtApplicationCaseRepository.findCaseStatusByApplicationId(applicationId, caseId)).thenReturn(INACTIVE);
        when(stringToJsonObjectConverter.convert(INACTIVE)).thenReturn(jsonObject1);

        final JsonEnvelope jsonEnvelopeOut = applicationQueryView.getCaseStatusForApplication(jsonEnvelopeIn);
        assertThat(jsonEnvelopeOut.payloadAsJsonObject().getString("caseStatus"), is(notNullValue()));
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
                .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person()
                        .withFirstName("firstName").withMiddleName("middleName").withLastName("lastName").build()).build()).build()).build();
    }

}


