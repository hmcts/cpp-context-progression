package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.processor.NowsRequestedEventProcessor.PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT;
import static uk.gov.moj.cpp.progression.service.DocumentGeneratorService.PROGRESSION_UPDATE_NOWS_MATERIAL_STATUS;
import static uk.gov.moj.cpp.progression.service.DocumentGeneratorService.RESULTS_UPDATE_NOWS_MATERIAL_STATUS;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNowsRequestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.justice.core.courts.CreateNowsRequest;
import uk.gov.justice.core.courts.NotificationDocumentState;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.NowsRequested;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.UploadMaterialContext;
import uk.gov.moj.cpp.progression.service.UploadMaterialContextBuilder;
import uk.gov.moj.cpp.progression.service.UploadMaterialService;
import uk.gov.moj.cpp.progression.service.exception.DocumentGenerationException;
import uk.gov.moj.cpp.progression.service.exception.FileUploadException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NowsRequestedEventProcessorTest {

    public static final String USER_ID = UUID.randomUUID().toString();

    public static final UUID fileId = UUID.randomUUID();

    private NowsRequestedEventProcessor nowsRequestedEventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope responseEnvelope;

    @Mock
    private UploadMaterialService uploadMaterialService;

    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Mock
    private DocumentGeneratorClient documentGeneratorClient;

    @Mock
    private FileStorer fileStorer;

    @Mock
    private SystemUserProvider systemUserProvider;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<InputStream> inputStreamArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Captor
    private ArgumentCaptor<UploadMaterialContext> uploadMaterialContextArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> caseIdCaptor;

    @Captor
    private ArgumentCaptor<UUID> uuidArgumentCaptor;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        nowsRequestedEventProcessor = new NowsRequestedEventProcessor(this.enveloper,
                this.sender,
                new DocumentGeneratorService(this.systemUserProvider, this.documentGeneratorClientProducer,
                        this.objectToJsonObjectConverter, this.fileStorer, this.uploadMaterialService),
                this.jsonObjectToObjectConverter,
                this.objectToJsonObjectConverter
        );
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldGenerateNowAndStoreInFileStore() throws IOException, FileServiceException {

        final UUID systemUserid = UUID.randomUUID();
        final NowsRequested nowsRequested = nowsRequestedTemplate();
        final CreateNowsRequest createNowsRequest = nowsRequested.getCreateNowsRequest();

        final byte[] bytesIn = new byte[2];
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(bytesIn);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserid));
        when(fileStorer.store(Mockito.any(JsonObject.class), Mockito.any(InputStream.class)))
                .thenReturn(fileId);
        final JsonEnvelope eventEnvelope = envelope(createNowsRequest);
        this.nowsRequestedEventProcessor.processNowsRequested( eventEnvelope );

        verify(this.documentGeneratorClient).generatePdfDocument(any(), any(), eq(systemUserid));
        verify(this.uploadMaterialService).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        verify(this.fileStorer).store(jsonObjectArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        assertThat(jsonObjectArgumentCaptor.getValue().getString("fileName"),
                is(startsWith(createNowsRequest.getNowTypes().get(0).getDescription())));
        assertThat(inputStreamArgumentCaptor.getValue().read(new byte[2]), is(bytesIn.length));
        final NotificationDocumentState nowsNotificationDocumentState = uploadMaterialContextArgumentCaptor.getValue().getNowsNotificationDocumentState();
        assertThat(normalizeName(nowsNotificationDocumentState.getDefendantName()), is(normalizeName(createNowsRequest.getNows().get(0).getRequestedMaterials().get(0).getNowVariantDefendant().getName())));
    }

    @Test
    public void shouldNotGenerateNowOnDocumentGenerationException() throws IOException, FileServiceException {

        final CreateNowsRequest createNowsRequest = nowsRequestedTemplate().getCreateNowsRequest();

        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenThrow(new DocumentGenerationException());
        final UUID systemUserid = UUID.randomUUID();

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserid));

        this.nowsRequestedEventProcessor.processNowsRequested(envelopeFrom(metadataWithRandomUUID("hearing.events.nows-requested").withUserId(USER_ID),
                Json.createObjectBuilder().add("createNowsRequest", objectToJsonObjectConverter.convert(createNowsRequest)).build()
                ));

        verify(this.sender, times(3)).send(this.envelopeArgumentCaptor.capture());

        final List<JsonEnvelope> allMessagesSent = envelopeArgumentCaptor.getAllValues();

        assertThat(allMessagesSent.get(0), jsonEnvelope(
                metadata().withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT),
                payloadIsJson(allOf(
                        withJsonPath("$.courtDocument.materials[0].id", is(createNowsRequest.getNows().get(0).getRequestedMaterials().get(0).getMaterialId().toString()))
                        ))));

        assertThat(allMessagesSent.get(1), jsonEnvelope(
                metadata().withName(PROGRESSION_UPDATE_NOWS_MATERIAL_STATUS),
                payloadIsJson(allOf(withJsonPath("$.hearing_id", is(createNowsRequest.getHearing().getId().toString())),
                        withJsonPath("$.material_id",
                                is(createNowsRequest.getNows().get(0).getRequestedMaterials().get(0).getMaterialId().toString()))))));

        assertThat(allMessagesSent.get(2), jsonEnvelope(
                metadata().withName(RESULTS_UPDATE_NOWS_MATERIAL_STATUS),
                payloadIsJson(allOf(withJsonPath("$.hearing_id", is(createNowsRequest.getHearing().getId().toString())),
                        withJsonPath("$.material_id",
                                is(createNowsRequest.getNows().get(0).getRequestedMaterials().get(0).getMaterialId().toString()))))));

        verifyNoMoreInteractions(this.fileStorer);
        verifyNoMoreInteractions(this.uploadMaterialService);

    }

    public JsonEnvelope envelope(final CreateNowsRequest createNowsRequest) {
        final JsonObject jsonObject = Json.createObjectBuilder().add("createNowsRequest", objectToJsonObjectConverter.convert(createNowsRequest)).build();
        return envelopeFrom(
                metadataWithRandomUUID("hearing.events.nows-requested").withUserId(USER_ID),
                objectToJsonObjectConverter.convert(jsonObject)
        );
    }

    @Test
    public void shouldNotGenerateNowOnFileUploadException() throws IOException, FileServiceException {

        final CreateNowsRequest createNowsRequest = nowsRequestedTemplate().getCreateNowsRequest();

        final byte[] bytesIn = new byte[2];
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        UUID systemUserId = UUID.randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(bytesIn);
        doThrow(new FileUploadException()).when(fileStorer).store(any(), any());
        this.nowsRequestedEventProcessor.processNowsRequested(envelope(createNowsRequest));

        verify(this.documentGeneratorClient).generatePdfDocument(any(), any(), eq(systemUserId));
        verify(this.fileStorer).store(jsonObjectArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());
        assertThat(inputStreamArgumentCaptor.getValue().read(new byte[2]), is(bytesIn.length));


        verify(this.sender, times(3)).send(this.envelopeArgumentCaptor.capture());

        final List<JsonEnvelope> allMessagesSent = envelopeArgumentCaptor.getAllValues();

        assertThat(allMessagesSent.get(0), jsonEnvelope(
                metadata().withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT),
                payloadIsJson(allOf(
                        withJsonPath("$.courtDocument.materials[0].id", is(createNowsRequest.getNows().get(0).getRequestedMaterials().get(0).getMaterialId().toString()))
                ))));
        
        assertThat(allMessagesSent.get(1), jsonEnvelope(
                metadata().withName(PROGRESSION_UPDATE_NOWS_MATERIAL_STATUS),
                payloadIsJson(allOf(withJsonPath("$.hearing_id", is(createNowsRequest.getHearing().getId().toString())),
                        withJsonPath("$.material_id",
                                is(createNowsRequest.getNows().get(0).getRequestedMaterials().get(0).getMaterialId().toString()))))));

        assertThat(allMessagesSent.get(2), jsonEnvelope(
                metadata().withName(RESULTS_UPDATE_NOWS_MATERIAL_STATUS),
                payloadIsJson(allOf(withJsonPath("$.hearing_id", is(createNowsRequest.getHearing().getId().toString())),
                        withJsonPath("$.material_id",
                                is(createNowsRequest.getNows().get(0).getRequestedMaterials().get(0).getMaterialId().toString()))))));
        verifyNoMoreInteractions(this.uploadMaterialService);
    }

    /**
     * GPE-6752 implement nowMaterial status updated
     *
     * @Test public void shouldRaiseAnPublicEventNowsMaterialStatusWasUpdated() {
     * <p>
     * final NowsMaterialStatusUpdated nowsMaterialStatusUpdated = new NowsMaterialStatusUpdated(UUID.randomUUID(),
     * UUID.randomUUID(), "GENERATED_STATUS_VALUE");
     * <p>
     * final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("hearing.events.nows-material-status-updated").withUserId(USER_ID),
     * objectToJsonObjectConverter.convert(nowsMaterialStatusUpdated));
     * <p>
     * this.nowsRequestedEventProcessor.propagateNowsMaterialStatusUpdated(event);
     * <p>
     * final ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
     * <p>
     * verify(sender).send(jsonEnvelopeCaptor.capture());
     * <p>
     * assertThat(jsonEnvelopeCaptor.getValue().metadata().name(), is("public.hearing.events.nows-material-status-updated"));
     * assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("materialId"), is(nowsMaterialStatusUpdated.getMaterialId().toString()));
     * <p>
     * }
     **/
    private String normalizeName(String name) {
        return name.replaceAll(" ", "").toLowerCase();
    }

    public static NowsRequested nowsRequestedTemplate() {
        return NowsRequested.nowsRequested()
                .withCreateNowsRequest(generateNowsRequestTemplate(UUID.randomUUID()))
                .build();
    }
}
