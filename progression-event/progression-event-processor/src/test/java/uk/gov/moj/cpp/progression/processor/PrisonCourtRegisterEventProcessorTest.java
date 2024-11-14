package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASE_ID;

import com.google.common.io.Resources;
import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterCaseOrApplication;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterHearingVenue;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterRecipient;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

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
public class PrisonCourtRegisterEventProcessorTest {

    @InjectMocks
    private PrisonCourtRegisterEventProcessor prisonCourtRegisterEventProcessor;

    @Mock
    private FileStorer fileStorer;

    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Spy
    private PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator;

    @Mock
    private SystemUserProvider systemUserProvider;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private DocumentGeneratorClient documentGeneratorClient;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @Mock
    private NotificationNotifyService notificationNotifyService;

    @Mock
    private Sender sender;
    @Mock
    private MaterialService materialService;

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Mock
    ProgressionService progressionService;
    @Spy
    private UtcClock utcClock;

    @Captor
    private ArgumentCaptor<JsonObject> notificationJsonObjectCaptor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldGeneratePrisonCourtRegister() throws IOException, FileServiceException {
        final UUID courtCentreId = randomUUID();
        final UUID caseId = randomUUID();

        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = PrisonCourtRegisterDocumentRequest.prisonCourtRegisterDocumentRequest()
                .withCourtCentreId(courtCentreId)
                .withHearingId(UUID.randomUUID())
                .withHearingDate(ZonedDateTime.now())
                .withRecipients(singletonList(new PrisonCourtRegisterRecipient.Builder()
                        .withEmailAddress1("test@hmcst.net")
                        .withEmailTemplateName("emailTemplateName").build()))
                .withHearingVenue(new PrisonCourtRegisterHearingVenue.Builder().withCourtHouse("liverpool Crown Court").build())
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                        .withName("defendant-name")
                        //.withDateOfBirth("dateOfBirth")
                        .withProsecutionCasesOrApplications(
                                singletonList(new PrisonCourtRegisterCaseOrApplication.Builder().withCaseOrApplicationReference("URN-999999").build())
                        ).build())
                .build();

