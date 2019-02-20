package uk.gov.moj.cpp.progression.service;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CreateNowsRequest;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.NowType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.event.nows.order.NowsDocumentOrder;
import uk.gov.moj.cpp.progression.processor.NowsNotificationDocumentState;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DocumentGeneratorServiceTest {

    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private FileStorer fileStorer;

    @Mock
    private UploadMaterialService uploadMaterialService;

    @Mock
    private SystemUserProvider systemUserProvider;

    @InjectMocks
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope originatingEnvelope;

    @Mock
    private DocumentGeneratorClient documentGeneratorClient;

    @Captor
    ArgumentCaptor<JsonObject> fileStorerMetaDataCaptor;

    @Captor
    ArgumentCaptor<InputStream> fileStorerInputStreamCaptor;

    @Captor
    ArgumentCaptor<UploadMaterialContext> uploadMaterialContextArgumentCaptor;

    @Test
    public void testGenerateNowNonRemote() throws Exception {
        test(false);
    }

    @Test
    public void testGenerateNowRemote() throws Exception {
        test(true);
    }

    public void test(final boolean isRemotePrintingRequired) throws Exception {

        final UUID systemUserId = UUID.randomUUID();
        final String templateName = "testTemplate";
        final byte[] documentData = {34, 56, 78, 90};
        final JsonObject nowsDocumentOrderJson = mock(JsonObject.class);

        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(nowsDocumentOrderJson, "templateIdentifier", systemUserId))
                .thenReturn(documentData);

        final UUID userId = UUID.randomUUID();
        final UUID nowsTypeId = UUID.randomUUID();
        final CreateNowsRequest nowsRequested = CreateNowsRequest.createNowsRequest()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(UUID.randomUUID())
                                        .build()
                        ))
                        .build())
                .withNowTypes(asList(
                        NowType.nowType()
                                .withId(nowsTypeId)
                                .withTemplateName(templateName)
                                .build()
                ))
                .build();
        final UUID hearingId = UUID.randomUUID();
        final Map<NowsDocumentOrder, NowsNotificationDocumentState> nowsDocumentOrderToNotificationState = new HashMap<>();
        final NowsNotificationDocumentState nowsNotificationDocumentState = (new NowsNotificationDocumentState())
                .setMaterialId(UUID.randomUUID())
                .setIsRemotePrintingRequired(isRemotePrintingRequired)
                .setOrderName("orderName")
                .setNowsTypeId(nowsTypeId);
        final NowsDocumentOrder nowsDocumentOrder = NowsDocumentOrder.nowsDocumentOrder()
                .build();

        when(objectToJsonObjectConverter.convert(nowsDocumentOrder)).thenReturn(nowsDocumentOrderJson);

        nowsDocumentOrderToNotificationState.put(nowsDocumentOrder, nowsNotificationDocumentState);

        when(documentGeneratorClient.generatePdfDocument(nowsDocumentOrderJson, templateName, systemUserId)).thenReturn(documentData);

        documentGeneratorService.generateNow(sender, originatingEnvelope, userId, nowsRequested,
                hearingId.toString(), nowsDocumentOrderToNotificationState, nowsDocumentOrder);

        verify(fileStorer, times(1)).store(fileStorerMetaDataCaptor.capture(), fileStorerInputStreamCaptor.capture());

        byte datasent[] = new byte[documentData.length];
        fileStorerInputStreamCaptor.getValue().read(datasent, 0, documentData.length);
        Assert.assertArrayEquals(documentData, datasent);

        verify(uploadMaterialService, times(1)).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        UploadMaterialContext uploadMaterialContext = uploadMaterialContextArgumentCaptor.getValue();
        Assert.assertEquals(uploadMaterialContext.isRemotePrintingRequired(), isRemotePrintingRequired);
        Assert.assertEquals(uploadMaterialContext.getMaterialId(), nowsNotificationDocumentState.getMaterialId());
        Assert.assertEquals(uploadMaterialContext.getCaseId(), nowsRequested.getHearing().getProsecutionCases().get(0).getId());

    }

}
