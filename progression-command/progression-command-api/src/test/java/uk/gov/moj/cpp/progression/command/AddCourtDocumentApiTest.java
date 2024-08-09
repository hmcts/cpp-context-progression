package uk.gov.moj.cpp.progression.command;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.command.api.UserDetailsLoader;
import uk.gov.moj.cpp.progression.command.service.DefenceQueryService;
import uk.gov.moj.cpp.progression.command.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.progression.command.service.UserGroupQueryService;
import uk.gov.moj.cpp.progression.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.Optional;
import java.util.UUID;

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
public class AddCourtDocumentApiTest {

    private static final String ADD_COURT_DOCUMENT_NAME = "progression.add-court-document";
    private static final String ADD_COURT_DOCUMENT_NAME_V2 = "progression.add-court-document-v2";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT_FOR_PROSECUTOR = "progression.add-court-document-for-prosecutor";
    private static final String ADD_COURT_DOCUMENT_COMMAND_NAME = "progression.command.add-court-document";
    private static final String ADD_COURT_DOCUMENT_COMMAND_NAME_V2 = "progression.command.add-court-document-v2";
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();
    @Mock
    private Sender sender;
    @InjectMocks
    private AddCourtDocumentApi addCourtDocumentApi;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;
    private UUID uuid;
    private UUID userId;
    private UUID docTypeId;

    @Mock
    private UserDetailsLoader userDetailsLoader;

    @Mock
    private Requester requester;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private DefenceQueryService defenceQueryService;

    @Mock
    private UserGroupQueryService userGroupQueryService;

    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Before
    public void setUp() throws Exception {
        uuid = randomUUID();
        userId = randomUUID();
        docTypeId = randomUUID();
    }

    @Test
    public void shouldHandleAddDocumentCommand() {
        assertThat(AddCourtDocumentApi.class, isHandlerClass(COMMAND_API)
                .with(method("handleAddCourtDocument").thatHandles(ADD_COURT_DOCUMENT_NAME)));
    }

    @Test
    public void shouldHandleAddDocumentForProsecutorCommand() {
        assertThat(AddCourtDocumentApi.class, isHandlerClass(COMMAND_API)
                .with(method("handleAddCourtDocumentForProsecutor").thatHandles(PROGRESSION_ADD_COURT_DOCUMENT_FOR_PROSECUTOR)));
    }

