package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsOrganisations;
import uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsWithOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.Defendants;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"squid:S1607"})
public class DefenceNotificationServiceTest {
    private static final UUID CASE_ID = UUID.randomUUID();

    /*
     * Defendant - AssociatedOrganisation
     * ----------------------------------
     * DEFENDANT1 - ORG1
     * DEFENDANT2 - ORG1
     * DEFENDANT3 - ORG2 (not exists in defendantDocument)
     * DEFENDANT4 - null
     * */
    private static final UUID ORG1 = UUID.randomUUID();
    private static final UUID ORG2 = UUID.randomUUID();
    private static final UUID DEFENDANT1 = UUID.randomUUID();
    private static final UUID DEFENDANT2 = UUID.randomUUID();
    private static final UUID DEFENDANT3 = UUID.randomUUID();
    private static final UUID DEFENDANT4 = UUID.randomUUID();

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private DefenceService defenceService;

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DefenceNotificationService defenceNotificationService;


    @Captor
    private ArgumentCaptor<JsonEnvelope> sourceEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<UUID> notificationIdCaptor;

    @Captor
    private ArgumentCaptor<UUID> caseIdCaptor;

    @Captor
    private ArgumentCaptor<UUID> applicationIdCaptor;

    @Captor
    private ArgumentCaptor<UUID> materialIdCaptor;

    @Captor
    private ArgumentCaptor<List<EmailChannel>> emailNotificationsCaptor;

    @Captor
    private ArgumentCaptor<String> materialUrlCaptor;

    @Before
    public void setUp() {
    }

