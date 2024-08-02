package uk.gov.moj.cpp.progression.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.sun.mail.iap.Argument;
import org.apache.commons.validator.Arg;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterDocumentRequest;
import uk.gov.justice.progression.courts.CourtRegisterGenerated;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.processor.CourtRegisterEventProcessor.COURT_REGISTER_TEMPLATE;

@RunWith(MockitoJUnitRunner.class)
public class CourtRegisterEventProcessorTest {
    @InjectMocks
    private CourtRegisterEventProcessor courtRegisterEventProcessor;

    @Mock
    private FileStorer fileStorer;

    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Mock
    private CourtRegisterPdfPayloadGenerator courtRegisterPdfPayloadGenerator;

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private MaterialService materialService;

    @Mock
    private NotificationNotifyService notificationNotifyService;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private DocumentGeneratorClient documentGeneratorClient;

    @Captor
    private ArgumentCaptor<JsonObject> filestorerMetadata;

    @Captor
    private ArgumentCaptor<JsonObject> notificationJson;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private Sender sender;

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldGenerateCourtRegister() throws IOException, FileServiceException {
        final UUID courtCentreId = UUID.randomUUID();
        final CourtRegisterDocumentRequest courtRegisterDocumentRequest = CourtRegisterDocumentRequest.courtRegisterDocumentRequest()
                .withCourtCentreId(courtCentreId)
                .withDefendantType("applicant")
                .withFileName("some file name").build();
        final CourtRegisterGenerated courtRegisterGenerated = CourtRegisterGenerated.courtRegisterGenerated()
                .withCourtRegisterDocumentRequests(Lists.newArrayList(courtRegisterDocumentRequest)).build();
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(courtRegisterGenerated);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-register-generated"),
                jsonObject);

        final UUID systemUserId = UUID.randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        final JsonObject fileStorePayload = Json.createObjectBuilder().add("templatePayload", "some values").build();
        when(courtRegisterPdfPayloadGenerator.mapPayload(any(JsonObject.class))).thenReturn(fileStorePayload);

        final UUID fileId = UUID.randomUUID();
        when(fileStorer.store(any(JsonObject.class), any(ByteArrayInputStream.class))).thenReturn(fileId);
        courtRegisterEventProcessor.generateCourtRegister(requestMessage);
        verify(fileStorer).store(filestorerMetadata.capture(), any(ByteArrayInputStream.class));
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());
        assertThat(captor.getValue().metadata().name(), is("systemdocgenerator.generate-document"));
        assertThat(objectToJsonObjectConverter.convert(captor.getValue().payload()).getString("originatingSource"), is("CourtRegister"));
        assertThat(objectToJsonObjectConverter.convert(captor.getValue().payload()).getString("templateIdentifier"), is(COURT_REGISTER_TEMPLATE));
        assertThat(objectToJsonObjectConverter.convert(captor.getValue().payload()).getString("conversionFormat"), is("pdf"));
        assertThat(objectToJsonObjectConverter.convert(captor.getValue().payload()).getString("sourceCorrelationId"), is(courtCentreId.toString()));
        assertThat(objectToJsonObjectConverter.convert(captor.getValue().payload()).getString("payloadFileServiceId"), is(fileId.toString()));
    }

    @Test
    public void shouldNotifyCourt() {
        final JsonArrayBuilder recipientJsonArray = Json.createArrayBuilder();
        final String templateId = UUID.randomUUID().toString();
        final String emailAddress1 = "abc@test.com";
        recipientJsonArray.add(Json.createObjectBuilder().add("templateId", templateId)
                .add("recipientName", "yots court center")
                .add("emailTemplateName", "some template")
                .add("emailAddress1", emailAddress1).build());
        final JsonObject notificationObject = Json.createObjectBuilder().add("recipients", recipientJsonArray).add("systemDocGeneratorId", "some uuid").build();
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.court-register-notified").withUserId(UUID.randomUUID().toString()),
                notificationObject);
        when(applicationParameters.getEmailTemplateId(anyString())).thenReturn(templateId);
        courtRegisterEventProcessor.notifyCourt(requestEnvelope);
        verify(notificationNotifyService).sendEmailNotification(eq(requestEnvelope), notificationJson.capture());
        assertThat(notificationJson.getValue().getString("notificationId"), is(notNullValue()));
        assertThat(notificationJson.getValue().getString("templateId"), is(templateId));
        assertThat(notificationJson.getValue().getString("sendToAddress"), is(emailAddress1));
        assertThat(notificationJson.getValue().getString("fileId"), is("some uuid"));

    }
}