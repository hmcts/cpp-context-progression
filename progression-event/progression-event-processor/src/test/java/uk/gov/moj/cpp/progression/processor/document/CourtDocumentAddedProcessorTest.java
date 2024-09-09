package uk.gov.moj.cpp.progression.processor.document;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentAddedProcessor.FEATURE_DEFENCE_DISCLOSURE;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentAddedProcessor.PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentAddedProcessor.PROGRESSION_COMMAND_UPDATE_CASE_FOR_CPS_PROSECUTOR;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentAddedProcessor.PUBLIC_COURT_DOCUMENT_ADDED;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentAddedProcessor.PUBLIC_DOCUMENT_ADDED;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentAddedProcessor.PUBLIC_IDPC_COURT_DOCUMENT_RECEIVED;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.CpsRestNotificationService;
import uk.gov.moj.cpp.progression.service.DefenceNotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.service.payloads.UserGroupDetails;
import uk.gov.moj.cpp.progression.transformer.CourtDocumentTransformer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentAddedProcessorTest {

    private static final UUID DOCUMENT_TYPE_ID = UUID.fromString("41be14e8-9df5-4b08-80b0-1e670bc80a5b");


    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @InjectMocks
    private CourtDocumentAddedProcessor eventProcessor;
    @Mock
    private Requester requester;
    @Mock
    private Sender sender;
    @Mock
    private UsersGroupService usersGroupService;
    @Mock
    private DefenceNotificationService defenceNotificationService;
    @Mock
    private ProgressionService progressionService;
    @Mock
    private RefDataService referenceDataService;
    @Mock
    private FeatureControlGuard featureControlGuard;
    @Mock
    private CourtDocumentTransformer courtDocumentTransformer;
    @Mock
    private CpsRestNotificationService cpsRestNotificationService;
    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private static JsonObject buildDefendantDocument() {
        return createObjectBuilder().add("defendantDocument",
                createObjectBuilder()
                        .add("prosecutionCaseId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                        .add("defendants", createArrayBuilder().add("e1d32d9d-29ec-4934-a932-22a50f223966"))).build();
    }

    private static JsonObject buildCaseDocument() {
        return createObjectBuilder().add("caseDocument",
                createObjectBuilder()
                        .add("prosecutionCaseId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")).build();
    }

    private static JsonObject buildDocumentAddedWithProsecutionCaseId() {
        return createObjectBuilder().add("prosecutionCase",
                createObjectBuilder()
                        .add("id", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
        ).add("courtDocument",
                createObjectBuilder()
                        .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e28")
                        .add("documentTypeId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e28")
                        .add("name", "SJP Notice")).build();
    }

    private static JsonObject buildDocumentAddedWithoutProsecutionCaseId() {
        return createObjectBuilder().add("courtDocument",
                createObjectBuilder()
                        .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e28")
                        .add("documentTypeId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e28")
                        .add("name", "SJP Notice")).build();
    }


    private static JsonObject buildDocumentCategoryJsonObject(JsonObject documentCategory, String documentTypeId, Boolean isCpsCase) {
        final JsonObjectBuilder courtDocument =
                createObjectBuilder().add("courtDocument",
                        createObjectBuilder()
                                .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e28")
                                .add("documentCategory", documentCategory)
                                .add("name", "SJP Notice")
                                .add("documentTypeId", documentTypeId)
                                .add("documentTypeDescription", "SJP Notice")
                                .add("mimeType", "pdf")
                                .add("materials", createArrayBuilder().add(buildMaterial()))
                                .add("containsFinancialMeans", false)
                                .add("documentTypeRBAC", buildDocumentTypeDataWithRBAC()))
                        .add("isUnbundledDocument", false);
        if (isCpsCase != null) {
            courtDocument.add("isCpsCase", isCpsCase);
        }

        return courtDocument.build();
    }

    private static JsonObject buildMaterial() {
        return createObjectBuilder().add("id", "5e1cc18c-76dc-47dd-99c1-d6f87385edf1").add("receivedDateTime", ZonedDateTime.now().toString()).build();
    }

    private static JsonObject buildDocumentTypeDataWithRBAC() {
        return Json.createObjectBuilder()
                .add("documentAccess", Json.createArrayBuilder().add("Listing Officer"))
                .add("canCreateUserGroups", Json.createArrayBuilder().add("Listing Officer"))
                .add("readUserGroups", Json.createArrayBuilder().add("Listing Officer").add("Magistrates").add("Defence Lawyers"))
                .add("canDownloadUserGroups", Json.createArrayBuilder().add("Listing Officer").add("Magistrates"))
                .build();
    }

    private static JsonObject generateDocumentTypeAccessForApplication(UUID id) {
        return createObjectBuilder()
                .add("id", id.toString())
                .add("section", "Applications")
                .add("notifyDefence", true)
                .build();
    }

    @Before
    public void setUp() {
        final JsonObject jsonObject = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("cpsFlag", true)
                .build();
        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), anyString()))
                .thenReturn(of(getProsecutionCase(true)));
        when(referenceDataService.getProsecutorV2(any(JsonEnvelope.class), any(UUID.class), any(Requester.class))).thenReturn(of(jsonObject));
    }

    @Test
    public void shouldProcessUploadCourtDocumentMessageForDefenceBasedOnNotification() {
        final JsonObject defendantDocumentPayload = buildDocumentCategoryJsonObject(buildDefendantDocument(), DOCUMENT_TYPE_ID.toString(), true);
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                defendantDocumentPayload);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Magistrates"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);
        when(usersGroupService.getGroupIdForDefenceLawyers()).thenReturn(randomUUID().toString());
        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), anyString()))
                .thenReturn(of(getProsecutionCase(false)));
        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(of(generateDocumentTypeAccessForApplication(DOCUMENT_TYPE_ID)));
        defenceNotificationService.prepareNotificationsForCourtDocument(any(), any(CourtDocument.class), anyString(), anyString());
        eventProcessor.handleCourtDocumentAddEvent(requestMessage);

        verify(sender, times(5)).send(envelopeCaptor.capture());
        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        verifyUpdateCpsCommand(commands.get(0), requestMessage);
        verifyCreateCommand(commands.get(1), requestMessage, false);
        verifyPublicCourtDocumentAdded(commands.get(2), requestMessage);
        verifyIDPCommand(commands.get(3));
    }

    @Test
    public void shouldProcessUploadCourtDocumentMessage() {

        final JsonObject courtDocumentPayload = buildDocumentCategoryJsonObject(buildDefendantDocument(), "0bb7b276-9dc0-4af2-83b9-f4acef0c7898", null);

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);
        when(usersGroupService.getGroupIdForDefenceLawyers()).thenReturn(randomUUID().toString());

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);
        verify(sender, times(3)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata(), withMetadataEnvelopedFrom(requestMessage).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT));
        final JsonObject commandCreateCourtDocumentPayload = commands.get(0).payload();
        //This is an Error Payload Structure that is actually returned....
        assertThat(commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getBoolean("containsFinancialMeans"), is(false));
        final JsonObject documentTypeRBACObject = commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getJsonObject("documentTypeRBAC");
        assertThat(documentTypeRBACObject, is(buildDocumentTypeDataWithRBAC()));
    }


    @Test
    public void shouldProcessUploadIDPCCourtDocumentMessage() {

        final JsonObject courtDocumentPayload = buildDocumentCategoryJsonObject(buildDefendantDocument(), "41be14e8-9df5-4b08-80b0-1e670bc80a5b", null);

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);
        verify(sender, times(4)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata(), withMetadataEnvelopedFrom(requestMessage).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT));
        assertThat(commands.get(2).metadata().name(), is(PUBLIC_IDPC_COURT_DOCUMENT_RECEIVED));
        final JsonObject commandCreateCourtDocumentPayload = commands.get(0).payload();
        final JsonObject commandIDPCPayload = commands.get(2).payload();
        //This is an Error Payload Structure that is actually returned....
        assertThat(commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getBoolean("containsFinancialMeans"), is(false));
        final JsonObject documentTypeRBACObject = commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getJsonObject("documentTypeRBAC");
        assertThat(documentTypeRBACObject, is(buildDocumentTypeDataWithRBAC()));
        assertThat(commandIDPCPayload.getString("caseId"), is("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27"));
        assertThat(commandIDPCPayload.getString("materialId"), is("5e1cc18c-76dc-47dd-99c1-d6f87385edf1"));
        assertThat(commandIDPCPayload.getString("defendantId"), is("e1d32d9d-29ec-4934-a932-22a50f223966"));


    }

    @Test
    public void shouldCallCommandToCreateCaseForCPSWhenCpsDefendantDocumentAdded() {

        final JsonObject defendantDocumentPayload = buildDocumentCategoryJsonObject(buildDefendantDocument(), "41be14e8-9df5-4b08-80b0-1e670bc80a5b", true);
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                defendantDocumentPayload);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")))
                .thenReturn(Optional.of(getProsecutionCase(true)));

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);
        verify(sender, times(4)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        verifyCreateCommand(commands.get(0), requestMessage, true);
        verifyPublicCourtDocumentAdded(commands.get(1), requestMessage);
        verifyIDPCommand(commands.get(2));
    }

    @Test
    public void shouldCallCommandToCreateCaseForCPSWhenCpsCaseDocumentAdded() {

        final JsonObject caseDocumentPayload = buildDocumentCategoryJsonObject(buildCaseDocument(), "41be14e8-9df5-4b08-80b0-1e670bc80a5a", true);
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                caseDocumentPayload);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);
        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")))
                .thenReturn(Optional.of(getProsecutionCase(true)));

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);
        verify(sender, times(3)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        verifyCreateCommand(commands.get(0), requestMessage, true);
        verifyPublicCourtDocumentAdded(commands.get(1), requestMessage);
    }

    @Test
    public void shouldCallOnlyPublicCourtDocumentAddedAndCreateCommandWhenCPSFlagIsFalseFromRefData() {
        final JsonObject jsonObject = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("cpsFlag", false)
                .build();

        final JsonObject caseDocumentPayload = buildDocumentCategoryJsonObject(buildCaseDocument(), "41be14e8-9df5-4b08-80b0-1e670bc80a5a", true);
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                caseDocumentPayload);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);
        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")))
                .thenReturn(Optional.of(getProsecutionCase(true)));
        when(referenceDataService.getProsecutorV2(any(JsonEnvelope.class), any(UUID.class), any(Requester.class))).thenReturn(of(jsonObject));

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);
        verify(sender, times(3)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        verifyCreateCommand(commands.get(0), requestMessage, false);
        verifyPublicCourtDocumentAdded(commands.get(1), requestMessage);
    }


    @Test
    public void shouldCallOnlyPublicDocumentAdded() {
        final JsonObject jsonObject = createObjectBuilder()
                .add("id", randomUUID().toString())
                .build();

        final JsonObject documentAddedWithProsecutionCaseId = buildDocumentAddedWithProsecutionCaseId();
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.document-with-prosecution-case-id-added"),
                documentAddedWithProsecutionCaseId);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);
        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")))
                .thenReturn(Optional.of(getProsecutionCase(true)));
        when(referenceDataService.getProsecutorV2(any(JsonEnvelope.class), any(UUID.class), any(Requester.class))).thenReturn(of(jsonObject));

        eventProcessor.handleAddDocumentWithProsecutionCaseId(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        verifyPublicDocumentAdded(commands.get(0), requestMessage);
    }

    @Test
    public void addDocumentWithProsecutionCaseIdShouldNotCallPublicDocumentAddedIfProsecutionCaseIsNull() {

        final JsonObject documentAddedWithProsecutionCaseId = buildDocumentAddedWithoutProsecutionCaseId();
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.document-with-prosecution-case-id-added"),
                documentAddedWithProsecutionCaseId);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);

        eventProcessor.handleAddDocumentWithProsecutionCaseId(requestMessage);
        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldNotCallCommandToUpdateCpsProsecutorWhenProsecutorIsAlreadyExist() {

        final JsonObject caseDocumentPayload = buildDocumentCategoryJsonObject(buildCaseDocument(), "41be14e8-9df5-4b08-80b0-1e670bc80a5a", true);
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                caseDocumentPayload);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);
        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")))
                .thenReturn(Optional.of(getProsecutionCase(true)));

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);
        verify(sender, times(3)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        verifyCreateCommand(commands.get(0), requestMessage, true);
        verifyPublicCourtDocumentAdded(commands.get(1), requestMessage);
    }

    @Test
    public void shouldNotCallCommandToUpdateCaseForCPSWhenNoCpsCaseDocumentAdded() {

        final JsonObject caseDocumentPayload = buildDocumentCategoryJsonObject(buildCaseDocument(), "41be14e8-9df5-4b08-80b0-1e670bc80a5a", false);
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                caseDocumentPayload);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);
        verify(sender, times(3)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        verifyCreateCommand(commands.get(0), requestMessage, true);
        verifyPublicCourtDocumentAdded(commands.get(1), requestMessage);
    }

    @Test
    public void shouldNotCallNotifyCpsWhenDefenceDisclosureFeatureNotEnabled() {

        final JsonObject caseDocumentPayload = buildDocumentCategoryJsonObject(buildCaseDocument(), "41be14e8-9df5-4b08-80b0-1e670bc80a5a", false);
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                caseDocumentPayload);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);

        verify(progressionService, times(0)).getCourtApplicationById(any(), any());
    }

    @Test
    public void shouldCallNotifyCpsWhenDefenceDisclosureFeatureEnabled() {

        final UUID applicationId = randomUUID();
        final JsonObject caseDocumentPayload = buildDocumentCategoryJsonObject(buildApplicationDocument(applicationId.toString()), "41be14e8-9df5-4b08-80b0-1e670bc80a5a", false);
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                caseDocumentPayload);

        List<UserGroupDetails> userGroupDetails = new ArrayList<>();
        userGroupDetails.add(new UserGroupDetails(randomUUID(), "Chambers Admin"));
        when(usersGroupService.getUserGroupsForUser(requestMessage)).thenReturn(userGroupDetails);
        when(featureControlGuard.isFeatureEnabled(FEATURE_DEFENCE_DISCLOSURE)).thenReturn(true);
        when(progressionService.getProsecutionCaseDetailById(eq(requestMessage), any())).thenReturn(empty());
        when(courtDocumentTransformer.transform(any(CourtDocument.class), eq(empty()), eq(requestMessage), anyString())).thenReturn(of("courtDocumentTransformed"));

        final UUID applicantCps = randomUUID();
        when(progressionService.getCourtApplicationById(requestMessage, applicationId.toString())).thenReturn(of(buildCourtApplication(applicationId.toString(), applicantCps.toString())));
        when(referenceDataService.getCPSProsecutors(requestMessage, requester)).thenReturn(Optional.of(createArrayBuilder()
                .add(createObjectBuilder().add("id", randomUUID().toString()).build())
                .add(createObjectBuilder().add("id", applicantCps.toString()).build())
                .build()));

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);

        verify(progressionService).getCourtApplicationById(any(), any());
        verify(cpsRestNotificationService).sendMaterial(anyString(), any(), any());
    }

    private JsonObject buildApplicationDocument(String applicationId) {
        return createObjectBuilder().add("applicationDocument",
                createObjectBuilder()
                        .add("applicationId", applicationId)
                        .add("prosecutionCaseId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27"))
                .add("caseDocument",
                        createObjectBuilder().add("prosecutionCaseId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")).build();
    }

    private JsonObject buildCourtApplication(final String applicationId, final String applicantProsecutorCps) {
        return createObjectBuilder()
                .add("courtApplication", createObjectBuilder()
                        .add("id", applicationId)
                        .add("applicant", createObjectBuilder().add("prosecutingAuthority", createObjectBuilder()
                                .add("prosecutionAuthorityId", applicantProsecutorCps).build()).build())
        ).build();
    }

    private JsonObject getProsecutionCase(boolean withProsecutor) {

        JsonObjectBuilder prosecutionCase = createObjectBuilder()
                .add("id", randomUUID().toString());
        if (withProsecutor) {
            prosecutionCase.add("prosecutor", createObjectBuilder().add("prosecutorName", "CPS").add("prosecutorId", randomUUID().toString()).build());
        }
        return createObjectBuilder().add("prosecutionCase", prosecutionCase.build()).build();
    }

    private void verifyIDPCommand(Envelope<JsonObject> commandIDPC) {
        assertThat(commandIDPC.metadata().name(), is(PUBLIC_IDPC_COURT_DOCUMENT_RECEIVED));
        assertThat(commandIDPC.payload().getString("caseId"), is("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27"));
        assertThat(commandIDPC.payload().getString("materialId"), is("5e1cc18c-76dc-47dd-99c1-d6f87385edf1"));
        assertThat(commandIDPC.payload().getString("defendantId"), is("e1d32d9d-29ec-4934-a932-22a50f223966"));
        assertNull(commandIDPC.payload().get("isCpsCase"));
    }

    private void verifyCreateCommand(final Envelope<JsonObject> commandCreateCourtDocument, final JsonEnvelope requestMessage, final boolean isCpsCase) {
        assertThat(commandCreateCourtDocument.metadata(), withMetadataEnvelopedFrom(requestMessage).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT));
        final JsonObject commandCreateCourtDocumentPayload = commandCreateCourtDocument.payload();
        //This is an Error Payload Structure that is actually returned....
        assertThat(commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getBoolean("containsFinancialMeans"), is(false));
        assertThat(commandCreateCourtDocumentPayload.getBoolean("isCpsCase"), is(isCpsCase));
        final JsonObject documentTypeRBACObject = commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getJsonObject("documentTypeRBAC");
        assertThat(documentTypeRBACObject, is(buildDocumentTypeDataWithRBAC()));
    }

    private void verifyUpdateCpsCommand(Envelope<JsonObject> commandUpdateCaseForCps, JsonEnvelope requestMessage) {
        assertThat(commandUpdateCaseForCps.metadata(), withMetadataEnvelopedFrom(requestMessage).withName(PROGRESSION_COMMAND_UPDATE_CASE_FOR_CPS_PROSECUTOR));
        assertThat(commandUpdateCaseForCps.payload().getString("caseId"), is("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27"));
        assertNull(commandUpdateCaseForCps.payload().get("isCpsCase"));
    }

    private void verifyPublicCourtDocumentAdded(Envelope<JsonObject> publicCourtDocumentAdded, JsonEnvelope requestMessage) {
        assertThat(publicCourtDocumentAdded.metadata().name(), is(PUBLIC_COURT_DOCUMENT_ADDED));
        assertNull(publicCourtDocumentAdded.payload().get("isCpsCase"));
    }
    private void verifyPublicDocumentAdded(Envelope<JsonObject> documentAdded, JsonEnvelope requestMessage) {
        assertThat(documentAdded.metadata().name(), is(PUBLIC_DOCUMENT_ADDED));
        assertNull(documentAdded.payload().get("isCpsCase"));
    }
}