    @Test
    public void shouldPrepareNotificationsForDefendantDocument() {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonObject courtDocumentPayload = buildDocumentCategoryWithDefendantDocument("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);

        final CourtDocument courtDocument = mapper.convertValue(requestMessage.payloadAsJsonObject().getJsonObject("courtDocument"), CourtDocument.class);
        final CourtsDocumentAdded courtsDocumentAdded = CourtsDocumentAdded.courtsDocumentAdded().withCourtDocument(courtDocument).build();

        final List<Defendants> defendants = Arrays.asList(
                new Defendants(ORG1, DEFENDANT1), new Defendants(ORG1, DEFENDANT2),
                new Defendants(ORG2, DEFENDANT3), new Defendants(null, DEFENDANT4)
        );
        final CaseDefendantsWithOrganisation caseDefendantsWithOrg = CaseDefendantsWithOrganisation.caseDefendantsWithOrganisation()
                .withCaseId(CASE_ID)
                .withUrn("urn-123456")
                .withDefendants(defendants)
                .build();
        final CaseDefendantsOrganisations caseDefendantsOrg = CaseDefendantsOrganisations.caseDefendantsOrganisations()
                .withCaseDefendantOrganisation(caseDefendantsWithOrg)
                .build();
        final List<String> emails = Collections.singletonList("email@abc.com");

        when(defenceService.getDefendantsAndAssociatedOrganisationsForCase(requestMessage, CASE_ID.toString())).thenReturn(caseDefendantsOrg);
        when(usersGroupService.getEmailsForOrganisationIds(requestMessage, Collections.singletonList(ORG1.toString()))).thenReturn(emails);
        when(applicationParameters.getNotifyDefenceOfNewMaterialTemplateId()).thenReturn(UUID.randomUUID().toString());
        when(applicationParameters.getEndClientHost()).thenReturn("EndClientHost");

        defenceNotificationService.prepareNotificationsForCourtDocument(requestMessage, courtsDocumentAdded.getCourtDocument());
        verify(notificationService, times(1)).sendEmail(
                sourceEnvelopeCaptor.capture(), notificationIdCaptor.capture(),
                caseIdCaptor.capture(), applicationIdCaptor.capture(), materialIdCaptor.capture(),
                emailNotificationsCaptor.capture(), materialUrlCaptor.capture());

        assertThat(caseIdCaptor.getValue(), is(CASE_ID));
        assertThat(applicationIdCaptor.getValue(), Matchers.nullValue());
        assertThat(materialIdCaptor.getValue(), Matchers.nullValue());
        assertThat(emailNotificationsCaptor.getValue(), Matchers.notNullValue());
        assertThat(emailNotificationsCaptor.getValue().size(), is(1));
        assertThat(emailNotificationsCaptor.getValue().get(0).getSendToAddress(), is("email@abc.com"));
        assertThat(materialUrlCaptor.getValue(), Matchers.nullValue());
    }

    @Test
    public void shouldPrepareNotificationsForCaseDocument() {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonObject courtDocumentPayload = buildDocumentCategoryWithCaseDocument("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);

        final CourtDocument courtDocument = mapper.convertValue(requestMessage.payloadAsJsonObject().getJsonObject("courtDocument"), CourtDocument.class);
        final CourtsDocumentAdded courtsDocumentAdded = CourtsDocumentAdded.courtsDocumentAdded().withCourtDocument(courtDocument).build();

        final List<Defendants> defendants = Arrays.asList(
                new Defendants(ORG1, DEFENDANT1), new Defendants(ORG1, DEFENDANT2),
                new Defendants(ORG2, DEFENDANT3), new Defendants(null, DEFENDANT4)
        );
        final CaseDefendantsWithOrganisation caseDefendantsWithOrg = CaseDefendantsWithOrganisation.caseDefendantsWithOrganisation()
                .withCaseId(CASE_ID)
                .withUrn("urn-123456")
                .withDefendants(defendants)
                .build();
        final CaseDefendantsOrganisations caseDefendantsOrg = CaseDefendantsOrganisations.caseDefendantsOrganisations()
                .withCaseDefendantOrganisation(caseDefendantsWithOrg)
                .build();
        final List<String> emails = Arrays.asList("email1@abc.com", "email2@abc.com");

        when(defenceService.getDefendantsAndAssociatedOrganisationsForCase(requestMessage, CASE_ID.toString())).thenReturn(caseDefendantsOrg);
        when(usersGroupService.getEmailsForOrganisationIds(requestMessage, Arrays.asList(ORG1.toString(), ORG2.toString()))).thenReturn(emails);
        when(applicationParameters.getNotifyDefenceOfNewMaterialTemplateId()).thenReturn(UUID.randomUUID().toString());
        when(applicationParameters.getEndClientHost()).thenReturn("EndClientHost");

        defenceNotificationService.prepareNotificationsForCourtDocument(requestMessage, courtsDocumentAdded.getCourtDocument());
        verify(notificationService, times(1)).sendEmail(
                sourceEnvelopeCaptor.capture(), notificationIdCaptor.capture(),
                caseIdCaptor.capture(), applicationIdCaptor.capture(), materialIdCaptor.capture(),
                emailNotificationsCaptor.capture(), materialUrlCaptor.capture());

        assertThat(caseIdCaptor.getValue(), is(CASE_ID));
        assertThat(applicationIdCaptor.getValue(), Matchers.nullValue());
        assertThat(materialIdCaptor.getValue(), Matchers.nullValue());
        assertThat(emailNotificationsCaptor.getValue(), Matchers.notNullValue());
        assertThat(emailNotificationsCaptor.getValue().size(), is(2));
        assertThat(emailNotificationsCaptor.getValue().get(0).getSendToAddress(), is("email1@abc.com"));
        assertThat(emailNotificationsCaptor.getValue().get(1).getSendToAddress(), is("email2@abc.com"));
        assertThat(materialUrlCaptor.getValue(), Matchers.nullValue());
    }

    @Test
    public void shouldSkipNotificationsWhenNoDefendants() {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonObject courtDocumentPayload = buildDocumentCategoryWith2Documents("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);

        final CourtDocument courtDocument = mapper.convertValue(requestMessage.payloadAsJsonObject().getJsonObject("courtDocument"), CourtDocument.class);
        final CourtsDocumentAdded courtsDocumentAdded = CourtsDocumentAdded.courtsDocumentAdded().withCourtDocument(courtDocument).build();

        final List<Defendants> defendants = new ArrayList<>();
        final CaseDefendantsWithOrganisation caseDefendantsWithOrg = CaseDefendantsWithOrganisation.caseDefendantsWithOrganisation()
                .withCaseId(CASE_ID)
                .withUrn("urn-123456")
                .withDefendants(defendants)
                .build();
        final CaseDefendantsOrganisations caseDefendantsOrg = CaseDefendantsOrganisations.caseDefendantsOrganisations()
                .withCaseDefendantOrganisation(caseDefendantsWithOrg)
                .build();

        when(defenceService.getDefendantsAndAssociatedOrganisationsForCase(requestMessage, CASE_ID.toString())).thenReturn(caseDefendantsOrg);
        when(applicationParameters.getNotifyDefenceOfNewMaterialTemplateId()).thenReturn(UUID.randomUUID().toString());

        defenceNotificationService.prepareNotificationsForCourtDocument(requestMessage, courtsDocumentAdded.getCourtDocument());
        verify(usersGroupService, never()).getEmailsForOrganisationIds(Mockito.any(), Mockito.anyListOf(String.class));
        verify(notificationService, never()).sendEmail(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyListOf(EmailChannel.class), Mockito.any());
    }

    private static JsonObject buildDefendantDocument() {
        return createObjectBuilder().add("defendantDocument",
                createObjectBuilder()
                        .add("prosecutionCaseId", CASE_ID.toString())
                        .add("defendants", createArrayBuilder()
                                .add(DEFENDANT1.toString())
                                .add(DEFENDANT2.toString())
                                .add(DEFENDANT4.toString())))
                .build();
    }

    private static JsonObject buildCaseDocument() {
        return createObjectBuilder().add("caseDocument",
                createObjectBuilder()
                        .add("prosecutionCaseId", CASE_ID.toString()))
                .build();
    }

    private static JsonObject buildCompositeDocument() {
        return createObjectBuilder()
                .add("defendantDocument",
                        createObjectBuilder()
                                .add("prosecutionCaseId", CASE_ID.toString())
                                .add("defendants", createArrayBuilder()
                                        .add(DEFENDANT1.toString())
                                        .add(DEFENDANT2.toString())
                                        .add(DEFENDANT4.toString())))
                .add("caseDocument",
                        createObjectBuilder()
                                .add("prosecutionCaseId", CASE_ID.toString()))
                .build();
    }

    private static JsonObjectBuilder buildCourtDocument() {
        return createObjectBuilder()
                .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                .add("name", "SJP Notice")
                .add("documentTypeDescription", "SJP Notice")
                .add("mimeType", "pdf")
                .add("materials", createArrayBuilder().add(buildMaterial()))
                .add("containsFinancialMeans", false)
                .add("documentTypeRBAC", buildDocumentTypeDataWithRBAC());
    }

    private static JsonObject buildDocumentCategoryWithDefendantDocument(String documentTypeId) {
        final JsonObject documentCategory = buildDefendantDocument();

        return createObjectBuilder().add("courtDocument",
                buildCourtDocument()
                        .add("documentCategory", documentCategory)
                        .add("documentTypeId", documentTypeId))
                .build();
    }

    private static JsonObject buildDocumentCategoryWithCaseDocument(String documentTypeId) {
        final JsonObject documentCategory = buildCaseDocument();

        return createObjectBuilder().add("courtDocument",
                buildCourtDocument()
                        .add("documentCategory", documentCategory)
                        .add("documentTypeId", documentTypeId))
                .build();
    }

    private static JsonObject buildDocumentCategoryWith2Documents(String documentTypeId) {
        final JsonObject documentCategory = buildCompositeDocument();

        return createObjectBuilder().add("courtDocument",
                buildCourtDocument()
                        .add("documentCategory", documentCategory)
                        .add("documentTypeId", documentTypeId))
                .build();
    }

    private static JsonObject buildMaterial() {
        return createObjectBuilder().add("id", "5e1cc18c-76dc-47dd-99c1-d6f87385edf1").add("receivedDateTime", ZonedDateTime.now().toString()).build();
    }

    private static JsonObject buildDocumentTypeDataWithRBAC() {
        return Json.createObjectBuilder()
                .add("documentAccess", Json.createArrayBuilder().add("Listing Officer"))
                .add("canCreateUserGroups", Json.createArrayBuilder().add("Listing Officer"))
                .add("canReadUserGroups", Json.createArrayBuilder().add("Listing Officer").add("Magistrates"))
                .add("canDownloadUserGroups", Json.createArrayBuilder().add("Listing Officer").add("Magistrates"))
                .build();
    }

}