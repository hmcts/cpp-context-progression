package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
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
public class InformantRegisterEventProcessorTest {
    private static final String TEMPLATE_ID = UUID.randomUUID().toString();

    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
    @Mock
    private MaterialService materialService;
    @Mock
    private FileStorer fileStorer;
    @Mock
    private NotificationNotifyService notificationNotifyService;
    @Mock
    private ApplicationParameters applicationParameters;
    @Captor
    private ArgumentCaptor<JsonObject> notificationJson;
    @InjectMocks
    private InformantRegisterEventProcessor informantRegisterEventProcessor;
    @Captor
    private ArgumentCaptor<JsonObject> filestorerMetadata;
    @Captor
    private ArgumentCaptor<ByteArrayInputStream> byteInputStreamArgumentCaptor;
    @Mock
    private Sender sender;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldGenerateInformantRegister() throws Exception {
        final MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("progression.event.informant-register-generated")
                .withUserId(randomUUID().toString());
        final JsonArray informantRegisters = getJsonPayload("informant-register-document-requests.json").getJsonArray("informantRegisterDocumentRequests");

        final JsonObject payload = Json.createObjectBuilder()
                .add("informantRegisterDocumentRequests", informantRegisters)
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataBuilder, payload);
        when(applicationParameters.getEmailTemplateId(anyString())).thenReturn(TEMPLATE_ID);

        informantRegisterEventProcessor.generateInformantRegister(event);
        verify(fileStorer).store(filestorerMetadata.capture(), byteInputStreamArgumentCaptor.capture());
        final String expectedCsvContent = getFileContent("informantRegister.csv");
        assertThat(IOUtils.toString(byteInputStreamArgumentCaptor.getValue(), Charset.defaultCharset()), is(expectedCsvContent));
        final JsonObject fileStoreMetadataValue = filestorerMetadata.getValue();
        final UUID fileId = UUID.randomUUID();
        when(fileStorer.store(eq(fileStoreMetadataValue), any(ByteArrayInputStream.class))).thenReturn(fileId);
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().payload().toString(), is(containsString("31af405e-7b60-4dd8-a244-c24c2d3fa595")));
    }

    @Test
    public void shouldGenerateInformantRegisterForNoRecipients() throws Exception {
        final MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("progression.event.informant-register-generated")
                .withUserId(randomUUID().toString());
        final JsonArray informantRegisters = getJsonPayload("informant-register-document-requests-without-recipients.json").getJsonArray("informantRegisterDocumentRequests");

        final JsonObject payload = Json.createObjectBuilder()
                .add("informantRegisterDocumentRequests", informantRegisters)
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataBuilder, payload);

        informantRegisterEventProcessor.generateInformantRegister(event);
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().payload().toString(), is(containsString("31af405e-7b60-4dd8-a244-c24c2d3fa595")));
    }

    @Test
    public void notifyProsecutionAuthority() {
        final JsonArrayBuilder recipientJsonArray = Json.createArrayBuilder();
        final String emailAddress1 = "abc@test.com";
        recipientJsonArray.add(Json.createObjectBuilder()
                .add("recipientName", "Prosecutor_name")
                .add("emailTemplateName", "some template")
                .add("emailAddress1", emailAddress1).build());
        final UUID fileId = randomUUID();
        final JsonObject notificationObject = Json.createObjectBuilder()
                .add("recipients", recipientJsonArray)
                .add("fileId", fileId.toString())
                .add("templateId", TEMPLATE_ID)
                .build();
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.informant-register-notified").withUserId(UUID.randomUUID().toString()),
                notificationObject);
        when(applicationParameters.getEmailTemplateId(anyString())).thenReturn(TEMPLATE_ID);
        informantRegisterEventProcessor.notifyProsecutionAuthority(requestEnvelope);
        verify(notificationNotifyService).sendEmailNotification(eq(requestEnvelope), notificationJson.capture());
        assertThat(notificationJson.getValue().getString("notificationId"), is(notNullValue()));
        assertThat(notificationJson.getValue().getString("templateId"), is(TEMPLATE_ID));
        assertThat(notificationJson.getValue().getString("sendToAddress"), is(emailAddress1));
        assertThat(notificationJson.getValue().getString("fileId"), is(fileId.toString()));

    }

    private static JsonObject getJsonPayload(final String fileName) {
        return new StringToJsonObjectConverter().convert(getFileContent(fileName));
    }

    private static String getFileContent(final String fileName) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(fileName),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return response;
    }

}