        final PrisonCourtRegisterRecorded prisonCourtRegisterRecorded = PrisonCourtRegisterRecorded.prisonCourtRegisterRecorded()
                .withCourtCentreId(UUID.randomUUID())
                .withId(UUID.randomUUID())
                .withPrisonCourtRegister(prisonCourtRegisterDocumentRequest).build();
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(prisonCourtRegisterRecorded);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.prison-court-register-recorded"),
                jsonObject);

        final UUID systemUserId = randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add(CASE_ID, caseId.toString()).build()
        ));

        doNothing().when(materialService).uploadMaterial((UUID) any(), (UUID) any(), (JsonEnvelope) any());

        final byte[] byteArray = new byte[]{};
        when(documentGeneratorClient.generatePdfDocument(any(), eq("OEE_Layout5"), eq(systemUserId))).thenReturn(byteArray);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);

        final UUID fileId = randomUUID();
        when(fileStorer.store(any(JsonObject.class), any(ByteArrayInputStream.class))).thenReturn(fileId);

        prisonCourtRegisterEventProcessor.generatePrisonCourtRegister(requestMessage);

        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());
        verify(this.documentGeneratorClient).generatePdfDocument(any(), any(), eq(systemUserId));
        verify(this.fileStorer).store(any(JsonObject.class), any(ByteArrayInputStream.class));
        List<Envelope<JsonObject>> commandList = envelopeArgumentCaptor.getAllValues();

        assertThat(commandList.get(0).metadata().name(), is("progression.add-court-document"));
        assertThat(commandList.get(0).payload().containsKey("materialId"), is(true));
        assertThat(commandList.get(0).payload().containsKey("courtDocument"), is(true));
        assertThat(commandList.get(0).payload().containsKey("courtDocument"), is(true));
        assertThat(commandList.get(0).payload().getJsonObject("courtDocument").getJsonObject("documentCategory").getJsonObject("caseDocument")
                        .getString("prosecutionCaseId"),is(caseId.toString()));

        final Envelope<JsonObject> command = commandList.get(1);
        assertThat(command.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("progression.command.record-prison-court-register-generated"));
        JsonObject commandPayload = command.payload();
        assertThat(commandPayload.getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(commandPayload.getString("fileId"), is(fileId.toString()));
        assertThat(commandPayload.getString("id"), is(prisonCourtRegisterRecorded.getId().toString()));
        assertThat(commandPayload.getJsonObject("hearingVenue").getString("courtHouse"), is("liverpool Crown Court"));
        assertThat(commandPayload.getJsonObject("defendant").getString("name"), is("defendant-name"));
        assertThat(commandPayload.getJsonArray("recipients").getValuesAs(JsonObject.class).get(0).getString("emailAddress1"), is("test@hmcst.net"));
        assertThat(commandPayload.getJsonArray("recipients").getValuesAs(JsonObject.class).get(0).getString("emailTemplateName"), is("emailTemplateName"));
    }

    @Test
    public void shouldGeneratePrisonCourtRegisterWhenCaseIsEmpty() throws IOException, FileServiceException {
        final UUID systemUserId = randomUUID();
        final UUID courtCentreId = randomUUID();

        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = PrisonCourtRegisterDocumentRequest.prisonCourtRegisterDocumentRequest()
                .withCourtCentreId(courtCentreId)
                .withHearingId(UUID.randomUUID())
                .withHearingDate(ZonedDateTime.now())
                .withRecipients(singletonList(new PrisonCourtRegisterRecipient.Builder()
                        .withEmailAddress1("test@hmcst.net")
                        .withEmailTemplateName("emailTemplateName").build()))
                .withHearingVenue(new PrisonCourtRegisterHearingVenue.Builder().withCourtHouse("liverpool Crown Court").build())
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                        .withName("defendant-name")
                                .withProsecutionCasesOrApplications(new ArrayList<>()).build())
                .build();

        final PrisonCourtRegisterRecorded prisonCourtRegisterRecorded = PrisonCourtRegisterRecorded.prisonCourtRegisterRecorded()
                .withCourtCentreId(UUID.randomUUID())
                .withId(UUID.randomUUID())
                .withPrisonCourtRegister(prisonCourtRegisterDocumentRequest).build();
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(prisonCourtRegisterRecorded);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.prison-court-register-recorded"),
                jsonObject);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        doNothing().when(materialService).uploadMaterial((UUID) any(), (UUID) any(), (JsonEnvelope) any());

        final byte[] byteArray = new byte[]{};
        when(documentGeneratorClient.generatePdfDocument(any(), eq("OEE_Layout5"), eq(systemUserId))).thenReturn(byteArray);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);

        final UUID fileId = randomUUID();
        when(fileStorer.store(any(JsonObject.class), any(ByteArrayInputStream.class))).thenReturn(fileId);

        prisonCourtRegisterEventProcessor.generatePrisonCourtRegister(requestMessage);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        verify(this.documentGeneratorClient).generatePdfDocument(any(), any(), eq(systemUserId));
        verify(this.fileStorer).store(any(JsonObject.class), any(ByteArrayInputStream.class));
        final Envelope<JsonObject> command = envelopeArgumentCaptor.getValue();

        assertThat(command.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("progression.command.record-prison-court-register-generated"));
        JsonObject commandPayload = command.payload();
        assertThat(commandPayload.getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(commandPayload.getString("fileId"), is(fileId.toString()));
        assertThat(commandPayload.getString("id"), is(prisonCourtRegisterRecorded.getId().toString()));
        assertThat(commandPayload.getJsonObject("hearingVenue").getString("courtHouse"), is("liverpool Crown Court"));
        assertThat(commandPayload.getJsonObject("defendant").getString("name"), is("defendant-name"));
        assertThat(commandPayload.getJsonArray("recipients").getValuesAs(JsonObject.class).get(0).getString("emailAddress1"), is("test@hmcst.net"));
        assertThat(commandPayload.getJsonArray("recipients").getValuesAs(JsonObject.class).get(0).getString("emailTemplateName"), is("emailTemplateName"));
    }

    @Test
    public void shouldSendPrisonCourtRegister() {
        final UUID fileId = randomUUID();
        final PrisonCourtRegisterGenerated prisonCourtRegisterGenerated = PrisonCourtRegisterGenerated.prisonCourtRegisterGenerated().withCourtCentreId(randomUUID())
                .withRecipients(singletonList(new PrisonCourtRegisterRecipient.Builder()
                        .withEmailAddress1("test@hmcst.net")
                        .withEmailTemplateName("emailTemplateName").build()))
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                        .withName("defendant-name")
                        .withDateOfBirth("dateOfBirth")
                        .withProsecutionCasesOrApplications(
                                singletonList(new PrisonCourtRegisterCaseOrApplication.Builder().withCaseOrApplicationReference("URN-999999").build())
                        ).build())
                .withHearingVenue(new PrisonCourtRegisterHearingVenue.Builder().withCourtHouse("liverpool Crown Court").build())
                .withFileId(fileId)
                .build();

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(prisonCourtRegisterGenerated);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.prison-court-register-generated"),
                jsonObject);
        when(applicationParameters.getEmailTemplateId(anyString())).thenReturn(randomUUID().toString());
        prisonCourtRegisterEventProcessor.sendPrisonCourtRegister(requestMessage);
        verify(notificationNotifyService).sendEmailNotification(eq(requestMessage), notificationJsonObjectCaptor.capture());
        assertThat(notificationJsonObjectCaptor.getValue().getString("fileId"), is(fileId.toString()));
        assertThat(notificationJsonObjectCaptor.getValue().getString("notificationId"), is(notNullValue()));
    }

    public static JsonObject getPayload(final String path) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return new StringToJsonObjectConverter().convert(response);
    }

}
