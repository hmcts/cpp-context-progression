package uk.gov.moj.cpp.progression.service;

import static com.google.common.io.Resources.getResource;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.service.DocumentGeneratorService.ACCOUNTING_DIVISION_CODE;
import static uk.gov.moj.cpp.progression.service.DocumentGeneratorService.NCES_DOCUMENT_TEMPLATE_NAME;

import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.core.courts.NowsDocumentGenerated;
import uk.gov.justice.core.courts.nces.DocumentContent;
import uk.gov.justice.core.courts.nces.NcesNotificationRequested;
import uk.gov.justice.core.courts.nowdocument.NowDistribution;
import uk.gov.justice.core.courts.nowdocument.Nowaddress;
import uk.gov.justice.core.courts.nowdocument.OrderAddressee;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.event.nows.order.Address;
import uk.gov.moj.cpp.progression.event.nows.order.Cases;
import uk.gov.moj.cpp.progression.event.nows.order.Defendant;
import uk.gov.moj.cpp.progression.event.nows.order.DefendantCaseOffences;
import uk.gov.moj.cpp.progression.event.nows.order.NowsDocumentOrder;
import uk.gov.moj.cpp.progression.service.utils.NowDocumentValidator;
import uk.gov.moj.cpp.progression.test.TestTemplates;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

    @Mock
    private NowDocumentValidator nowDocumentValidator;

    @Captor
    ArgumentCaptor<JsonObject> fileStorerMetaDataCaptor;

    @Captor
    ArgumentCaptor<InputStream> fileStorerInputStreamCaptor;

    @Captor
    ArgumentCaptor<UploadMaterialContext> uploadMaterialContextArgumentCaptor;

    @Captor
    ArgumentCaptor<JsonObject> nowDocumentContentArgumentCaptor;

    @Captor
    ArgumentCaptor<DocumentContent> documentContentArgumentCaptorr;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();


    @Mock
    private MaterialService materialService;

    @Captor
    ArgumentCaptor<UUID> fileIdmaterialServiceCaptor;

    @Captor
    ArgumentCaptor<UUID> materialIdmaterialServiceCaptor;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();


    @Test
    public void shouldGenerateNowDocumentWithLetterFlagsAsTrueWhenDocumentPostable() throws Exception {
        when(nowDocumentValidator.isPostable(any(OrderAddressee.class))).thenReturn(TRUE);
        shouldGenerateNow(TRUE);
    }

    @Test
    public void shouldGenerateNowDocumentWithLetterFlagsAsFalseWhenDocumentNotPostable() {
        when(nowDocumentValidator.isPostable(any(OrderAddressee.class))).thenReturn(FALSE);
        shouldGenerateNow(FALSE);
    }


    public void shouldGenerateNow(final boolean expectedLetterFlag) {
        when(materialUrlGenerator.pdfFileStreamUrlFor(any())).thenReturn("http://materialUrl");

        when(applicationParameters.getEmailTemplateId(anyString())).thenReturn(randomUUID().toString());

        final UUID userId = randomUUID();

        final NowsDocumentGenerated nowsDocumentGenerated = NowsDocumentGenerated.nowsDocumentGenerated()
                .withMaterialId(randomUUID())
                .withHearingId(randomUUID())
                .withSystemDocGeneratorId(randomUUID())
                .withFileName(randomUUID().toString())
                .withUserId(userId)
                .withNowDistribution(NowDistribution.nowDistribution()
                        .withEmail(true)
                        .withFirstClassLetter(expectedLetterFlag)
                        .withSecondClassLetter(expectedLetterFlag)
                        .withEmailTemplateName("now_extradition_sla_template")
                        .build())
                .withOrderAddressee(OrderAddressee.orderAddressee()
                        .withAddress(Nowaddress.nowaddress()
                                .withEmailAddress1("emailAddress1@test.com")
                                .withEmailAddress2("emailAddress2@test.com")
                                .build()).build())
                .withCpsProsecutionCase(false)
                .build();

        final Set<DefendantCaseOffences> defendantCaseOffences = new HashSet<>();
        defendantCaseOffences.add(DefendantCaseOffences.defendantCaseOffences()
                .withWording("Test")
                .build());

        final NowsDocumentOrder nowsDocumentOrder = NowsDocumentOrder.nowsDocumentOrder()
                .withCourtCentreName("CourtCenter 1")
                .withCases(Arrays.asList(Cases.cases()
                        .withUrn("CaseUrn1")
                        .withDefendantCaseOffences(defendantCaseOffences)
                        .build()))
                .withDefendant(Defendant.defendant()
                        .withName("Defendant1")
                        .withDateOfBirth("20-09-1978")
                        .withAddress(Address.address()
                                .withLine1("78")
                                .withLine2("Address1")
                                .withLine3("Address2")
                                .withLine4("Address4")
                                .withLine5("Address5")
                                .withPostCode("XXXXX")
                                .build())
                        .build())
                .build();

        documentGeneratorService.addDocumentToMaterial(sender, originatingEnvelope, nowsDocumentGenerated);


        verify(uploadMaterialService, times(1)).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        UploadMaterialContext uploadMaterialContext = uploadMaterialContextArgumentCaptor.getValue();
        assertThat(uploadMaterialContext.getMaterialId(), is(nowsDocumentGenerated.getMaterialId()));
        assertThat(uploadMaterialContext.getEmailNotifications().size(), is(2));
        assertThat(uploadMaterialContext.getEmailNotifications().get(0).getSendToAddress(), is("emailAddress1@test.com"));
        assertThat(uploadMaterialContext.getEmailNotifications().get(1).getSendToAddress(), is("emailAddress2@test.com"));
        assertThat(uploadMaterialContext.isFirstClassLetter(), is(expectedLetterFlag));
        assertThat(uploadMaterialContext.isSecondClassLetter(), is(expectedLetterFlag));
    }

    @Test
    public void shouldGenerateNces() throws Exception {
        final NcesNotificationRequested ncesNotificationRequested = TestTemplates.generateNcesNotificationRequested();
        final JsonObject ncesDocumentContent = createNcesDocumentContent();
        final UUID systemUserId = randomUUID();
        final byte[] documentData = {34, 56, 78, 90};
        when(objectToJsonObjectConverter.convert(any())).thenReturn(ncesDocumentContent);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(ncesDocumentContent, NCES_DOCUMENT_TEMPLATE_NAME, systemUserId))
                .thenReturn(documentData);

        final UUID userId = randomUUID();

        when(documentGeneratorClient.generatePdfDocument(ncesDocumentContent, NCES_DOCUMENT_TEMPLATE_NAME, systemUserId)).thenReturn(documentData);

        documentGeneratorService.generateNcesDocument(sender, originatingEnvelope, userId, ncesNotificationRequested);

        verify(fileStorer, times(1)).store(fileStorerMetaDataCaptor.capture(), fileStorerInputStreamCaptor.capture());

        byte[] dataSent = new byte[documentData.length];
        fileStorerInputStreamCaptor.getValue().read(dataSent, 0, documentData.length);
        assertThat(documentData, is(dataSent));

        verify(uploadMaterialService, times(1)).uploadFile(uploadMaterialContextArgumentCaptor.capture());
        UploadMaterialContext uploadMaterialContext = uploadMaterialContextArgumentCaptor.getValue();
        assertThat(uploadMaterialContext.getMaterialId(), is(ncesNotificationRequested.getMaterialId()));
        assertThat(uploadMaterialContext.getCaseId(), is(ncesNotificationRequested.getCaseId()));

        verify(documentGeneratorClient, times(1)).generatePdfDocument(nowDocumentContentArgumentCaptor.capture(), anyString(), any(UUID.class));

        verify(objectToJsonObjectConverter, times(1)).convert(documentContentArgumentCaptorr.capture());
        DocumentContent documentContentContext = documentContentArgumentCaptorr.getValue();
        assertThat(documentContentContext.getDivisionCode(), is("77"));
    }

    private JsonObject createNowDocumentContent() {
        return Json.createObjectBuilder()
                .add("defendant", Json.createObjectBuilder().add("address", Json.createObjectBuilder().add("emailAddress1", "emailAddress1@test.com")
                        .add("emailAddress1", "emailAddress1@test.com").build()).build())
                .add("financialOrderDetails",
                        Json.createObjectBuilder().add(ACCOUNTING_DIVISION_CODE, "77").build())
                .build();
    }

    private JsonObject createNcesDocumentContent() {
        return Json.createObjectBuilder()
                .add("defendant", Json.createObjectBuilder().add("address", Json.createObjectBuilder().add("emailAddress1", "emailAddress1@test.com")
                        .add("emailAddress1", "emailAddress1@test.com").build()).build())
                .add(ACCOUNTING_DIVISION_CODE, "77")
                .build();
    }

    @Test
    public void shouldGenerateFormDocument() throws Exception {
        final byte[] documentData = {34, 56, 78, 90};
        final UUID systemUserId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID fileId = randomUUID();
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(fileStorer.store(any(), any())).thenReturn(fileId);

        final String inputEvent = Resources.toString(getResource("finalised-form-data-with-welsh-data.json"), defaultCharset());
        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent);
        JsonArray formDataArray = readData.getJsonArray("finalisedFormData");

        final JsonObject formData = formDataArray.getJsonObject(0);
        when(documentGeneratorClient.generatePdfDocument(formData, DocumentTemplateType.BETTER_CASE_MANAGEMENT.getTemplateName(), systemUserId))
                .thenReturn(documentData);

        final String fileName = documentGeneratorService.generateFormDocument(originatingEnvelope, FormType.BCM, formData, materialId);
        assertThat(fileName, anyOf(is("Jane JOHNSON, 22 May 1976, created on 22 December 10:45 2022.pdf"),
                is("Kris kidman, 14 June 1981, created on 22 December 10:45 2022.pdf")));
        verify(materialService, times(1)).uploadMaterial(fileIdmaterialServiceCaptor.capture(), materialIdmaterialServiceCaptor.capture(), (JsonEnvelope) any());
        final UUID capturedFileId = fileIdmaterialServiceCaptor.getValue();
        final UUID capturedMaterialId = materialIdmaterialServiceCaptor.getValue();
        assertThat(capturedFileId, is(fileId));
        assertThat(capturedMaterialId, is(materialId));


        final JsonObject formDataWelsh = formDataArray.getJsonObject(1);
        when(documentGeneratorClient.generatePdfDocument(formDataWelsh, DocumentTemplateType.BETTER_CASE_MANAGEMENT_WELSH.getTemplateName(), systemUserId))
                .thenReturn(documentData);

        final String fileNameWithWelsh = documentGeneratorService.generateFormDocument(originatingEnvelope, FormType.BCM, formDataWelsh, materialId);
        assertThat(fileNameWithWelsh, anyOf(is("Joseph Stallion, 22 May 1954, created (welsh) on 22 December 10:45 2022.pdf")));


        final JsonObject formDataLast = formDataArray.getJsonObject(2);
        when(documentGeneratorClient.generatePdfDocument(formDataLast, DocumentTemplateType.BETTER_CASE_MANAGEMENT.getTemplateName(), systemUserId))
                .thenReturn(documentData);

        final String fileNameLast = documentGeneratorService.generateFormDocument(originatingEnvelope, FormType.BCM, formDataLast, materialId);
        assertThat(fileNameLast, anyOf(is("Jane JOHNSON, 22 May 1976, created on 22 December 10:45 2022.pdf"),
                is("Kris kidman, 14 June 1981, created on 22 December 10:45 2022.pdf")));

    }

    @Test
    public void shouldGenerateDisqualificationWarning() throws Exception {

        final byte[] documentData = {34, 56, 78, 90};
        final String fileName = "filename";

        when(fileStorer.store(any(), any())).thenReturn(randomUUID());

        documentGeneratorService.generatePdfDocument(originatingEnvelope, fileName, documentData);
        verify(fileStorer, times(1)).store(fileStorerMetaDataCaptor.capture(), fileStorerInputStreamCaptor.capture());

        byte[] dataSent = new byte[documentData.length];
        fileStorerInputStreamCaptor.getValue().read(dataSent, 0, documentData.length);
        assertThat(documentData, is(dataSent));

        verify(materialService, times(1)).uploadMaterial(fileIdmaterialServiceCaptor.capture(), materialIdmaterialServiceCaptor.capture(), (JsonEnvelope) any());
        final UUID capturedFileId = fileIdmaterialServiceCaptor.getValue();
        final UUID capturedMaterialId = materialIdmaterialServiceCaptor.getValue();
        assertThat(capturedFileId, is(notNullValue()));
        assertThat(capturedMaterialId, is(notNullValue()));
    }

}