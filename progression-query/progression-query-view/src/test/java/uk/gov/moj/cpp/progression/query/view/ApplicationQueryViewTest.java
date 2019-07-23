package uk.gov.moj.cpp.progression.query.view;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
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

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ApplicationQueryViewTest {

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

    final static private UUID APPLICATION_ID = UUID.randomUUID();
    final static private String APPLICATION_ARN = new StringGenerator().next();
    final static private String APPLICANT_FIRST_NAME = new StringGenerator().next();
    final static private String APPLICANT_LAST_NAME = new StringGenerator().next();
    final static private String APPLICANT_MIDDLE_NAME = new StringGenerator().next();
    final static private String RESPONDENTS_ORG_NAME = new StringGenerator().next();
    final static private String APPLICATION_PROSECUTOR_NAME = new StringGenerator().next();

    @Test
    public void shouldFindApplicationById() {
        final UUID applicationId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("applicationId", applicationId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.application").build(),
                jsonObject);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload("{}");

        HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey());
        hearingApplicationEntity.setHearing(new HearingEntity());

        CourtDocument courtDocument = CourtDocument.courtDocument()
                .withIsRemoved(false)
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
        assertThat(response.payloadAsJsonObject().get("courtDocuments"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("linkedApplicationsSummary"), notNullValue());
    }

    @Test
    public void shouldReturnApplicationSummary() {
        final UUID applicationId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("applicationId", applicationId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.application.summary").build(),
                jsonObject);

        CourtApplication courtApplication = getCourtApplication();
        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setApplicationId(APPLICATION_ID);
        courtApplicationEntity.setPayload("{}");

        when(courtApplicationRepository.findByParentApplicationId(applicationId)).thenReturn(Arrays.asList(courtApplicationEntity));
        when(jsonObjectToObjectConverter.convert(this.applicationJson, CourtApplication.class)).thenReturn(courtApplication);
        final JsonEnvelope response = applicationQueryView.getApplicationSummary(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("courtApplications"), notNullValue());
    }

    @Test
    public void shouldGetCourtDocumentNotificationStatus() {
        final UUID applicationId = UUID.randomUUID();

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(ApplicationQueryView.APPLICATION_ID_SEARCH_PARAM, applicationId.toString())
                .build();

        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName("progression.query.application.notification-status").build(),
                jsonObject);

        final NotificationStatusEntity notificationStatusEntity = notificationStatusEntity(applicationId);

        when(notificationStatusRepository.findByApplicationId(applicationId)).thenReturn(asList(notificationStatusEntity));

        final JsonEnvelope jsonEnvelopeOut = applicationQueryView.getApplicationNotifications(jsonEnvelopeIn);

        System.out.println(jsonEnvelopeOut.payloadAsJsonObject());

        assertThat(jsonEnvelopeOut.payloadAsJsonObject().getJsonArray("notificationStatus").size(), is(1));
    }

    private NotificationStatusEntity notificationStatusEntity(final UUID applicationId) {
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

    private ProsecutionCase createProsecutionCase(UUID caseId) {
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

        JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);
        prosecutionCaseEntity.setPayload(prosecutionCaseJson.toString());

        return prosecutionCaseEntity;
    }

    private List<Defendant> createDefendants() {
        List<Defendant> defendants = new ArrayList<>();
        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withPersonDefendant(createPersonDefendant())
                .withOffences(createOffences())
                .build();
        defendants.add(defendant);
        return defendants;
    }

    private PersonDefendant createPersonDefendant() {
        return PersonDefendant.personDefendant()
                .withPersonDetails(Person.person()
                        .withFirstName("John")
                        .withMiddleName("Martin")
                        .withLastName("Williams")
                        .withDateOfBirth(LocalDate.of(2017, 02, 01))
                        .build())
                .build();
    }

    private List<Offence> createOffences() {
        Offence offence = Offence.offence()
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

    private CourtApplication getCourtApplication() {
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
}


