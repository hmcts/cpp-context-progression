package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterCaseOrApplication;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterHearingVenue;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterRecipient;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.DocumentGenerationRequest;
import uk.gov.moj.cpp.progression.service.FileService;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.progression.service.SystemDocGeneratorService;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Spy
    private UtcClock utcClock;

    @Captor
    private ArgumentCaptor<JsonObject> notificationJsonObjectCaptor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldGeneratePrisonCourtRegister() {
        final UUID courtCentreId = UUID.randomUUID();
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
                        .withDateOfBirth("dateOfBirth")
                        .withProsecutionCasesOrApplications(
                                singletonList(new PrisonCourtRegisterCaseOrApplication.Builder().withCaseOrApplicationReference("URN-999999").build())
                        ).build())
                .build();

        final PrisonCourtRegisterRecorded prisonCourtRegisterRecorded = PrisonCourtRegisterRecorded.prisonCourtRegisterRecorded()
                .withCourtCentreId(UUID.randomUUID())
                .withPrisonCourtRegister(prisonCourtRegisterDocumentRequest).build();

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(prisonCourtRegisterRecorded);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.prison-court-register-recorded"),
                jsonObject);

        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor = new ArgumentCaptor<>();

        when(fileService.storePayload(any(JsonObject.class), anyString(), anyString())).thenReturn((UUID.randomUUID()));

        doNothing().when(systemDocGeneratorService).generateDocument(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));

        when(prisonCourtRegisterPdfPayloadGenerator.mapPayload(any(JsonObject.class))).thenReturn(Json.createObjectBuilder().build());

        prisonCourtRegisterEventProcessor.generatePrisonCourtRegister(requestMessage);

        verify(systemDocGeneratorService).generateDocument(documentGenerationRequestArgumentCaptor.capture(), any(JsonEnvelope.class));

        verify(sender).send(envelopeArgumentCaptor.capture());

        DocumentGenerationRequest documentGenerationRequest = documentGenerationRequestArgumentCaptor.getValue();

        assertThat(documentGenerationRequest.getOriginatingSource(), is("PRISON_COURT_REGISTER"));

        assertEquals("progression.command.record-prison-court-register-document-sent", envelopeArgumentCaptor.getValue().metadata().name());
    }


    @Test
    public void shouldSendPrisonCourtRegister() {
        final UUID fileId = UUID.randomUUID();
        final PrisonCourtRegisterGenerated prisonCourtRegisterGenerated = PrisonCourtRegisterGenerated.prisonCourtRegisterGenerated().withCourtCentreId(UUID.randomUUID())
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
        when(applicationParameters.getEmailTemplateId(anyString())).thenReturn(UUID.randomUUID().toString());
        prisonCourtRegisterEventProcessor.sendPrisonCourtRegister(requestMessage);
        verify(notificationNotifyService).sendEmailNotification(eq(requestMessage), notificationJsonObjectCaptor.capture());
        assertThat(notificationJsonObjectCaptor.getValue().getString("fileId"), is(fileId.toString()));
        assertThat(notificationJsonObjectCaptor.getValue().getString("notificationId"), is(notNullValue()));
    }

}