    @Test
    public void shouldAddDocument() {

        final UUID caseId = randomUUID();
        final JsonEnvelope commandEnvelope = buildEnvelope(null, true, caseId);

        addCourtDocumentApi.handleAddCourtDocument(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        JsonObjectBuilder builder = createObjectBuilder()
                .add("documentTypeId", docTypeId.toString())
                .add("documentCategory", createObjectBuilder()
                        .add("caseDocument", createObjectBuilder()
                                .add("prosecutionCaseId", caseId.toString())
                                .build())
                        .build());
        builder = createObjectBuilder().add("courtDocument", builder.build())
                .add("isUnbundledDocument", true);

        assertThat(newCommand.metadata().name(), is(ADD_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(builder.build()));
    }

    @Test(expected = ForbiddenRequestException.class)
    public void shouldThrowExceptionWhenAddDocumentForProsecutorWithForbiddenSectionType() {

        final UUID caseId = randomUUID();
        final JsonEnvelope commandEnvelope = buildEnvelope(null, true, caseId);

        Optional<DocumentTypeAccessReferenceData> documentTypeAccessReferenceData = of(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData()
                .withId(docTypeId)
                .withDefenceOnly(true)
                .build());

        when(referenceDataService.getDocumentTypeAccessReferenceData(requester, docTypeId)).thenReturn(documentTypeAccessReferenceData);
        when(prosecutionCaseQueryService.getProsecutionCase(commandEnvelope,caseId)).thenReturn(Optional.of(createProsecution()));
        when(userGroupQueryService.validateNonCPSUserOrg(any(),eq(userId),eq("Non CPS Prosecutors"),eq("DVLA"))).thenReturn(empty());
        addCourtDocumentApi.handleAddCourtDocumentForProsecutor(commandEnvelope);

    }

    @Test(expected = ForbiddenRequestException.class)
    public void shouldThrowExceptionWhenAddDocumentForProsecutorWithNON_CPS_PROSECUTORS() {

        final UUID caseId = randomUUID();
        final JsonEnvelope commandEnvelope = buildEnvelope(null, true, caseId);

        Optional<DocumentTypeAccessReferenceData> documentTypeAccessReferenceData = of(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData()
                .withId(docTypeId)
                .withDefenceOnly(true)
                .build());

        when(referenceDataService.getDocumentTypeAccessReferenceData(requester, docTypeId)).thenReturn(documentTypeAccessReferenceData);
        when(prosecutionCaseQueryService.getProsecutionCase(commandEnvelope,caseId)).thenReturn(Optional.of(createProsecution()));
        when(userGroupQueryService.validateNonCPSUserOrg(any(),eq(userId),eq("Non CPS Prosecutors"),eq("DVLA"))).thenReturn(of("OrganisationMisMatch"));
        addCourtDocumentApi.handleAddCourtDocumentForProsecutor(commandEnvelope);

    }

    @Test
    public void shouldAddDocumentForProsecutorWithAllowedSectionType() {
        final UUID caseId = randomUUID();
        final JsonEnvelope commandEnvelope = buildEnvelope(null, true, caseId);

        Optional<DocumentTypeAccessReferenceData> documentTypeAccessReferenceData = of(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData()
                .withId(docTypeId)
                .withDefenceOnly(false)
                .build());

        when(referenceDataService.getDocumentTypeAccessReferenceData(requester, docTypeId)).thenReturn(documentTypeAccessReferenceData);
        when(defenceQueryService.isUserProsecutingCase(commandEnvelope, caseId)).thenReturn(true);
        when(prosecutionCaseQueryService.getProsecutionCase(commandEnvelope,caseId)).thenReturn(Optional.of(createProsecution()));
        when(userGroupQueryService.validateNonCPSUserOrg(any(),eq(userId),eq("Non CPS Prosecutors"),eq("DVLA"))).thenReturn(empty());

        addCourtDocumentApi.handleAddCourtDocumentForProsecutor(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        JsonObjectBuilder builder = createObjectBuilder()
                .add("documentTypeId", docTypeId.toString())
                .add("documentCategory", createObjectBuilder()
                        .add("caseDocument", createObjectBuilder()
                                .add("prosecutionCaseId", caseId.toString())
                                .build())
                        .build());
        builder = createObjectBuilder().add("courtDocument", builder.build())
                .add("isUnbundledDocument", true);

        assertThat(newCommand.metadata().name(), is(ADD_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(builder.build()));
    }

    @Test
    public void shouldAddDocumentForProsecutorWithAllowedSectionTypeAndNonCPSProsecutors() {
        final UUID caseId = randomUUID();
        final JsonEnvelope commandEnvelope = buildEnvelope(null, true, caseId);

        Optional<DocumentTypeAccessReferenceData> documentTypeAccessReferenceData = of(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData()
                .withId(docTypeId)
                .withDefenceOnly(false)
                .build());

        when(referenceDataService.getDocumentTypeAccessReferenceData(requester, docTypeId)).thenReturn(documentTypeAccessReferenceData);
        when(defenceQueryService.isUserProsecutingCase(commandEnvelope, caseId)).thenReturn(true);
        when(prosecutionCaseQueryService.getProsecutionCase(commandEnvelope,caseId)).thenReturn(Optional.of(createProsecution()));
        when(userGroupQueryService.validateNonCPSUserOrg(any(),eq(userId),eq("Non CPS Prosecutors"),eq("DVLA"))).thenReturn(of("OrganisationMatch"));

        addCourtDocumentApi.handleAddCourtDocumentForProsecutor(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        JsonObjectBuilder builder = createObjectBuilder()
                .add("documentTypeId", docTypeId.toString())
                .add("documentCategory", createObjectBuilder()
                        .add("caseDocument", createObjectBuilder()
                                .add("prosecutionCaseId", caseId.toString())
                                .build())
                        .build());
        builder = createObjectBuilder().add("courtDocument", builder.build())
                .add("isUnbundledDocument", true);

        assertThat(newCommand.metadata().name(), is(ADD_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(builder.build()));
    }

    @Test
    public void shouldAddDocumentForProsecutorWithAllowedSectionTypeAndNonCPSProsecutorsAndProsecutorObjectExists()
    {
        final UUID caseId = randomUUID();
        final JsonEnvelope commandEnvelope = buildEnvelope(null, true, caseId);

        Optional<DocumentTypeAccessReferenceData> documentTypeAccessReferenceData = of(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData()
                .withId(docTypeId)
                .withDefenceOnly(false)
                .build());

        when(referenceDataService.getDocumentTypeAccessReferenceData(requester, docTypeId)).thenReturn(documentTypeAccessReferenceData);
        when(defenceQueryService.isUserProsecutingCase(commandEnvelope, caseId)).thenReturn(true);
        when(prosecutionCaseQueryService.getProsecutionCase(commandEnvelope,caseId)).thenReturn(Optional.of(createProsecutionWithProsecutors()));
        when(userGroupQueryService.validateNonCPSUserOrg(any(),eq(userId),eq("Non CPS Prosecutors"),eq("DVLA"))).thenReturn(of("OrganisationMatch"));

        addCourtDocumentApi.handleAddCourtDocumentForProsecutor(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        JsonObjectBuilder builder = createObjectBuilder()
                .add("documentTypeId", docTypeId.toString())
                .add("documentCategory", createObjectBuilder()
                        .add("caseDocument", createObjectBuilder()
                                .add("prosecutionCaseId", caseId.toString())
                                .build())
                        .build());
        builder = createObjectBuilder().add("courtDocument", builder.build())
                .add("isUnbundledDocument", true);

        assertThat(newCommand.metadata().name(), is(ADD_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(builder.build()));
    }

    @Test(expected = ForbiddenRequestException.class)
    public void shouldThrowExceptionWhenAddDocumentForProsecutorWithAUserNotProsecutingTheCase() {

        final UUID caseId = randomUUID();
        final JsonEnvelope commandEnvelope = buildEnvelope(null, true, caseId);

        Optional<DocumentTypeAccessReferenceData> documentTypeAccessReferenceData = of(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData()
                .withId(docTypeId)
                .withDefenceOnly(false)
                .build());

        when(referenceDataService.getDocumentTypeAccessReferenceData(requester, docTypeId)).thenReturn(documentTypeAccessReferenceData);
        when(defenceQueryService.isUserProsecutingCase(commandEnvelope, caseId)).thenReturn(false);
        when(prosecutionCaseQueryService.getProsecutionCase(commandEnvelope,caseId)).thenReturn(Optional.of(createProsecution()));
        when(userGroupQueryService.validateNonCPSUserOrg(any(),eq(userId),eq("Non CPS Prosecutors"),eq("DVLA"))).thenReturn(empty());
        addCourtDocumentApi.handleAddCourtDocumentForProsecutor(commandEnvelope);

    }

    @Test(expected = BadRequestException.class)
    public void shouldThroughBadRequest() {
        final JsonEnvelope commandEnvelope = buildEnvelopeWithResource(randomUUID().toString());
        addCourtDocumentApi.handleAddCourtDocumentForDefence(commandEnvelope);
    }

    @Test(expected = ForbiddenRequestException.class)
    public void shouldThroughForbiddenRequest() {
        final JsonEnvelope commandEnvelope = buildEnvelopeWithResource("e1d32d9d-29ec-4934-a932-22a50f223966");
        when(userDetailsLoader.isPermitted(any(), any())).thenReturn(false);
        addCourtDocumentApi.handleAddCourtDocumentForDefence(commandEnvelope);
    }


    @Test
    public void shouldProcessRequestForDefence() {
        final JsonEnvelope commandEnvelope = buildEnvelopeWithResource("e1d32d9d-29ec-4934-a932-22a50f223966");
        when(userDetailsLoader.isPermitted(any(), any())).thenReturn(true);
        addCourtDocumentApi.handleAddCourtDocumentForDefence(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(ADD_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(expected().payload()));
    }

    @Test
    public void shouldAddDocumentWithCpsCaseAndUnbundledDocument() {

        final UUID caseId = randomUUID();
        final JsonEnvelope commandEnvelope = buildEnvelope(true, null, caseId);
        final JsonEnvelope expectedCommandEnvelope = buildEnvelopeForHandler(true, null, caseId);


        addCourtDocumentApi.handleAddCourtDocument(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(ADD_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(expectedCommandEnvelope.payload()));
    }

    private JsonEnvelope buildEnvelope(final Boolean isCpsCase, final Boolean isUnbundledDocument, final UUID prosecutionCaseId) {
        JsonObjectBuilder builder = createObjectBuilder()
                .add("documentTypeId", docTypeId.toString())
                .add("documentCategory", createObjectBuilder()
                        .add("caseDocument", createObjectBuilder()
                                .add("prosecutionCaseId", prosecutionCaseId.toString())
                                .build())
                        .build());
        if (nonNull(isCpsCase)) {
            builder.add("isCpsCase", isCpsCase);
        }
        if (nonNull(isUnbundledDocument)) {
            builder.add("isUnbundledDocument", isUnbundledDocument);
        }

        builder = createObjectBuilder().add("courtDocument", builder.build());

        final JsonObject payload = builder.build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_COURT_DOCUMENT_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

    private JsonEnvelope buildEnvelopeForHandler(final Boolean isCpsCase, final Boolean isUnbundledDocument, final UUID prosecutionCaseId) {
        JsonObjectBuilder courtDocumentBuilder = createObjectBuilder()
                .add("documentTypeId", docTypeId.toString())
                .add("documentCategory", createObjectBuilder()
                        .add("caseDocument", createObjectBuilder()
                                .add("prosecutionCaseId", prosecutionCaseId.toString())
                                .build())
                        .build());

        final JsonObjectBuilder builder = createObjectBuilder()
                .add("courtDocument", courtDocumentBuilder.build());
        if (nonNull(isCpsCase)) {
            builder.add("isCpsCase", isCpsCase);
        }
        if (nonNull(isUnbundledDocument)) {
            builder.add("isUnbundledDocument", isUnbundledDocument);
        }
        final JsonObject payload = builder.build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_COURT_DOCUMENT_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

    private JsonEnvelope buildEnvelopeWithResource(String defendantId) {
        final JsonObject payload = CommandClientTestBase.readJson("json/add-court-document.json", JsonObject.class);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_COURT_DOCUMENT_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, createObjectBuilder().add("courtDocument", payload).add("defendantId", defendantId).build());
    }

    private JsonEnvelope expected() {
        final JsonObject payload = CommandClientTestBase.readJson("json/add-court-document.json", JsonObject.class);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_COURT_DOCUMENT_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, createObjectBuilder().add("courtDocument", payload).build());
    }

    @Test
    public void shouldAddDocumentV2() {

        final JsonObject payload = CommandClientTestBase.readJson("json/progression.add-court-document-v2.json", JsonObject.class);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_COURT_DOCUMENT_NAME_V2)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        addCourtDocumentApi.handleV2(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(ADD_COURT_DOCUMENT_COMMAND_NAME_V2));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payload()));
    }

    @Test
    public void shouldAddDocumentCpsCase() {

        final UUID caseId = randomUUID();
        final JsonEnvelope commandEnvelope = buildEnvelope(true, true, caseId);


        addCourtDocumentApi.handleAddCourtDocument(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        JsonObjectBuilder builder = createObjectBuilder()
                .add("documentTypeId", docTypeId.toString())
                .add("documentCategory", createObjectBuilder()
                        .add("caseDocument", createObjectBuilder()
                                .add("prosecutionCaseId", caseId.toString())
                                .build())
                        .build());
        builder = createObjectBuilder().add("courtDocument", builder.build())
                .add("isCpsCase", true)
                .add("isUnbundledDocument", true);

        assertThat(newCommand.metadata().name(), is(ADD_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(builder.build()));
    }

    private JsonObject createProsecution() {
        return createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                .add("prosecutionCaseIdentifier", createObjectBuilder()
                        .add("prosecutionAuthorityCode", "DVLA").build())
                .build()).build();

    }

    private JsonObject createProsecutionWithProsecutors() {
        return createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                .add("prosecutionCaseIdentifier", createObjectBuilder()
                        .add("prosecutionAuthorityCode", "DVLA1").build())
                .add("prosecutor", createObjectBuilder().add("prosecutorCode", "DVLA"))

                .build()).build();

    }

}
