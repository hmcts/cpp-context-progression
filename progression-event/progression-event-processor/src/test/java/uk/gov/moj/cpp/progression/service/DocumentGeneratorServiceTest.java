package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.service.DocumentGeneratorService.NCES_DOCUMENT_TEMPLATE_NAME;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.nces.NcesNotificationRequested;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.event.nows.order.NowsDocumentOrder;
import uk.gov.moj.cpp.progression.test.TestTemplates;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

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
    private MaterialUrlGenerator materialUrlGenerator;

    @Mock
    private ApplicationParameters applicationParameters;

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
    public void shouldGenerateNow() throws Exception {

        final NowDocumentRequest nowDocumentRequest = TestTemplates.generateNowDocumentRequestTemplate(randomUUID(), JurisdictionType.CROWN, false);

        final UUID systemUserId = randomUUID();
        final byte[] documentData = {34, 56, 78, 90};
        final JsonObject nowsDocumentOrderJson = mock(JsonObject.class);

        when(objectToJsonObjectConverter.convert(nowDocumentRequest.getNowContent())).thenReturn(nowsDocumentOrderJson);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(documentData);
        when(fileStorer.store(any(), any())).thenReturn(randomUUID());
        when(materialUrlGenerator.pdfFileStreamUrlFor(any())).thenReturn("http://materialUrl");
        when(applicationParameters.getEmailTemplateId(anyString())).thenReturn(randomUUID().toString());

        final UUID userId = randomUUID();

        final NowsDocumentOrder nowsDocumentOrder = NowsDocumentOrder.nowsDocumentOrder().build();

        when(objectToJsonObjectConverter.convert(nowsDocumentOrder)).thenReturn(nowsDocumentOrderJson);

        when(documentGeneratorClient.generatePdfDocument(nowsDocumentOrderJson, nowDocumentRequest.getTemplateName(), systemUserId)).thenReturn(documentData);

        documentGeneratorService.generateNow(sender, originatingEnvelope, userId, nowDocumentRequest);

        verify(fileStorer, times(1)).store(fileStorerMetaDataCaptor.capture(), fileStorerInputStreamCaptor.capture());

        byte[] dataSent = new byte[documentData.length];
        fileStorerInputStreamCaptor.getValue().read(dataSent, 0, documentData.length);
        assertThat(documentData, is(dataSent));

        verify(uploadMaterialService, times(1)).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        UploadMaterialContext uploadMaterialContext = uploadMaterialContextArgumentCaptor.getValue();
        assertThat(uploadMaterialContext.getMaterialId(), is(nowDocumentRequest.getMaterialId()));
        assertThat(uploadMaterialContext.getEmailNotifications().size(), is(2));
        assertThat(uploadMaterialContext.getEmailNotifications().get(0).getSendToAddress(), is("emailAddress1@test.com"));
        assertThat(uploadMaterialContext.getEmailNotifications().get(1).getSendToAddress(), is("emailAddress2@test.com"));
    }

    @Test
    public void shouldGenerateNces() throws Exception {
        final NcesNotificationRequested ncesNotificationRequested = TestTemplates.generateNcesNotificationRequested();

        final UUID systemUserId = randomUUID();
        final byte[] documentData = {34, 56, 78, 90};
        final JsonObject ncesDocumentOrderJson = mock(JsonObject.class);

        when(objectToJsonObjectConverter.convert(ncesNotificationRequested.getDocumentContent())).thenReturn(ncesDocumentOrderJson);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(ncesDocumentOrderJson, NCES_DOCUMENT_TEMPLATE_NAME, systemUserId))
                .thenReturn(documentData);

        final UUID userId = randomUUID();

        when(documentGeneratorClient.generatePdfDocument(ncesDocumentOrderJson, NCES_DOCUMENT_TEMPLATE_NAME, systemUserId)).thenReturn(documentData);

        documentGeneratorService.generateNcesDocument(sender, originatingEnvelope, userId, ncesNotificationRequested);

        verify(fileStorer, times(1)).store(fileStorerMetaDataCaptor.capture(), fileStorerInputStreamCaptor.capture());

        byte[] dataSent = new byte[documentData.length];
        fileStorerInputStreamCaptor.getValue().read(dataSent, 0, documentData.length);
        assertThat(documentData, is(dataSent));

        verify(uploadMaterialService, times(1)).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        UploadMaterialContext uploadMaterialContext = uploadMaterialContextArgumentCaptor.getValue();
        assertThat(uploadMaterialContext.getMaterialId(), is(ncesNotificationRequested.getMaterialId()));
        assertThat(uploadMaterialContext.getCaseId(), is(ncesNotificationRequested.getCaseId()));
    }

}
