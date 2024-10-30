package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsOrganisations;
import uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsWithOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.Defendants;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    private static final UUID DEFENDANT5 = UUID.randomUUID();

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private DefenceService defenceService;

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DefenceNotificationService defenceNotificationService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> sourceEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<UUID> notificationIdCaptor;

    @Captor
    private ArgumentCaptor<UUID> caseIdCaptor;

    @Captor
    private ArgumentCaptor<UUID> materialIdCaptor;

    @Captor
    private ArgumentCaptor<String> urnCaptor;

    @Captor
    private ArgumentCaptor<HashMap<Defendants, String>> defendantAndRelatedOrganisationEmailCaptor;

    @Captor
    private ArgumentCaptor<String> documentSectionCaptor;

    @Captor
    private ArgumentCaptor<String> documentNameCaptor;

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void shouldPrepareNotificationsForDefendantDocument() {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonObject courtDocumentPayload = buildDocumentCategoryWithDefendantDocument("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);
        final String documentSection = "Case Summary";
        final String documentName = "Smith John 53NP7934321.pdf";

        final CourtDocument courtDocument = mapper.convertValue(requestMessage.payloadAsJsonObject().getJsonObject("courtDocument"), CourtDocument.class);
        final CourtsDocumentAdded courtsDocumentAdded = CourtsDocumentAdded.courtsDocumentAdded().withCourtDocument(courtDocument).build();

        final List<Defendants> defendants = Arrays.asList(
                new Defendants(ORG1, DEFENDANT1, "defendantFirstName1", "defendantLastName1", "organisationName1"),
                new Defendants(ORG1, DEFENDANT2, "defendantFirstName2", "defendantLastName2", "organisationName1"),
                new Defendants(ORG2, DEFENDANT3, "defendantFirstName3", "defendantLastName3", "organisationName2"),
                new Defendants(ORG1, DEFENDANT4, "defendantFirstName4", "defendantLastName4", "organisationName1"),
                new Defendants(ORG2, DEFENDANT5, "defendantFirstName5", "defendantLastName5", "organisationName2")
        );

        final List<String> organisationIds = new ArrayList<>();
        organisationIds.add(ORG1.toString());
        organisationIds.add(ORG2.toString());

        final String urn = "urn-123456";
        final CaseDefendantsWithOrganisation caseDefendantsWithOrg =
                CaseDefendantsWithOrganisation.caseDefendantsWithOrganisation()
                        .withCaseId(CASE_ID)
                        .withUrn(urn)
                        .withDefendants(defendants)
                        .build();
        final CaseDefendantsOrganisations caseDefendantsOrg = CaseDefendantsOrganisations.caseDefendantsOrganisations()
                .withCaseDefendantOrganisation(caseDefendantsWithOrg)
                .build();

        final HashMap<String, String> emailOrganisationIds = new HashMap<>();
        emailOrganisationIds.put(ORG1.toString(), "email1");
        emailOrganisationIds.put(ORG2.toString(), "email2");

        when(usersGroupService.getEmailsForOrganisationIds(any(), any())).thenReturn(emailOrganisationIds);
        when(defenceService.getDefendantsAndAssociatedOrganisationsForCase(requestMessage, CASE_ID.toString())).thenReturn(caseDefendantsOrg);

        defenceNotificationService.prepareNotificationsForCourtDocument(requestMessage, courtsDocumentAdded.getCourtDocument(), documentSection, documentName);
        final UUID materialId = courtDocument.getMaterials().get(0).getId();

        verify(emailService, times(2))
                .sendEmailNotifications(sourceEnvelopeCaptor.capture(),
                        materialIdCaptor.capture(),
                        urnCaptor.capture(),
                        caseIdCaptor.capture(),
                        defendantAndRelatedOrganisationEmailCaptor.capture(), documentSectionCaptor.capture(), documentNameCaptor.capture());
        verify(usersGroupService, times(2)).getEmailsForOrganisationIds(any(), Mockito.anyList());

        assertThat(materialIdCaptor.getValue(), is(materialId));
        assertThat(urnCaptor.getValue(), is(urn));
        assertThat(caseIdCaptor.getValue(), is(CASE_ID));
    }

    @Test
    public void shouldPrepareNotificationsForCaseDocument() {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonObject courtDocumentPayload = buildDocumentCategoryWithCaseDocument("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);
        final String documentSection = "Case Summary";
        final String documentName = "Smith John 53NP7934321.pdf";

        final CourtDocument courtDocument = mapper.convertValue(requestMessage.payloadAsJsonObject().getJsonObject("courtDocument"), CourtDocument.class);
        final CourtsDocumentAdded courtsDocumentAdded = CourtsDocumentAdded.courtsDocumentAdded().withCourtDocument(courtDocument).build();

        final List<Defendants> defendants = Arrays.asList(
                new Defendants(ORG1, DEFENDANT1, "defendantFirstName1", "defendantLastName1", "organisationName1"),
                new Defendants(ORG1, DEFENDANT2, "defendantFirstName2", "defendantLastName2", "organisationName1"),
                new Defendants(ORG2, DEFENDANT3, "defendantFirstName3", "defendantLastName3", "organisationName2"),
                new Defendants(null, DEFENDANT4, "defendantFirstName4", "defendantLastName4", null)
        );

        final List<String> organisationIds = new ArrayList<>();
        organisationIds.add(ORG1.toString());
        organisationIds.add(ORG2.toString());

        final String urn = "urn-123456";
        final CaseDefendantsWithOrganisation caseDefendantsWithOrg = CaseDefendantsWithOrganisation.caseDefendantsWithOrganisation()
                .withCaseId(CASE_ID)
                .withUrn(urn)
                .withDefendants(defendants)
                .build();
        final CaseDefendantsOrganisations caseDefendantsOrg = CaseDefendantsOrganisations.caseDefendantsOrganisations()
                .withCaseDefendantOrganisation(caseDefendantsWithOrg)
                .build();

        final HashMap<String, String> emailOrganisationIds = new HashMap<>();
        emailOrganisationIds.put(ORG1.toString(), "email1");
        emailOrganisationIds.put(ORG2.toString(), "email2");


        when(usersGroupService.getEmailsForOrganisationIds(any(), any())).thenReturn(emailOrganisationIds);
        when(defenceService.getDefendantsAndAssociatedOrganisationsForCase(requestMessage, CASE_ID.toString())).thenReturn(caseDefendantsOrg);


        final HashMap<Defendants, String> defendantAndRelatedOrganisationEmailL = new HashMap<>();
        defendantAndRelatedOrganisationEmailL.put(Defendants.defendants().withDefendantId(DEFENDANT1).build(), "email1");
        defendantAndRelatedOrganisationEmailL.put(Defendants.defendants().withDefendantId(DEFENDANT2).build(), "email1");
        defendantAndRelatedOrganisationEmailL.put(Defendants.defendants().withDefendantId(DEFENDANT3).build(), "email2");

        defenceNotificationService.prepareNotificationsForCourtDocument(requestMessage, courtsDocumentAdded.getCourtDocument(), documentSection, documentName);
        final UUID materialId = courtDocument.getMaterials().get(0).getId();

        verify(emailService, times(1))
                .sendEmailNotifications(sourceEnvelopeCaptor.capture(),
                        materialIdCaptor.capture(),
                        urnCaptor.capture(), caseIdCaptor.capture(), defendantAndRelatedOrganisationEmailCaptor.capture(), documentSectionCaptor.capture(), documentNameCaptor.capture());
        verify(usersGroupService, times(1)).getEmailsForOrganisationIds(any(), Mockito.anyList());

        assertThat(materialIdCaptor.getValue(), is(materialId));
        assertThat(urnCaptor.getValue(), is(urn));
        assertThat(caseIdCaptor.getValue(), is(CASE_ID));
        assertThat(defendantAndRelatedOrganisationEmailCaptor.getValue(), is(defendantAndRelatedOrganisationEmailL));

    }

    @Test
    public void shouldSkipNotificationsWhenNoDefendants() {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonObject courtDocumentPayload = buildDocumentCategoryWith2Documents("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);
        final String documentSection = "Case Summary";
        final String documentName = "Smith John 53NP7934321.pdf";

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

        defenceNotificationService.prepareNotificationsForCourtDocument(requestMessage, courtsDocumentAdded.getCourtDocument(), documentSection, documentName);
        verify(emailService, never()).sendEmailNotifications(any(), any(), any(), any(), Mockito.anyMap(), any(), any());
        verify(usersGroupService, never()).getEmailsForOrganisationIds(any(), Mockito.anyList());
    }

    private static JsonObject buildDefendantDocument() {
        return createObjectBuilder().add("defendantDocument",
                createObjectBuilder()
                        .add("prosecutionCaseId", CASE_ID.toString())
                        .add("defendants", createArrayBuilder()
                                .add(DEFENDANT1.toString())
                                .add(DEFENDANT2.toString())
                        ))
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