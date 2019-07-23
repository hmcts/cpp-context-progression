package uk.gov.moj.cpp.progression.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.event.nows.order.NowsDocumentOrder;
import uk.gov.moj.cpp.progression.test.TestTemplates;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.InputStream;
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

    private void test(final boolean isRemotePrintingRequired) throws Exception {
        final NowDocumentRequest nowDocumentRequest = TestTemplates.generateNowDocumentRequestTemplate(UUID.randomUUID(),
                JurisdictionType.CROWN, false, isRemotePrintingRequired);

        final UUID systemUserId = UUID.randomUUID();
        final byte[] documentData = {34, 56, 78, 90};
        final JsonObject nowsDocumentOrderJson = mock(JsonObject.class);

        when(objectToJsonObjectConverter.convert(nowDocumentRequest.getNowContent())).thenReturn(nowsDocumentOrderJson);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(nowsDocumentOrderJson, nowDocumentRequest.getTemplateName(), systemUserId))
                .thenReturn(documentData);

        final UUID userId = UUID.randomUUID();

        final NowsDocumentOrder nowsDocumentOrder = NowsDocumentOrder.nowsDocumentOrder()
                .build();

        when(objectToJsonObjectConverter.convert(nowsDocumentOrder)).thenReturn(nowsDocumentOrderJson);

        when(documentGeneratorClient.generatePdfDocument(nowsDocumentOrderJson, nowDocumentRequest.getTemplateName(), systemUserId)).thenReturn(documentData);

        documentGeneratorService.generateNow(sender, originatingEnvelope, userId, nowDocumentRequest);

        verify(fileStorer, times(1)).store(fileStorerMetaDataCaptor.capture(), fileStorerInputStreamCaptor.capture());

        byte datasent[] = new byte[documentData.length];
        fileStorerInputStreamCaptor.getValue().read(datasent, 0, documentData.length);
        Assert.assertArrayEquals(documentData, datasent);

        verify(uploadMaterialService, times(1)).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        UploadMaterialContext uploadMaterialContext = uploadMaterialContextArgumentCaptor.getValue();
        Assert.assertEquals(uploadMaterialContext.isRemotePrintingRequired(), isRemotePrintingRequired);
        Assert.assertEquals(uploadMaterialContext.getMaterialId(), nowDocumentRequest.getMaterialId());
        Assert.assertEquals(uploadMaterialContext.getCaseId(), nowDocumentRequest.getCaseId());


    }

}
