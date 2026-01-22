package uk.gov.moj.cpp.progression.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterGeneratedV2;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterCaseOrApplication;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterHearingVenue;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterRecipient;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.DocumentGenerationRequest;
import uk.gov.moj.cpp.progression.service.FileService;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SystemDocGeneratorService;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayload;
import uk.gov.moj.cpp.progression.service.amp.mappers.AmpPcrMapper;
import uk.gov.moj.cpp.progression.service.amp.service.AmpClientService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASE_ID;

@ExtendWith(MockitoExtension.class)
public class PrisonCourtRegisterEventProcessorTest {

    @InjectMocks
    private PrisonCourtRegisterEventProcessor prisonCourtRegisterEventProcessor;
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private SystemDocGeneratorService systemDocGeneratorService;

    @Mock
    private FileService fileService;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private NotificationNotifyService notificationNotifyService;

    @Mock
    private Sender sender;

    @Mock
    private PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator;

    @Mock
    AmpPcrMapper ampPcrMapper;
    @Mock
    AmpClientService ampClientService;

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();
    @Mock
    private ProgressionService progressionService;
    @Spy
    private UtcClock utcClock;

    @Captor
    private ArgumentCaptor<JsonObject> notificationJsonObjectCaptor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldGeneratePrisonCourtRegister() {
        final UUID courtCentreId = randomUUID();
        final UUID prisonCourtRegisterStreamId = randomUUID();
        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = PrisonCourtRegisterDocumentRequest.prisonCourtRegisterDocumentRequest()
                .withCourtCentreId(courtCentreId)
                .withHearingId(randomUUID())
                .withHearingDate(ZonedDateTime.now())
                .withRecipients(singletonList(new PrisonCourtRegisterRecipient.Builder()
                        .withEmailAddress1("test@hmcst.net")
                        .withEmailTemplateName("emailTemplateName").build()))
                .withHearingVenue(new PrisonCourtRegisterHearingVenue.Builder().withCourtHouse("liverpool Crown Court").build())
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                        .withName("defendant-name")
                        .withDateOfBirth("dateOfBirth")
                        .withProsecutionCasesOrApplications(
                                singletonList(new PrisonCourtRegisterCaseOrApplication.Builder().withCaseOrApplicationReference("URN-999999").build())
                        ).build())
                .build();
        final UUID id = randomUUID();

