package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.processor.NowsRequestedEventProcessor.PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT;
import static uk.gov.moj.cpp.progression.service.DocumentGeneratorService.ACCOUNTING_DIVISION_CODE;
import static uk.gov.moj.cpp.progression.service.DocumentGeneratorService.FINANCIAL_ORDER_DETAILS;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNowDocumentRequestTemplate;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNowDocumentRequestTemplateWithNonVisibleUserList;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNowDocumentRequestTemplateWithVisibleUserList;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.core.courts.NowsDocumentGenerated;
import uk.gov.justice.core.courts.nowdocument.NowDistribution;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.core.courts.nowdocument.OrderAddressee;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.FileService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.SystemDocGeneratorService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.test.FileUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NowsRequestedEventProcessorTest {

    public static final String USER_ID = UUID.randomUUID().toString();

    public static final UUID fileId = UUID.randomUUID();
    private static final String PUBLIC_PROGRESSION_NOW_DOCUMENT_REQUESTED = "public.progression.now-document-requested";
    private static final String PROGRESSION_COMMAND_RECORD_NOWS_DOCUMENT_SENT = "progression.command.record-nows-document-sent";
    private static final String DEFENCE_USERS = "Defence Users";
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String COURT_FINAL_ORDERS = "Court Final orders";
    private static final String COURT_DOCUMENT = "courtDocument";
    private static final String MAGISTRATES = "Magistrates";

    @Spy
    @InjectMocks
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);


    private NowsRequestedEventProcessor nowsRequestedEventProcessor;
    @Mock
    private Sender sender;
    @Mock
    private RefDataService refDataService;
    @Mock
    private UsersGroupService usersGroupService;
    @Captor
    private ArgumentCaptor<DefaultEnvelope<?>> envelopeArgumentCaptor;
    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;
    @Captor
    private ArgumentCaptor<JsonObject> nowContentArgumentCaptor;
    @Mock
    private FileService fileService;
    @Mock
    private SystemDocGeneratorService systemDocGeneratorService;
    @Mock
    private DocumentGeneratorService documentGeneratorService;

    public static NowDocumentRequest nowsRequestedTemplate() {
        return generateNowDocumentRequestTemplate(UUID.randomUUID());
    }

    public static NowDocumentRequest nowsRequestedTemplateWithVisibleUsers() {
        return generateNowDocumentRequestTemplateWithVisibleUserList(UUID.randomUUID());
    }

    public static NowDocumentRequest nowsRequestedTemplateWithNonVisibleUsers() {
        return generateNowDocumentRequestTemplateWithNonVisibleUserList(UUID.randomUUID());
    }

    private static JsonObjectBuilder buildUserGroup(final String userGroupName) {
        return JsonObjects.createObjectBuilder().add("cppGroup", JsonObjects.createObjectBuilder().add("id", randomUUID().toString()).add("groupName", userGroupName));
    }

    @BeforeEach
    public void initMocks() {
        nowsRequestedEventProcessor = new NowsRequestedEventProcessor(
                this.sender,
                this.documentGeneratorService,
                this.jsonObjectToObjectConverter,
                this.objectToJsonObjectConverter,
                this.refDataService,
                this.usersGroupService,
                this.fileService,
                this.systemDocGeneratorService);
    }

    @Test
    public void shouldProcessNowsDocumentGenerated() {
        final NowsDocumentGenerated nowsDocumentGenerated = NowsDocumentGenerated.nowsDocumentGenerated()
                .withMaterialId(randomUUID())
                .withHearingId(randomUUID())
                .withNowDistribution(NowDistribution.nowDistribution().build())
                .withOrderAddressee(OrderAddressee.orderAddressee().build())
                .withSystemDocGeneratorId(randomUUID())
                .withUserId(fromString(USER_ID))
                .withFileName(randomUUID().toString())
                .build();

        doNothing().when(documentGeneratorService).addDocumentToMaterial(any(), any(), any());

        final JsonEnvelope eventEnvelope = envelope(nowsDocumentGenerated);

        this.nowsRequestedEventProcessor.processNowsDocumentGenerated(eventEnvelope);

        verify(documentGeneratorService).addDocumentToMaterial(any(), any(), any());
    }

    @Test
    public void shouldGenerateNowAndStoreWithVisibleUserGroupInFileStore() {
        initReferenceData();
        initUserGroupsData();
        final UUID userId = UUID.randomUUID();
        final String fileName = "filename";
        final String templateName = "templateName";

        final NowDocumentRequest nowDocumentRequest = nowsRequestedTemplateWithVisibleUsers();
        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest)
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .withFileName(fileName)
                .withCpsProsecutionCase(false)
                .withTemplateName(templateName)
                .withUserId(userId)
                .build();

        UUID fileId = randomUUID();
        when(fileService.storePayload(nowContentArgumentCaptor.capture(), any(), any())).thenReturn(fileId);
        doNothing().when(systemDocGeneratorService).generateDocument(any(), any());

        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);
        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(systemDocGeneratorService).generateDocument(any(), any());
        verify(fileService).storePayload(any(), any(), any());

        verify(this.sender, times(4)).send(this.envelopeArgumentCaptor.capture());
        final List<DefaultEnvelope<?>> jsonEnvelopeList = this.envelopeArgumentCaptor.getAllValues();
        final DefaultEnvelope<?> JsonEnvelopeForCreateCourtDocument = jsonEnvelopeList.stream()
                .filter(envelope -> envelope.metadata().name().equalsIgnoreCase(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT))
                .findFirst()
                .get();
        final JsonObject jsonObject = (JsonObject) JsonEnvelopeForCreateCourtDocument.payload();
        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject(COURT_DOCUMENT), CourtDocument.class);
        assertThat(courtDocument.getMaterials().get(0).getUserGroups().size(), is(8));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains(DEFENCE_USERS));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Defence Lawyers"));
        assertThat(courtDocument.getMimeType(), is(APPLICATION_PDF));
        assertThat(courtDocument.getDocumentTypeDescription(), is(COURT_FINAL_ORDERS));
        assertThat(courtDocument.getSeqNum(), is(3));
        assertThat(courtDocument.getDocumentTypeRBAC().getReadUserGroups().size(), is(8));
        assertTrue(courtDocument.getDocumentTypeRBAC().getReadUserGroups().contains(DEFENCE_USERS));
        assertTrue(courtDocument.getDocumentTypeRBAC().getReadUserGroups().contains("Defence Lawyers"));

        final DefaultEnvelope<?> nowsDocumentSentEnvelope = jsonEnvelopeList.stream()
                .filter(envelope -> envelope.metadata().name().equalsIgnoreCase(PROGRESSION_COMMAND_RECORD_NOWS_DOCUMENT_SENT))
                .findFirst()
                .get();
        assertThat(((JsonObject) nowsDocumentSentEnvelope.payload()).getString("materialId"), is(nowDocumentRequested.getMaterialId().toString()));
        assertThat(((JsonObject) nowsDocumentSentEnvelope.payload()).getString("payloadFileId"), is(fileId.toString()));
        assertThat(((JsonObject) nowsDocumentSentEnvelope.payload()).getString("hearingId"), is(nowDocumentRequested.getNowDocumentRequest().getHearingId().toString()));
        assertThat(((JsonObject) nowsDocumentSentEnvelope.payload()).getBoolean("cpsProsecutionCase"), is(false));
        assertThat(((JsonObject) nowsDocumentSentEnvelope.payload()).getJsonObject("orderAddressee").getJsonObject("address").getString("emailAddress1"), is(nowDocumentRequested.getNowDocumentRequest().getNowContent().getOrderAddressee().getAddress().getEmailAddress1()));

        final DefaultEnvelope<?> publicNowDocumentRequestedEnvelope = jsonEnvelopeList.stream()
                .filter(envelope -> envelope.metadata().name().equalsIgnoreCase(PUBLIC_PROGRESSION_NOW_DOCUMENT_REQUESTED))
                .findFirst()
                .get();
        assertThat(((JsonObject) publicNowDocumentRequestedEnvelope.payload()).getString("materialId"), is(nowDocumentRequested.getMaterialId().toString()));

        final String updatedNowContent = ((JsonObject) (nowContentArgumentCaptor.getValue().get(FINANCIAL_ORDER_DETAILS))).toString();
        with(updatedNowContent).assertThat(ACCOUNTING_DIVISION_CODE, is("077"));
    }

    @Test
    public void shouldGenerateNowAndStoreWithNotVisibleUserGroupInFileStore() {

        initReferenceData();
        initUserGroupsData();
        final UUID userId = UUID.randomUUID();
        final String fileName = "filename";
        final String templateName = "templateName";

        final NowDocumentRequest nowDocumentRequest = nowsRequestedTemplateWithNonVisibleUsers();
        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest)
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .withFileName(fileName)
                .withCpsProsecutionCase(false)
                .withTemplateName(templateName)
                .withUserId(userId)
                .build();

        when(fileService.storePayload(nowContentArgumentCaptor.capture(), stringArgumentCaptor.capture(), any())).thenReturn(randomUUID());
        doNothing().when(systemDocGeneratorService).generateDocument(any(), any());

        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);
        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(systemDocGeneratorService).generateDocument(any(), any());
        verify(fileService).storePayload(any(), any(), any());
        assertThat(stringArgumentCaptor.getValue(), is(fileName));

        verify(this.sender, times(4)).send(this.envelopeArgumentCaptor.capture());
        final List<DefaultEnvelope<?>> jsonEnvelopeList = this.envelopeArgumentCaptor.getAllValues();
        final DefaultEnvelope<?> JsonEnvelopeForCreateCourtDocument = jsonEnvelopeList.stream()
                .filter(envelope -> envelope.metadata().name().equalsIgnoreCase(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT))
                .findFirst()
                .get();
        final JsonObject jsonObject = (JsonObject) JsonEnvelopeForCreateCourtDocument.payload();
        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject(COURT_DOCUMENT), CourtDocument.class);
        assertThat(courtDocument.getMaterials().get(0).getUserGroups().size(), is(9));
        assertFalse(courtDocument.getMaterials().get(0).getUserGroups().contains("Probation Admin"));
        assertFalse(courtDocument.getMaterials().get(0).getUserGroups().contains(DEFENCE_USERS));
        assertFalse(courtDocument.getMaterials().get(0).getUserGroups().contains("Chambers Clerk"));
        assertThat(courtDocument.getMimeType(), is(APPLICATION_PDF));
        assertThat(courtDocument.getDocumentTypeDescription(), is(COURT_FINAL_ORDERS));
        assertThat(courtDocument.getSeqNum(), is(3));
        assertThat(courtDocument.getDocumentTypeRBAC().getReadUserGroups().size(), is(9));
        assertFalse(courtDocument.getDocumentTypeRBAC().getReadUserGroups().contains("Probation Admin"));
        assertFalse(courtDocument.getDocumentTypeRBAC().getReadUserGroups().contains(DEFENCE_USERS));
        assertFalse(courtDocument.getDocumentTypeRBAC().getReadUserGroups().contains("Chambers Clerk"));
    }

    @Test
    public void shouldGenerateNowAndStoreInFileStore() {
        initReferenceData();
        initUserGroupsData();
        final UUID userId = randomUUID();
        final String fileName = "filename";
        final String templateName = "templateName";

        final NowDocumentRequest nowDocumentRequest = nowsRequestedTemplate();
        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest)
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .withFileName(fileName)
                .withCpsProsecutionCase(false)
                .withTemplateName(templateName)
                .withUserId(userId)
                .build();

        when(fileService.storePayload(nowContentArgumentCaptor.capture(), stringArgumentCaptor.capture(), any())).thenReturn(randomUUID());
        doNothing().when(systemDocGeneratorService).generateDocument(any(), any());

        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);
        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(systemDocGeneratorService).generateDocument(any(), any());
        verify(fileService).storePayload(any(), any(), any());
        assertThat(stringArgumentCaptor.getValue(), is(startsWith(fileName)));

        verify(this.sender, times(4)).send(this.envelopeArgumentCaptor.capture());
        final List<DefaultEnvelope<?>> jsonEnvelopeList = this.envelopeArgumentCaptor.getAllValues();
        final DefaultEnvelope<?> JsonEnvelopeForCreateCourtDocument = jsonEnvelopeList.stream()
                .filter(envelope -> envelope.metadata().name().equalsIgnoreCase(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT))
                .findFirst()
                .get();
        final JsonObject jsonObject = (JsonObject) JsonEnvelopeForCreateCourtDocument.payload();
        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject(COURT_DOCUMENT), CourtDocument.class);

        assertThat(courtDocument.getMaterials().get(0).getUserGroups().size(), is(13));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Crown Court Admin"));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains(MAGISTRATES));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Court Associate"));
        assertThat(courtDocument.getMimeType(), is(APPLICATION_PDF));
        assertThat(courtDocument.getDocumentTypeDescription(), is(COURT_FINAL_ORDERS));
        assertThat(courtDocument.getSeqNum(), is(3));
        assertThat(courtDocument.getDocumentTypeRBAC().getReadUserGroups().size(), is(13));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Crown Court Admin"));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains(MAGISTRATES));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Court Associate"));

        DefaultEnvelope<?> addCourtDocjsonEnvelope =  jsonEnvelopeList.stream()
                .filter(envelope -> envelope.metadata().name().equalsIgnoreCase("progression.command.add-court-document"))
                .findFirst()
                .get();
        final JsonObject addCourtDocjsonObject = (JsonObject) addCourtDocjsonEnvelope.payload();
        final CourtDocument addCourtDocument = jsonObjectToObjectConverter.convert(addCourtDocjsonObject.getJsonObject(COURT_DOCUMENT), CourtDocument.class);
        assertThat(addCourtDocument.getDocumentTypeDescription(), is("Electronic Notifications"));

        final DefaultEnvelope<?> sentCommandEnvelope = jsonEnvelopeList.stream()
                .filter(envelope -> envelope.metadata().name().equalsIgnoreCase(PROGRESSION_COMMAND_RECORD_NOWS_DOCUMENT_SENT))
                .findFirst()
                .get();
        final JsonObject sentCommand = (JsonObject)sentCommandEnvelope.payload();
        assertThat(sentCommand.getString("fileName"), is(fileName));
        assertThat(sentCommand.getBoolean("cpsProsecutionCase"), is(false));
    }

    private JsonEnvelope envelope(final NowsDocumentGenerated nowsDocumentGenerated) {
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(nowsDocumentGenerated);
        return envelopeFrom(
                metadataWithRandomUUID("progression.event.nows-document-generated").withUserId(USER_ID),
                objectToJsonObjectConverter.convert(jsonObject)
        );
    }

    private JsonEnvelope envelope(final NowDocumentRequested nowDocumentRequested) {
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(nowDocumentRequested);
        return envelopeFrom(
                metadataWithRandomUUID("progression.event.nows-requested").withUserId(USER_ID),
                objectToJsonObjectConverter.convert(jsonObject)
        );
    }

    private void initReferenceData() {
        final JsonObject docTypeData = JsonObjects.createObjectBuilder()
                .add("section", COURT_FINAL_ORDERS)
                .add("seqNum", 3)
                .add("courtDocumentTypeRBAC",
                        JsonObjects.createObjectBuilder()
                                .add("uploadUserGroups", createArrayBuilder().build())
                                .add("readUserGroups", createArrayBuilder()
                                        .add(buildUserGroup(MAGISTRATES))
                                        .add(buildUserGroup("Court Clerk")).build())
                                .add("downloadUserGroups", createArrayBuilder()
                                        .add(buildUserGroup(MAGISTRATES)).build()).build())
                .build();

        when(refDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(docTypeData));
    }

    private void initUserGroupsData() {
        String response = FileUtil.getPayload("usersgroups.get-groups-with-organisation.json");
        final JsonObject userGroups = new StringToJsonObjectConverter().convert(response);
        when(usersGroupService.getGroupsWithOrganisation(any())).thenReturn(userGroups);
    }
}
