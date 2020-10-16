package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.processor.NowsRequestedEventProcessor.PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT;
import static uk.gov.moj.cpp.progression.service.DocumentGeneratorService.PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNowDocumentRequestTemplate;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNowDocumentRequestTemplateWithNonVisibleUserList;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNowDocumentRequestTemplateWithVisibleUserList;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
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
import uk.gov.moj.cpp.progression.service.ReferenceDataService;
import uk.gov.moj.cpp.progression.service.UploadMaterialContext;
import uk.gov.moj.cpp.progression.service.UploadMaterialService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.service.exception.DocumentGenerationException;
import uk.gov.moj.cpp.progression.service.exception.FileUploadException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NowsRequestedEventProcessorTest {

    public static final String USER_ID = UUID.randomUUID().toString();

    public static final UUID fileId = UUID.randomUUID();
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
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private NowsRequestedEventProcessor nowsRequestedEventProcessor;
    @Mock
    private Sender sender;
    @Mock
    private UploadMaterialService uploadMaterialService;
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
    private ReferenceDataService refDataService;
    @Mock
    private UsersGroupService usersGroupService;
    @Mock
    private SystemUserProvider systemUserProvider;
    @Captor
    private ArgumentCaptor<DefaultEnvelope<?>> envelopeArgumentCaptor;
    @Captor
    private ArgumentCaptor<InputStream> inputStreamArgumentCaptor;
    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;
    @Captor
    private ArgumentCaptor<UploadMaterialContext> uploadMaterialContextArgumentCaptor;

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
                new DocumentGeneratorService(this.systemUserProvider,
                        this.documentGeneratorClientProducer,
                        this.objectToJsonObjectConverter,
                        this.fileStorer,
                        this.uploadMaterialService,
                        materialUrlGenerator,
                        applicationParameters),
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
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(bytesIn);
        when(materialUrlGenerator.pdfFileStreamUrlFor(any())).thenReturn("http://materialUrl");
        when(applicationParameters.getEmailTemplateId(anyString())).thenReturn(randomUUID().toString());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserid));
        when(fileStorer.store(Mockito.any(JsonObject.class), Mockito.any(InputStream.class)))
                .thenReturn(fileId);
        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);

        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(this.documentGeneratorClient).generatePdfDocument(any(), any(), eq(systemUserid));
        verify(this.uploadMaterialService).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        verify(this.fileStorer).store(jsonObjectArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());
        assertThat(jsonObjectArgumentCaptor.getValue().getString("fileName"), is(startsWith(nowDocumentRequest.getNowContent().getOrderName())));
        assertThat(inputStreamArgumentCaptor.getValue().read(new byte[2]), is(bytesIn.length));

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        final DefaultEnvelope<?> jsonEnvelope = this.envelopeArgumentCaptor.getValue();
        final JsonObject jsonObject = (JsonObject) jsonEnvelope.payload();
        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("courtDocument"), CourtDocument.class);
        assertThat(courtDocument.getMaterials().get(0).getUserGroups().size(), is(8));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Defence Users"));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Defence Lawyers"));
        assertThat(courtDocument.getMimeType(), is("application/pdf"));
        assertThat(courtDocument.getDocumentTypeDescription(), is("Court Final orders"));
        assertThat(courtDocument.getSeqNum(), is(3));
        assertThat(courtDocument.getDocumentTypeRBAC().getReadUserGroups().size(), is(8));
        assertTrue(courtDocument.getDocumentTypeRBAC().getReadUserGroups().contains("Defence Users"));
        assertTrue(courtDocument.getDocumentTypeRBAC().getReadUserGroups().contains("Defence Lawyers"));
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
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(bytesIn);
        when(materialUrlGenerator.pdfFileStreamUrlFor(any())).thenReturn("http://materialUrl");
        when(applicationParameters.getEmailTemplateId(anyString())).thenReturn(randomUUID().toString());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserid));
        when(fileStorer.store(Mockito.any(JsonObject.class), Mockito.any(InputStream.class))).thenReturn(fileId);
        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);
        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(this.documentGeneratorClient).generatePdfDocument(any(), any(), eq(systemUserid));
        verify(this.uploadMaterialService).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        verify(this.fileStorer).store(jsonObjectArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());
        assertThat(jsonObjectArgumentCaptor.getValue().getString("fileName"), is(startsWith(nowDocumentRequest.getNowContent().getOrderName())));
        assertThat(inputStreamArgumentCaptor.getValue().read(new byte[2]), is(bytesIn.length));

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        final DefaultEnvelope<?> jsonEnvelope = this.envelopeArgumentCaptor.getValue();
        final JsonObject jsonObject = (JsonObject) jsonEnvelope.payload();
        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("courtDocument"), CourtDocument.class);
        assertThat(courtDocument.getMaterials().get(0).getUserGroups().size(), is(9));
        assertFalse(courtDocument.getMaterials().get(0).getUserGroups().contains("Probation Admin"));
        assertFalse(courtDocument.getMaterials().get(0).getUserGroups().contains("Defence Users"));
        assertFalse(courtDocument.getMaterials().get(0).getUserGroups().contains("Chambers Clerk"));
        assertThat(courtDocument.getMimeType(), is("application/pdf"));
        assertThat(courtDocument.getDocumentTypeDescription(), is("Court Final orders"));
        assertThat(courtDocument.getSeqNum(), is(3));
        assertThat(courtDocument.getDocumentTypeRBAC().getReadUserGroups().size(), is(9));
        assertFalse(courtDocument.getDocumentTypeRBAC().getReadUserGroups().contains("Probation Admin"));
        assertFalse(courtDocument.getDocumentTypeRBAC().getReadUserGroups().contains("Defence Users"));
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
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(bytesIn);
        when(materialUrlGenerator.pdfFileStreamUrlFor(any())).thenReturn("http://materialUrl");
        when(applicationParameters.getEmailTemplateId(anyString())).thenReturn(randomUUID().toString());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserid));
        when(fileStorer.store(Mockito.any(JsonObject.class), Mockito.any(InputStream.class))).thenReturn(fileId);
        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequested);
        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(this.documentGeneratorClient).generatePdfDocument(any(), any(), eq(systemUserid));
        verify(this.uploadMaterialService).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        verify(this.fileStorer).store(jsonObjectArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());
        assertThat(jsonObjectArgumentCaptor.getValue().getString("fileName"), is(startsWith(nowDocumentRequest.getNowContent().getOrderName())));
        assertThat(inputStreamArgumentCaptor.getValue().read(new byte[2]), is(bytesIn.length));

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        final DefaultEnvelope<?> jsonEnvelope = this.envelopeArgumentCaptor.getValue();
        final JsonObject jsonObject = (JsonObject) jsonEnvelope.payload();
        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("courtDocument"), CourtDocument.class);

        assertThat(courtDocument.getMaterials().get(0).getUserGroups().size(), is(13));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Crown Court Admin"));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Magistrates"));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Court Associate"));
        assertThat(courtDocument.getMimeType(), is("application/pdf"));
        assertThat(courtDocument.getDocumentTypeDescription(), is("Court Final orders"));
        assertThat(courtDocument.getSeqNum(), is(3));
        assertThat(courtDocument.getDocumentTypeRBAC().getReadUserGroups().size(), is(13));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Crown Court Admin"));
        assertTrue(courtDocument.getMaterials().get(0).getUserGroups().contains("Magistrates"));
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

        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        final List<DefaultEnvelope<?>> allMessagesSent = envelopeArgumentCaptor.getAllValues();

        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(((JsonObject) allMessagesSent.get(0).payload()).getJsonObject("courtDocument"), CourtDocument.class);

        assertThat((allMessagesSent.get(0).metadata().name()), is(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT));
        assertThat(courtDocument.getMaterials().get(0).getId().toString(), is(nowDocumentRequest.getMaterialId().toString()));
        assertThat(courtDocument.getMimeType(), is("application/pdf"));
        assertThat(courtDocument.getDocumentTypeDescription(), is("Court Final orders"));
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

        this.nowsRequestedEventProcessor.processNowDocumentRequested(eventEnvelope);

        verify(this.documentGeneratorClient).generatePdfDocument(any(), any(), eq(systemUserId));
        verify(this.fileStorer).store(jsonObjectArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());
        assertThat(inputStreamArgumentCaptor.getValue().read(new byte[2]), is(bytesIn.length));


        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        final List<DefaultEnvelope<?>> allMessagesSent = envelopeArgumentCaptor.getAllValues();

        final JsonObject jsonObject = (JsonObject) allMessagesSent.get(0).payload();

        final CourtDocument courtDocument = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("courtDocument"), CourtDocument.class);

        assertThat(courtDocument.getMaterials().get(0).getId().toString(), is(nowDocumentRequest.getMaterialId().toString()));
        assertThat(courtDocument.getMimeType(), is("application/pdf"));
        assertThat(courtDocument.getDocumentTypeDescription(), is("Court Final orders"));

        verifyNoMoreInteractions(this.uploadMaterialService);
    }

    private void initReferenceData() {
        final JsonObject docTypeData = Json.createObjectBuilder()
                .add("section", "Court Final orders")
                .add("seqNum", 3)
                .add("courtDocumentTypeRBAC",
                        Json.createObjectBuilder()
                                .add("uploadUserGroups", createArrayBuilder().build())
                                .add("readUserGroups", createArrayBuilder()
                                        .add(buildUserGroup("Magistrates"))
                                        .add(buildUserGroup("Court Clerk")).build())
                                .add("downloadUserGroups", createArrayBuilder()
                                        .add(buildUserGroup("Magistrates")).build()).build())
                .build();

        when(refDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(Optional.of(docTypeData));
    }

    private void initUserGroupsData() {
        try {
            String response = Resources.toString(Resources.getResource("usersgroups.get-groups-with-organisation.json"), Charset.defaultCharset());
            final JsonObject userGroups = new StringToJsonObjectConverter().convert(response);
            when(usersGroupService.getGroupsWithOrganisation(any())).thenReturn(userGroups);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
