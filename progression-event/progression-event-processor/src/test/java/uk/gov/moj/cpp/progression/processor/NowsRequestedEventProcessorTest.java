package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.processor.NowsRequestedEventProcessor.PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT;
import static uk.gov.moj.cpp.progression.service.DocumentGeneratorService.ACCOUNTING_DIVISION_CODE;
import static uk.gov.moj.cpp.progression.service.DocumentGeneratorService.FINANCIAL_ORDER_DETAILS;
import static uk.gov.moj.cpp.progression.service.DocumentGeneratorService.PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNowDocumentRequestTemplate;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNowDocumentRequestTemplateWithNonVisibleUserList;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNowDocumentRequestTemplateWithVisibleUserList;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.core.courts.nowdocument.OrderAddressee;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UploadMaterialContext;
import uk.gov.moj.cpp.progression.service.UploadMaterialService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.service.exception.DocumentGenerationException;
import uk.gov.moj.cpp.progression.service.exception.FileUploadException;
import uk.gov.moj.cpp.progression.service.utils.NowDocumentValidator;
import uk.gov.moj.cpp.progression.test.FileUtil;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NowsRequestedEventProcessorTest {

    public static final String USER_ID = UUID.randomUUID().toString();

    public static final UUID fileId = UUID.randomUUID();
    private static final String PUBLIC_PROGRESSION_NOW_DOCUMENT_REQUESTED = "public.progression.now-document-requested";
    private static final String DEFENCE_USERS = "Defence Users";
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String COURT_FINAL_ORDERS = "Court Final orders";
    private static final String HTTP_MATERIAL_URL = "http://materialUrl";
    private static final String FILE_NAME = "fileName";
    private static final String COURT_DOCUMENT = "courtDocument";
    private static final String MAGISTRATES = "Magistrates";


    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);


    private NowsRequestedEventProcessor nowsRequestedEventProcessor;
    @Mock
    private Sender sender;
    @Mock
    private UploadMaterialService uploadMaterialService;
    @Mock
    private MaterialService materialService;
    @Mock
    private ApplicationParameters applicationParameters;
    @Mock
    private MaterialUrlGenerator materialUrlGenerator;
    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;
    @Mock
    private DocumentGeneratorClient documentGeneratorClient;
    @Mock
    private FileStorer fileStorer;
    @Mock
    private RefDataService refDataService;
    @Mock
    private UsersGroupService usersGroupService;
    @Mock
    private SystemUserProvider systemUserProvider;
    @Mock
    private NowDocumentValidator nowDocumentValidator;

    @Captor
    private ArgumentCaptor<DefaultEnvelope<?>> envelopeArgumentCaptor;
    @Captor
    private ArgumentCaptor<InputStream> inputStreamArgumentCaptor;
    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;
    @Captor
    private ArgumentCaptor<UploadMaterialContext> uploadMaterialContextArgumentCaptor;
    @Captor
    private ArgumentCaptor<JsonObject> nowContentArgumentCaptor;

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

    @BeforeEach
    public void initMocks() {
        initReferenceData();
        initUserGroupsData();
        nowsRequestedEventProcessor = new NowsRequestedEventProcessor(
                this.sender,
                new DocumentGeneratorService(this.systemUserProvider,
                        this.documentGeneratorClientProducer,
                        this.objectToJsonObjectConverter,
                        this.fileStorer,
                        this.uploadMaterialService,
                        materialUrlGenerator,
                        applicationParameters,
                        nowDocumentValidator,
                        objectMapper,
                        this.materialService),
                this.jsonObjectToObjectConverter,
                this.objectToJsonObjectConverter,
                this.refDataService,
                this.usersGroupService);
    }

    @Test
    public void shouldGenerateNowAndStoreWithVisibleUserGroupInFileStore() throws IOException, FileServiceException {

        final UUID systemUserid = UUID.randomUUID();
        final NowDocumentRequest nowDocumentRequest = nowsRequestedTemplateWithVisibleUsers();
        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest)
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .build();

        final byte[] bytesIn = new byte[2];
        when(nowDocumentValidator.isPostable(any(OrderAddressee.class))).thenReturn(TRUE);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(bytesIn);
        when(materialUrlGenerator.pdfFileStreamUrlFor(any())).thenReturn(HTTP_MATERIAL_URL);
        when(applicationParameters.getEmailTemplateId(any())).thenReturn(randomUUID().toString());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserid));
        when(fileStorer.store(any(JsonObject.class), any(InputStream.class)))
                .thenReturn(fileId);
        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);

        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(this.documentGeneratorClient).generatePdfDocument(nowContentArgumentCaptor.capture(), any(), eq(systemUserid));
        verify(this.uploadMaterialService).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        verify(this.fileStorer).store(jsonObjectArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());
        assertThat(jsonObjectArgumentCaptor.getValue().getString(FILE_NAME), is(startsWith(nowDocumentRequest.getNowContent().getOrderName())));
        assertThat(inputStreamArgumentCaptor.getValue().read(new byte[2]), is(bytesIn.length));

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());
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

        final DefaultEnvelope<?> publicNowDocumentRequestedEnvelope = this.envelopeArgumentCaptor.getAllValues().get(1);
        assertThat(publicNowDocumentRequestedEnvelope.metadata().name(), is(PUBLIC_PROGRESSION_NOW_DOCUMENT_REQUESTED));
        assertThat(((JsonObject) publicNowDocumentRequestedEnvelope.payload()).getString("materialId"), is(nowDocumentRequested.getMaterialId().toString()));
        final String updatedNowContent = ((JsonObject) (nowContentArgumentCaptor.getValue().get(FINANCIAL_ORDER_DETAILS))).toString();
        with(updatedNowContent).assertThat(ACCOUNTING_DIVISION_CODE, is("077"));
    }

    @Test
    public void shouldGenerateNowAndStoreWithNotVisibleUserGroupInFileStore() throws IOException, FileServiceException {

        final UUID systemUserid = UUID.randomUUID();
        final NowDocumentRequest nowDocumentRequest = nowsRequestedTemplateWithNonVisibleUsers();
        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest)
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .build();
        final byte[] bytesIn = new byte[2];
        when(nowDocumentValidator.isPostable(any(OrderAddressee.class))).thenReturn(TRUE);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(bytesIn);
        when(materialUrlGenerator.pdfFileStreamUrlFor(any())).thenReturn(HTTP_MATERIAL_URL);
        when(applicationParameters.getEmailTemplateId(any())).thenReturn(randomUUID().toString());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserid));
        when(fileStorer.store(any(JsonObject.class), any(InputStream.class))).thenReturn(fileId);
        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);
        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(this.documentGeneratorClient).generatePdfDocument(any(), any(), eq(systemUserid));
        verify(this.uploadMaterialService).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        verify(this.fileStorer).store(jsonObjectArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());
        assertThat(jsonObjectArgumentCaptor.getValue().getString(FILE_NAME), is(startsWith(nowDocumentRequest.getNowContent().getOrderName())));
        assertThat(inputStreamArgumentCaptor.getValue().read(new byte[2]), is(bytesIn.length));

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());
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
    public void shouldGenerateNowAndStoreInFileStore() throws IOException, FileServiceException {

        final UUID systemUserid = UUID.randomUUID();
        final NowDocumentRequest nowDocumentRequest = nowsRequestedTemplate();
        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest)
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .build();

        final byte[] bytesIn = new byte[2];
        when(nowDocumentValidator.isPostable(any(OrderAddressee.class))).thenReturn(TRUE);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(bytesIn);
        when(materialUrlGenerator.pdfFileStreamUrlFor(any())).thenReturn(HTTP_MATERIAL_URL);
        when(applicationParameters.getEmailTemplateId(any())).thenReturn(randomUUID().toString());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserid));
        when(fileStorer.store(any(JsonObject.class), any(InputStream.class))).thenReturn(fileId);
        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);
        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(this.documentGeneratorClient).generatePdfDocument(any(), any(), eq(systemUserid));
        verify(this.uploadMaterialService).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        verify(this.fileStorer).store(jsonObjectArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());
        assertThat(jsonObjectArgumentCaptor.getValue().getString(FILE_NAME), is(startsWith(nowDocumentRequest.getNowContent().getOrderName())));
        assertThat(inputStreamArgumentCaptor.getValue().read(new byte[2]), is(bytesIn.length));

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());
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

    @Test
    public void shouldNotGenerateNowOnDocumentGenerationException() throws IOException {

        final NowDocumentRequest nowDocumentRequest = nowsRequestedTemplate();
        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest)
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .build();
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenThrow(new DocumentGenerationException());
        final UUID systemUserid = UUID.randomUUID();

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserid));

        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("progression.events.nows-requested")
                        .withUserId(USER_ID),
                objectToJsonObjectConverter.convert(nowDocumentRequested));

        try {
            this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);
            fail();
        } catch (final RuntimeException expected) {
            assertThat(expected.getMessage(), is("Progression : exception while generating NOWs document "));
        }
        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        final List<DefaultEnvelope<?>> allMessagesSent = envelopeArgumentCaptor.getAllValues();

        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(((JsonObject) allMessagesSent.get(0).payload()).getJsonObject(COURT_DOCUMENT), CourtDocument.class);

        assertThat((allMessagesSent.get(0).metadata().name()), is(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT));
        assertThat(courtDocument.getMaterials().get(0).getId().toString(), is(nowDocumentRequest.getMaterialId().toString()));
        assertThat(courtDocument.getMimeType(), is(APPLICATION_PDF));
        assertThat(courtDocument.getDocumentTypeDescription(), is(COURT_FINAL_ORDERS));
        assertThat((allMessagesSent.get(1).metadata().name()), is(PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS));
        assertThat(((JsonObject) allMessagesSent.get(1).payload()).getString("status"), is("failed"));
        assertThat(((JsonObject) allMessagesSent.get(1).payload()).getString("materialId"), is(nowDocumentRequest.getMaterialId().toString()));

        verifyNoMoreInteractions(this.fileStorer);
        verifyNoMoreInteractions(this.uploadMaterialService);

    }

    public JsonEnvelope envelope(final NowDocumentRequested nowDocumentRequested) {
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(nowDocumentRequested);
        return envelopeFrom(
                metadataWithRandomUUID("progression.event.nows-requested").withUserId(USER_ID),
                objectToJsonObjectConverter.convert(jsonObject)
        );
    }

    @Test
    public void shouldNotGenerateNowOnFileUploadException() throws IOException, FileServiceException {

        final NowDocumentRequest nowDocumentRequest = nowsRequestedTemplate();
        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest)
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .build();
        final byte[] bytesIn = new byte[2];
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        final UUID systemUserId = UUID.randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(bytesIn);
        doThrow(new FileUploadException()).when(fileStorer).store(any(), any());

        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);

        try {
            this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);
            fail();
        } catch (final RuntimeException expected) {
            assertThat(expected.getMessage(), is("Progression : exception while generating NOWs document "));
        }

        verify(this.documentGeneratorClient).generatePdfDocument(any(), any(), eq(systemUserId));
        verify(this.fileStorer).store(jsonObjectArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());
        assertThat(inputStreamArgumentCaptor.getValue().read(new byte[2]), is(bytesIn.length));


        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        final List<DefaultEnvelope<?>> allMessagesSent = envelopeArgumentCaptor.getAllValues();

        final JsonObject jsonObject = (JsonObject) allMessagesSent.get(0).payload();

        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject(COURT_DOCUMENT), CourtDocument.class);

        assertThat(courtDocument.getMaterials().get(0).getId().toString(), is(nowDocumentRequest.getMaterialId().toString()));
        assertThat(courtDocument.getMimeType(), is(APPLICATION_PDF));
        assertThat(courtDocument.getDocumentTypeDescription(), is(COURT_FINAL_ORDERS));

        verifyNoMoreInteractions(this.uploadMaterialService);
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