        final PrisonCourtRegisterRecorded prisonCourtRegisterRecorded = PrisonCourtRegisterRecorded.prisonCourtRegisterRecorded()
                .withCourtCentreId(randomUUID())
                .withPrisonCourtRegisterStreamId(prisonCourtRegisterStreamId)
                .withId(id)
                .withPrisonCourtRegister(prisonCourtRegisterDocumentRequest).build();

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(prisonCourtRegisterRecorded);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.prison-court-register-recorded"),
                jsonObject);

        when(fileService.storePayload(any(JsonObject.class), anyString(), anyString())).thenReturn((randomUUID()));

        doNothing().when(systemDocGeneratorService).generateDocument(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));

        when(prisonCourtRegisterPdfPayloadGenerator.mapPayload(any(JsonObject.class))).thenReturn(Json.createObjectBuilder().build());
        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add(CASE_ID, randomUUID().toString()).build()
        ));
        prisonCourtRegisterEventProcessor.generatePrisonCourtRegister(requestMessage);

        verify(systemDocGeneratorService).generateDocument(documentGenerationRequestArgumentCaptor.capture(), any(JsonEnvelope.class));

        verify(sender).send(envelopeArgumentCaptor.capture());

        DocumentGenerationRequest documentGenerationRequest = documentGenerationRequestArgumentCaptor.getValue();

        assertThat(documentGenerationRequest.getOriginatingSource(), is("PRISON_COURT_REGISTER"));
        assertThat(documentGenerationRequest.getSourceCorrelationId(), is(prisonCourtRegisterStreamId.toString()));
        assertThat(documentGenerationRequest.getAdditionalInformation(), notNullValue());
        assertThat(documentGenerationRequest.getAdditionalInformation().size(), is(3));

        assertEquals("progression.command.record-prison-court-register-document-sent", envelopeArgumentCaptor.getValue().metadata().name());
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

    @Test
    public void shouldSendPrisonCourtRegisterV2() throws InterruptedException {
        final UUID fileId = randomUUID();
        final String prisonCourtRegisterId = randomUUID().toString();
        final PrisonCourtRegisterGeneratedV2 prisonCourtRegisterGenerated = PrisonCourtRegisterGeneratedV2.prisonCourtRegisterGeneratedV2()
                .withCourtCentreId(randomUUID())
                .withRecipients(singletonList(new PrisonCourtRegisterRecipient.Builder()
                        .withEmailAddress1("test@hmcst.net")
                        .withEmailTemplateName("emailTemplateName").build()))
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                        .withName("defendant-name")
                        .withDateOfBirth("dateOfBirth")
                        .withProsecutionCasesOrApplications(
                                singletonList(new PrisonCourtRegisterCaseOrApplication.Builder().withCaseOrApplicationReference("URN-999999").build())
                        ).build())
                .withFileId(fileId)
                .build();

        MetadataBuilder metadata = MetadataBuilderFactory.metadataWithDefaults();
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(prisonCourtRegisterGenerated);
        JsonObject jsonObjectWithId = Json.createObjectBuilder(jsonObject).add("id", prisonCourtRegisterId).build();
        final JsonEnvelope requestMessage = envelopeFrom(metadata, jsonObjectWithId);
        PcrEventPayload pcrEventPayload = PcrEventPayload.builder()
                .materialId(randomUUID())
                .eventId(randomUUID())
                .build();
        when(ampPcrMapper.mapPcrForAmp(any(PrisonCourtRegisterGeneratedV2.class), eq("test@hmcst.net"), any(Instant.class))).thenReturn(pcrEventPayload);
        when(applicationParameters.getAmpPcrNotificationUrl()).thenReturn("http://amp-url");
        when(ampClientService.post("http://amp-url", pcrEventPayload)).thenReturn(Response.ok().build());
        when(applicationParameters.getAmpPcrNotificationRetryTimes()).thenReturn("3");
        when(applicationParameters.getAmpPcrNotificationRetryInterval()).thenReturn("1000");

        prisonCourtRegisterEventProcessor.sendPrisonCourtRegisterV2(requestMessage);

        verify(ampClientService).post("http://amp-url", pcrEventPayload);
        verify(ampPcrMapper).mapPcrForAmp(any(PrisonCourtRegisterGeneratedV2.class), eq("test@hmcst.net"), any(Instant.class));
    }

    @Test
    public void shouldSendPrisonCourtRegisterV2WithEmptyEmailRecipients() throws InterruptedException {
        final UUID fileId = randomUUID();
        final PrisonCourtRegisterGeneratedV2 prisonCourtRegisterGenerated = PrisonCourtRegisterGeneratedV2.prisonCourtRegisterGeneratedV2()
                .withCourtCentreId(randomUUID())
                .withRecipients(singletonList(new PrisonCourtRegisterRecipient.Builder()
                        .withEmailTemplateName("emailTemplateName").build()))
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                        .withName("defendant-name")
                        .withDateOfBirth("dateOfBirth")
                        .withProsecutionCasesOrApplications(
                                singletonList(new PrisonCourtRegisterCaseOrApplication.Builder().withCaseOrApplicationReference("URN-999999").build())
                        ).build())
                .withFileId(fileId)
                .build();

        MetadataBuilder metadata = MetadataBuilderFactory.metadataWithDefaults();
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(prisonCourtRegisterGenerated);
        final JsonEnvelope requestMessage = envelopeFrom(metadata, jsonObject);
        PcrEventPayload pcrEventPayload = PcrEventPayload.builder()
                .materialId(randomUUID())
                .eventId(randomUUID())
                .build();
        when(ampPcrMapper.mapPcrForAmp(any(PrisonCourtRegisterGeneratedV2.class), eq(""), any(Instant.class))).thenReturn(pcrEventPayload);
        when(applicationParameters.getAmpPcrNotificationUrl()).thenReturn("http://amp-address");
        when(ampClientService.post("http://amp-address", pcrEventPayload)).thenReturn(Response.ok().build());
        when(applicationParameters.getAmpPcrNotificationRetryTimes()).thenReturn("3");
        when(applicationParameters.getAmpPcrNotificationRetryInterval()).thenReturn("1000");

        prisonCourtRegisterEventProcessor.sendPrisonCourtRegisterV2(requestMessage);

        verify(ampClientService).post("http://amp-address", pcrEventPayload);
        verify(ampPcrMapper).mapPcrForAmp(any(PrisonCourtRegisterGeneratedV2.class), eq(""), any(Instant.class));
    }

    @Test
    public void shouldSendPrisonCourtRegisterV2WithMissingIdUsesFileId() throws InterruptedException {
        final UUID fileId = randomUUID();
        final PrisonCourtRegisterGeneratedV2 prisonCourtRegisterGenerated = PrisonCourtRegisterGeneratedV2.prisonCourtRegisterGeneratedV2()
                .withCourtCentreId(randomUUID())
                .withRecipients(singletonList(new PrisonCourtRegisterRecipient.Builder()
                        .withEmailAddress1("test@hmcst.net")
                        .withEmailTemplateName("emailTemplateName").build()))
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                        .withName("defendant-name")
                        .withDateOfBirth("dateOfBirth")
                        .withProsecutionCasesOrApplications(
                                singletonList(new PrisonCourtRegisterCaseOrApplication.Builder().withCaseOrApplicationReference("URN-999999").build())
                        ).build())
                .withFileId(fileId)
                .build();

        MetadataBuilder metadata = MetadataBuilderFactory.metadataWithDefaults();
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(prisonCourtRegisterGenerated);
        final JsonEnvelope requestMessage = envelopeFrom(metadata, jsonObject);
        PcrEventPayload pcrEventPayload = PcrEventPayload.builder()
                .materialId(randomUUID())
                .eventId(randomUUID())
                .build();
        when(ampPcrMapper.mapPcrForAmp(any(PrisonCourtRegisterGeneratedV2.class), eq("test@hmcst.net"), any(Instant.class))).thenReturn(pcrEventPayload);
        when(applicationParameters.getAmpPcrNotificationUrl()).thenReturn("http://amp-address");
        when(ampClientService.post("http://amp-address", pcrEventPayload)).thenReturn(Response.ok().build());
        when(applicationParameters.getAmpPcrNotificationRetryTimes()).thenReturn("3");
        when(applicationParameters.getAmpPcrNotificationRetryInterval()).thenReturn("1000");

        prisonCourtRegisterEventProcessor.sendPrisonCourtRegisterV2(requestMessage);

        verify(ampClientService).post("http://amp-address", pcrEventPayload);
        verify(ampPcrMapper).mapPcrForAmp(any(PrisonCourtRegisterGeneratedV2.class), eq("test@hmcst.net"), any(Instant.class));
    }

    @Test
    public void shouldSendPrisonCourtRegisterV2WithMissingCreatedAtUsesCurrentTime() throws InterruptedException {
        final UUID fileId = randomUUID();
        final PrisonCourtRegisterGeneratedV2 prisonCourtRegisterGenerated = PrisonCourtRegisterGeneratedV2.prisonCourtRegisterGeneratedV2()
                .withCourtCentreId(randomUUID())
                .withRecipients(singletonList(new PrisonCourtRegisterRecipient.Builder()
                        .withEmailAddress1("test@hmcst.net")
                        .withEmailTemplateName("emailTemplateName").build()))
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                        .withName("defendant-name")
                        .withDateOfBirth("dateOfBirth")
                        .withProsecutionCasesOrApplications(
                                singletonList(new PrisonCourtRegisterCaseOrApplication.Builder().withCaseOrApplicationReference("URN-999999").build())
                        ).build())
                .withFileId(fileId)
                .build();

        MetadataBuilder metadata = MetadataBuilderFactory.metadataWithRandomUUID("progression.event.prison-court-register-generated-v2");
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(prisonCourtRegisterGenerated);
        final JsonEnvelope requestMessage = envelopeFrom(metadata, jsonObject);
        PcrEventPayload pcrEventPayload = PcrEventPayload.builder()
                .materialId(randomUUID())
                .eventId(randomUUID())
                .build();
        when(ampPcrMapper.mapPcrForAmp(any(PrisonCourtRegisterGeneratedV2.class), eq("test@hmcst.net"), any(Instant.class))).thenReturn(pcrEventPayload);
        when(applicationParameters.getAmpPcrNotificationUrl()).thenReturn("http://amp-address");
        when(ampClientService.post("http://amp-address", pcrEventPayload)).thenReturn(Response.ok().build());
        when(applicationParameters.getAmpPcrNotificationRetryTimes()).thenReturn("3");
        when(applicationParameters.getAmpPcrNotificationRetryInterval()).thenReturn("1000");

        prisonCourtRegisterEventProcessor.sendPrisonCourtRegisterV2(requestMessage);

        verify(ampClientService).post("http://amp-address", pcrEventPayload);
        verify(ampPcrMapper).mapPcrForAmp(any(PrisonCourtRegisterGeneratedV2.class), eq("test@hmcst.net"), any(Instant.class));
    }

    @Test
    public void shouldSendPrisonCourtRegisterWithDefendantHasNoDateOfBirth() {
        final UUID fileId = randomUUID();
        final PrisonCourtRegisterGenerated prisonCourtRegisterGenerated = PrisonCourtRegisterGenerated.prisonCourtRegisterGenerated().withCourtCentreId(randomUUID())
                .withRecipients(singletonList(new PrisonCourtRegisterRecipient.Builder()
                        .withEmailAddress1("test@hmcst.net")
                        .withEmailTemplateName("emailTemplateName").build()))
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                        .withName("defendant-name")
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

}