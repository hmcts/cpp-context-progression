package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
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
    private final Enveloper enveloper = createEnveloper();
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
        return Json.createObjectBuilder().add("cppGroup", Json.createObjectBuilder().add("id", randomUUID().toString()).add("groupName", userGroupName));
    }

    @Before
    public void initMocks() {
        initReferenceData();
        initUserGroupsData();
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

        final NowDocumentRequest nowDocumentRequest = nowsRequestedTemplateWithVisibleUsers();
        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest)
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .build();

        UUID fileId = randomUUID();
        when(fileService.storePayload(nowContentArgumentCaptor.capture(), any(), any())).thenReturn(fileId);
        doNothing().when(systemDocGeneratorService).generateDocument(any(), any());

        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);
        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(systemDocGeneratorService).generateDocument(any(), any());
        verify(fileService).storePayload(any(), any(), any());

        verify(this.sender, times(3)).send(this.envelopeArgumentCaptor.capture());
        final DefaultEnvelope<?> jsonEnvelope = this.envelopeArgumentCaptor.getAllValues().get(0);
        final JsonObject jsonObject = (JsonObject) jsonEnvelope.payload();
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

        final DefaultEnvelope<?> nowsDocumentSentEnvelope = this.envelopeArgumentCaptor.getAllValues().get(1);
        assertThat(nowsDocumentSentEnvelope.metadata().name(), is(PROGRESSION_COMMAND_RECORD_NOWS_DOCUMENT_SENT));
        assertThat(((JsonObject) nowsDocumentSentEnvelope.payload()).getString("materialId"), is(nowDocumentRequested.getMaterialId().toString()));
        assertThat(((JsonObject) nowsDocumentSentEnvelope.payload()).getString("payloadFileId"), is(fileId.toString()));
        assertThat(((JsonObject) nowsDocumentSentEnvelope.payload()).getString("hearingId"), is(nowDocumentRequested.getNowDocumentRequest().getHearingId().toString()));
        assertThat(((JsonObject) nowsDocumentSentEnvelope.payload()).getBoolean("cpsProsecutionCase"), is(false));
        assertThat(((JsonObject) nowsDocumentSentEnvelope.payload()).getJsonObject("orderAddressee").getJsonObject("address").getString("emailAddress1"), is(nowDocumentRequested.getNowDocumentRequest().getNowContent().getOrderAddressee().getAddress().getEmailAddress1()));

        final DefaultEnvelope<?> publicNowDocumentRequestedEnvelope = this.envelopeArgumentCaptor.getAllValues().get(2);
        assertThat(publicNowDocumentRequestedEnvelope.metadata().name(), is(PUBLIC_PROGRESSION_NOW_DOCUMENT_REQUESTED));
        assertThat(((JsonObject) publicNowDocumentRequestedEnvelope.payload()).getString("materialId"), is(nowDocumentRequested.getMaterialId().toString()));

        final String updatedNowContent = ((JsonObject) (nowContentArgumentCaptor.getValue().get(FINANCIAL_ORDER_DETAILS))).toString();
        with(updatedNowContent).assertThat(ACCOUNTING_DIVISION_CODE, is("077"));
    }

    @Test
    public void shouldGenerateNowAndStoreWithNotVisibleUserGroupInFileStore() {

        final UUID systemUserid = UUID.randomUUID();
        final NowDocumentRequest nowDocumentRequest = nowsRequestedTemplateWithNonVisibleUsers();
        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest)
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .build();

        when(fileService.storePayload(nowContentArgumentCaptor.capture(), stringArgumentCaptor.capture(), any())).thenReturn(randomUUID());
        doNothing().when(systemDocGeneratorService).generateDocument(any(), any());

        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);
        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(systemDocGeneratorService).generateDocument(any(), any());
        verify(fileService).storePayload(any(), any(), any());
        assertThat(stringArgumentCaptor.getValue(), is(startsWith(nowDocumentRequest.getNowContent().getOrderName())));

        verify(this.sender, times(3)).send(this.envelopeArgumentCaptor.capture());
        final DefaultEnvelope<?> jsonEnvelope = this.envelopeArgumentCaptor.getAllValues().get(0);
        final JsonObject jsonObject = (JsonObject) jsonEnvelope.payload();
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
        final NowDocumentRequest nowDocumentRequest = nowsRequestedTemplate();
        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest)
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .build();

        when(fileService.storePayload(nowContentArgumentCaptor.capture(), stringArgumentCaptor.capture(), any())).thenReturn(randomUUID());
        doNothing().when(systemDocGeneratorService).generateDocument(any(), any());

        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);
        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(systemDocGeneratorService).generateDocument(any(), any());
        verify(fileService).storePayload(any(), any(), any());
        assertThat(stringArgumentCaptor.getValue(), is(startsWith(nowDocumentRequest.getNowContent().getOrderName())));

        verify(this.sender, times(3)).send(this.envelopeArgumentCaptor.capture());
        final DefaultEnvelope<?> jsonEnvelope = this.envelopeArgumentCaptor.getAllValues().get(0);
        final JsonObject jsonObject = (JsonObject) jsonEnvelope.payload();
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
        final JsonObject docTypeData = Json.createObjectBuilder()
                .add("section", COURT_FINAL_ORDERS)
                .add("seqNum", 3)
                .add("courtDocumentTypeRBAC",
                        Json.createObjectBuilder()
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
