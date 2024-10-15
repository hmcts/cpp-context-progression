package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.processor.FormEventProcessor.CASE_ID;
import static uk.gov.moj.cpp.progression.utils.PayloadUtil.getPayloadAsJsonObject;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelope;
import uk.gov.moj.cpp.progression.model.HearingEventLog;
import uk.gov.moj.cpp.progression.service.HearingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.hearingeventlog.HearingEventLogGenerationService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class HearingResultedEventProcessorTest {

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final String hearingEventLogResponse = "hearing.get-hearing-event-log-document.json";
    private final String noHearingEventLogResponse = "hearing.no-hearing-event-log-document.json";
    @InjectMocks
    private HearingResultedEventProcessor eventProcessor;
    @Mock
    private Sender sender;
    @Captor
    private ArgumentCaptor<DefaultJsonEnvelope> senderJsonEnvelopeCaptor;
    @Mock
    private Requester requester;
    @Mock
    private HearingService hearingService;
    @Mock
    private ProgressionService progressionService;
    @Mock
    private HearingEventLogGenerationService hearingEventLogGenerationService;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter1 = new JsonObjectToObjectConverter(objectMapper);

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldProcessRetentionCalculated() {
        //Given
        final String hearingId = randomUUID().toString();
        final JsonObject caseRetentionLengthCalculated = createObjectBuilder()
                .add("hearingId", hearingId)
                .add("hearingType", "Trial")
                .add("retentionPolicy", createObjectBuilder()
                        .add("policyType", "2")
                        .add("period", "7Y0M0D")
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.events.case-retention-length-calculated"),
                caseRetentionLengthCalculated);

        //When
        eventProcessor.processRetentionCalculated(event);

        //Then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultJsonEnvelope commandEvent = senderJsonEnvelopeCaptor.getValue();
        assertThat(commandEvent.metadata().name(), is("public.events.progression.case-retention-length-calculated"));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId)),
                withJsonPath("$.hearingType", equalTo("Trial")),
                withJsonPath("$.retentionPolicy.policyType", equalTo("2")),
                withJsonPath("$.retentionPolicy.period", equalTo("7Y0M0D"))
        )));

    }

    @Test
    public void shouldProcesCaagHearingLogDocument() throws IOException {
        //Given
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final JsonObject responsePayload = getPayloadAsJsonObject(hearingEventLogResponse);
        when(hearingService.getHearingEventLogs(any(JsonEnvelope.class), any(), any())).thenReturn(responsePayload);

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-event-logs-document-created").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .build());

        eventProcessor.processHearingLogDocument(requestEnvelope);

        //Then
        verify(hearingEventLogGenerationService).generateHearingLogEvent(any(), any(), any(), any());

        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("public.progression.hearing-event-logs-document-success"));
        assertThat(currentEvents.get(0).payload(), notNullValue());
    }

    @Test
    public void shouldProcesAaagHearingLogDocument() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        JsonObjectToObjectConverter jsonObjectToObjectConverter1 = new JsonObjectToObjectConverter(objectMapper);
        //Given
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("hearing.get-hearing-event-log-for-documents.json"), defaultCharset()));
        final HearingEventLog hearingPayload = jsonObjectToObjectConverter1.convert(hearing, HearingEventLog.class);

        final JsonObject responsePayload = getPayloadAsJsonObject("hearing.get-hearing-event-log-document.json");

        final Optional<JsonObject> caseStatusResponsePayload = Optional.ofNullable(getPayloadAsJsonObject("prosecution-case-with-inactive-status.json"));

        final JsonObject courtApplicationPayload = stringToJsonObjectConverter.convert(Resources.toString(getResource("progression.query.application.aaag.json"), defaultCharset()));


        when(progressionService.getCaseStatusForApplicationId(any(JsonEnvelope.class), any(), any())).thenReturn(caseStatusResponsePayload);
        when(hearingService.getHearingEventLogs(any(JsonEnvelope.class), any(), any())).thenReturn(responsePayload);
        when(jsonObjectToObjectConverter.convert(any(), any()))
                .thenReturn(hearingPayload);
        when(progressionService.retrieveApplication(any(JsonEnvelope.class), any())).thenReturn(courtApplicationPayload);

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-event-logs-document-created").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .add("applicationId", applicationId)
                        .build());

        eventProcessor.processHearingLogDocument(requestEnvelope);

        //Then
        verify(hearingEventLogGenerationService).generateHearingLogEvent(any(), any(), any(), any());

        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("public.progression.hearing-event-logs-document-success"));
        assertThat(currentEvents.get(0).payload(), notNullValue());
    }

    @Test
    public void shouldProcesAaagHearingLogDocumentForNoCaseStatus() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        JsonObjectToObjectConverter jsonObjectToObjectConverter1 = new JsonObjectToObjectConverter(objectMapper);
        //Given
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("hearing.get-hearing-event-log-for-documents.json"), defaultCharset()));
        final HearingEventLog hearingPayload = jsonObjectToObjectConverter1.convert(hearing, HearingEventLog.class);

        final JsonObject responsePayload = getPayloadAsJsonObject("hearing.get-hearing-event-log-document.json");

        final Optional<JsonObject> caseStatusResponsePayload = Optional.ofNullable(getPayloadAsJsonObject("prosecution-case-with-no-status.json"));
        final JsonObject courtApplicationPayload = stringToJsonObjectConverter.convert(Resources.toString(getResource("progression.query.application.aaag.json"), defaultCharset()));

        when(progressionService.getCaseStatusForApplicationId(any(JsonEnvelope.class), any(), any())).thenReturn(caseStatusResponsePayload);
        when(hearingService.getHearingEventLogs(any(JsonEnvelope.class), any(), any())).thenReturn(responsePayload);
        when(jsonObjectToObjectConverter.convert(any(), any()))
                .thenReturn(hearingPayload);
        when(progressionService.retrieveApplication(any(JsonEnvelope.class), any())).thenReturn(courtApplicationPayload);

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-event-logs-document-created").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .add("applicationId", applicationId)
                        .build());

        eventProcessor.processHearingLogDocument(requestEnvelope);

        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("public.progression.hearing-event-logs-document-success"));
        assertThat(currentEvents.get(0).payload(), notNullValue());
    }

    @Test
    public void shouldProcesAaagHearingLogDocumentWhenNoRespondent() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        JsonObjectToObjectConverter jsonObjectToObjectConverter1 = new JsonObjectToObjectConverter(objectMapper);
        //Given
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("hearing.get-hearing-event-log-for-documents.json"), defaultCharset()));
        final HearingEventLog hearingPayload = jsonObjectToObjectConverter1.convert(hearing, HearingEventLog.class);

        final JsonObject responsePayload = getPayloadAsJsonObject("hearing.get-hearing-event-log-document.json");

        final Optional<JsonObject> caseStatusResponsePayload = Optional.ofNullable(getPayloadAsJsonObject("prosecution-case-with-inactive-status.json"));
        final JsonObject courtApplicationPayload = stringToJsonObjectConverter.convert(Resources.toString(getResource("progression.query.application.aaag-no-respondent.json"), defaultCharset()));


        when(progressionService.getCaseStatusForApplicationId(any(JsonEnvelope.class), any(), any())).thenReturn(caseStatusResponsePayload);
        when(hearingService.getHearingEventLogs(any(JsonEnvelope.class), any(), any())).thenReturn(responsePayload);
        when(jsonObjectToObjectConverter.convert(any(), any()))
                .thenReturn(hearingPayload);
        when(progressionService.retrieveApplication(any(JsonEnvelope.class), any())).thenReturn(courtApplicationPayload);

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-event-logs-document-created").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .add("applicationId", applicationId)
                        .build());

        eventProcessor.processHearingLogDocument(requestEnvelope);

        //Then
        verify(hearingEventLogGenerationService).generateHearingLogEvent(any(), any(), any(), any());

        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("public.progression.hearing-event-logs-document-success"));
        assertThat(currentEvents.get(0).payload(), notNullValue());
    }

    @Test
    public void shouldProcesAaagHearingLogDocumentWhenNoApplicant() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        JsonObjectToObjectConverter jsonObjectToObjectConverter1 = new JsonObjectToObjectConverter(objectMapper);
        //Given
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("hearing.get-hearing-event-log-for-documents.json"), defaultCharset()));
        final HearingEventLog hearingPayload = jsonObjectToObjectConverter1.convert(hearing, HearingEventLog.class);

        final JsonObject responsePayload = getPayloadAsJsonObject("hearing.get-hearing-event-log-document.json");

        final Optional<JsonObject> caseStatusResponsePayload = Optional.ofNullable(getPayloadAsJsonObject("prosecution-case-with-inactive-status.json"));
        final JsonObject courtApplicationPayload = stringToJsonObjectConverter.convert(Resources.toString(getResource("progression.query.application.aaag-no-applicant.json"), defaultCharset()));


        when(progressionService.getCaseStatusForApplicationId(any(JsonEnvelope.class), any(), any())).thenReturn(caseStatusResponsePayload);
        when(hearingService.getHearingEventLogs(any(JsonEnvelope.class), any(), any())).thenReturn(responsePayload);
        when(jsonObjectToObjectConverter.convert(any(), any()))
                .thenReturn(hearingPayload);
        when(progressionService.retrieveApplication(any(JsonEnvelope.class), any())).thenReturn(courtApplicationPayload);

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-event-logs-document-created").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .add("applicationId", applicationId)
                        .build());

        eventProcessor.processHearingLogDocument(requestEnvelope);

        //Then
        verify(hearingEventLogGenerationService).generateHearingLogEvent(any(), any(), any(), any());

        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("public.progression.hearing-event-logs-document-success"));
        assertThat(currentEvents.get(0).payload(), notNullValue());
    }

    @Test
    public void shouldProcesAaagHearingLogDocumentWhenNoApplicantAndRespondent() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        JsonObjectToObjectConverter jsonObjectToObjectConverter1 = new JsonObjectToObjectConverter(objectMapper);
        //Given
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("hearing.get-hearing-event-log-for-documents.json"), defaultCharset()));
        final HearingEventLog hearingPayload = jsonObjectToObjectConverter1.convert(hearing, HearingEventLog.class);

        final JsonObject responsePayload = getPayloadAsJsonObject("hearing.get-hearing-event-log-document.json");

        final Optional<JsonObject> caseStatusResponsePayload = Optional.ofNullable(getPayloadAsJsonObject("prosecution-case-with-active-status.json"));
        ProsecutionCase prosecutionCase = jsonObjectToObjectConverter1.convert(caseStatusResponsePayload.get(),ProsecutionCase.class);
        final JsonObject courtApplicationPayload = stringToJsonObjectConverter.convert(Resources.toString(getResource("progression.query.application.aaag-no-applicant-respondent.json"), defaultCharset()));


        when(progressionService.getCaseStatusForApplicationId(any(JsonEnvelope.class), any(), any())).thenReturn(caseStatusResponsePayload);
        when(hearingService.getHearingEventLogs(any(JsonEnvelope.class), any(), any())).thenReturn(responsePayload);
        when(jsonObjectToObjectConverter.convert(any(), eq(HearingEventLog.class)))
                .thenReturn(hearingPayload);
        when(jsonObjectToObjectConverter.convert(any(), eq(ProsecutionCase.class)))
                .thenReturn(prosecutionCase);

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-event-logs-document-created").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .add("applicationId", applicationId)
                        .build());

        eventProcessor.processHearingLogDocument(requestEnvelope);

        //Then
        verify(hearingEventLogGenerationService).generateHearingLogEvent(any(), any(), any(), any());

        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("public.progression.hearing-event-logs-document-success"));
        assertThat(currentEvents.get(0).payload(), notNullValue());
    }


   @Test
    public void shouldProcesCaagHearingLogDocumentForActiveCase() throws IOException {
        //Given
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final JsonObject responsePayload = getPayloadAsJsonObject(hearingEventLogResponse);
        final Optional<JsonObject> caseStatusResponsePayload = Optional.ofNullable(getPayloadAsJsonObject("prosecution-case-with-active-status.json"));

        when(progressionService.getCaseStatusForApplicationId(any(JsonEnvelope.class), any(), any())).thenReturn(caseStatusResponsePayload);
        when(hearingService.getHearingEventLogs(any(JsonEnvelope.class), any(), any())).thenReturn(responsePayload);
        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("hearing.get-hearing-event-log-for-documents.json"), defaultCharset()));
        final HearingEventLog hearingPayload = jsonObjectToObjectConverter1.convert(hearing, HearingEventLog.class);

        when(jsonObjectToObjectConverter.convert(any(), eq(HearingEventLog.class)))
                .thenReturn(hearingPayload);
       ProsecutionCase prosecutionCase = jsonObjectToObjectConverter1.convert(caseStatusResponsePayload.get(),ProsecutionCase.class);
       when(jsonObjectToObjectConverter.convert(any(), eq(ProsecutionCase.class)))
               .thenReturn(prosecutionCase);
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-event-logs-document-created").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .add("applicationId", applicationId)
                        .build());

        eventProcessor.processHearingLogDocument(requestEnvelope);

        //Then
        verify(hearingEventLogGenerationService).generateHearingLogEvent(any(), any(), any(), any());

        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("public.progression.hearing-event-logs-document-success"));
        assertThat(currentEvents.get(0).payload(), notNullValue());
    }

    @Test
    public void shouldNotProcessHearingLogDocumentWhenNoHearingLogs() throws IOException {
        //Given
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String applicationId = randomUUID().toString();

        final JsonObject responsePayload = getPayloadAsJsonObject(noHearingEventLogResponse);

        when(hearingService.getHearingEventLogs(any(JsonEnvelope.class), any(), any())).thenReturn(responsePayload);

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-event-logs-document-created").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .add(CASE_ID, randomUUID().toString())
                        .build());

        eventProcessor.processHearingLogDocument(requestEnvelope);

        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("public.progression.hearing-event-logs-document-failed"));
        assertThat(currentEvents.get(0).payload(), notNullValue());
    }

    @Test
    public void shouldNotProcessHearingLogDocumentWhenNoRecordForApplicationForCase() throws IOException {
        //Given
        final String userId = randomUUID().toString();
        final String applicationId = randomUUID().toString();

        final JsonObject responsePayload = getPayloadAsJsonObject(noHearingEventLogResponse);

        final Optional<JsonObject> caseStatusResponsePayload = Optional.ofNullable(createObjectBuilder().build());
        final JsonObject courtApplicationPayload = stringToJsonObjectConverter.convert(Resources.toString(getResource("progression.query.application.aaag-no-applicant.json"), defaultCharset()));


        when(progressionService.getCaseStatusForApplicationId(any(JsonEnvelope.class), any(), any())).thenReturn(caseStatusResponsePayload);

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-event-logs-document-created").withUserId(userId),
                createObjectBuilder()
                        .add("applicationId", applicationId)
                        .add(CASE_ID, randomUUID().toString())
                        .build());

        eventProcessor.processHearingLogDocument(requestEnvelope);

        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("public.progression.hearing-event-logs-document-failed"));
        assertThat(currentEvents.get(0).payload(), notNullValue());
    }

}