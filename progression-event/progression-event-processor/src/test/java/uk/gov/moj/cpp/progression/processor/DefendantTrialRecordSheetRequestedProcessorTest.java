package uk.gov.moj.cpp.progression.processor;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.DefendantTrialRecordSheetRequested;
import uk.gov.justice.core.courts.DefendantTrialRecordSheetRequestedForApplication;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.service.ConversionFormat;
import uk.gov.moj.cpp.progression.service.DocumentGenerationRequest;
import uk.gov.moj.cpp.progression.service.FileService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SystemDocGeneratorService;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

@ExtendWith(MockitoExtension.class)
public class DefendantTrialRecordSheetRequestedProcessorTest {

    @InjectMocks
    private DefendantTrialRecordSheetRequestedProcessor  eventProcessor;

    @Mock
    private Sender sender;

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    private final StringToJsonObjectConverter stringToJsonObjectConverter =  new StringToJsonObjectConverter();

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;
    @Spy
    private UtcClock utcClock;

    @Mock
    private FileService fileService;
    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();
    @Mock
    ProgressionService progressionService;
    @Mock
    SystemDocGeneratorService systemDocGeneratorService;
    @Test
    void process() {
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID streamId = randomUUID();

        JsonObject nameJsonObject = createObjectBuilder()
                .add("name", "test name")
                .build();
        JsonObject recordSheetPayload = createObjectBuilder()
                .add("defendant", nameJsonObject)
                .build();
        JsonObject newRecordSheetPayload = createObjectBuilder()
                .add("payload", recordSheetPayload)
                .build();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.defendant-trial-record-sheet-requested").withStreamId(streamId),
                objectToJsonObjectConverter.convert(DefendantTrialRecordSheetRequested.defendantTrialRecordSheetRequested()
                        .withDefendantId(defendantId)
                        .withCaseId(caseId)
                        .build()));

        when(progressionService.generateTrialRecordSheetPayload(event, caseId, defendantId)).thenReturn(newRecordSheetPayload);
        when(fileService.storePayload(any(JsonObject.class), anyString(), anyString())).thenReturn((randomUUID()));
        doNothing().when(systemDocGeneratorService).generateDocument(any(), any());

        this.eventProcessor.process(event);
        verify(sender, times(0)).send(envelopeArgumentCaptor.capture());
    }

    @Test
    void processWhenPayloadIsNull() {
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID streamId = randomUUID();

        JsonObject nameJsonObject = createObjectBuilder()
                .add("name", "test name")
                .build();
        JsonObject recordSheetPayload = createObjectBuilder()
                .add("defendant", nameJsonObject)
                .build();
        JsonObject newRecordSheetPayload = createObjectBuilder()
                .build();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.defendant-trial-record-sheet-requested").withStreamId(streamId),
                objectToJsonObjectConverter.convert(DefendantTrialRecordSheetRequested.defendantTrialRecordSheetRequested()
                        .withDefendantId(defendantId)
                        .withCaseId(caseId)
                        .build()));

        when(progressionService.generateTrialRecordSheetPayload(event, caseId, defendantId)).thenReturn(newRecordSheetPayload);

        this.eventProcessor.process(event);
        verify(sender, never()).send(envelopeArgumentCaptor.capture());
        verify(fileService, never()).storePayload(any(JsonObject.class), anyString(), anyString());
        verify(systemDocGeneratorService, never()).generateDocument(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));
    }

    @Test
    void shouldProcessForApplication() {
        ArgumentCaptor<DocumentGenerationRequest> captor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        final UUID applicationId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID fileId1 = randomUUID();
        final UUID fileId2 = randomUUID();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.defendant-trial-record-sheet-requested-for-application").withStreamId(applicationId),
                objectToJsonObjectConverter.convert(DefendantTrialRecordSheetRequestedForApplication.defendantTrialRecordSheetRequestedForApplication()
                        .withCaseId(caseId)
                        .withOffenceIds(Arrays.asList(offenceId1, offenceId2))
                        .withCourtApplication(CourtApplication.courtApplication().build())
                        .build()));
        final JsonObject payload1 = createObjectBuilder().add("caseReference", randomUUID().toString()).build();
        final JsonObject payload2 = createObjectBuilder().add("caseReference", randomUUID().toString()).build();
        final String defendantName1 = "name1";
        final String defendantName2 = "name2";
        when(progressionService.generateTrialRecordSheetPayloadForApplication(event, caseId, Arrays.asList(offenceId1, offenceId2))).thenReturn(Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("payload", payload1)
                        .add("defendantName", defendantName1)
                        .build())
                .add(Json.createObjectBuilder()
                        .add("payload", payload2)
                        .add("defendantName", defendantName2)
                        .build())
                .build());

        when(fileService.storePayload(eq(payload1), anyString(), eq("RecordSheet"))).thenReturn((fileId1));
        when(fileService.storePayload(eq(payload2), anyString(), eq("RecordSheet"))).thenReturn((fileId2));

        eventProcessor.processForApplication(event);
        verify(systemDocGeneratorService, times(2)).generateDocument(captor.capture(), eq(event));

        final Map<String, String> additionalInformation1 = ImmutableMap.of("caseId", caseId.toString(), "defendantName", defendantName1);
        final DocumentGenerationRequest firstRequest = captor.getAllValues().get(0);
        assertThat(firstRequest.getPayloadFileServiceId(), is((fileId1)));
        assertThat(firstRequest.getAdditionalInformation(), is(additionalInformation1));
        assertThat(firstRequest.getConversionFormat(), is((ConversionFormat.PDF)));
        assertThat(firstRequest.getSourceCorrelationId(), is((applicationId.toString())));
        assertThat(firstRequest.getOriginatingSource(), is(("RECORD_SHEET")));
        assertThat(firstRequest.getTemplateIdentifier(), is(("RecordSheet")));

        final Map<String, String> additionalInformation2 = ImmutableMap.of("caseId", caseId.toString(), "defendantName", defendantName2);
        final DocumentGenerationRequest secondRequest = captor.getAllValues().get(1);
        assertThat(secondRequest.getPayloadFileServiceId(), is((fileId2)));
        assertThat(secondRequest.getAdditionalInformation(), is(additionalInformation2));
        assertThat(secondRequest.getConversionFormat(), is((ConversionFormat.PDF)));
        assertThat(secondRequest.getSourceCorrelationId(), is((applicationId.toString())));
        assertThat(secondRequest.getOriginatingSource(), is(("RECORD_SHEET")));
        assertThat(secondRequest.getTemplateIdentifier(), is(("RecordSheet")));
    }

    @Test
    void shouldProcessForApplicationWhenCourtOrderOffencesResultIsUpdated() throws IOException {
        ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);

        final UUID applicationId = randomUUID();
        final UUID caseId = UUID.fromString("09816ffe-38e5-4fe1-af4b-4bee159eb034");
        final UUID offenceId1 = UUID.fromString("0ab230be-f414-46e9-8744-639b4b49a5c6");

        final UUID fileId1 = randomUUID();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.defendant-trial-record-sheet-requested-for-application").withStreamId(applicationId),
                new StringToJsonObjectConverter().convert(Resources.toString(getResource("progression.event.defendant-trial-record-sheet-requested-for-application.json"), defaultCharset())));

        final JsonArray queryResponse =  stringToJsonObjectConverter.convert(Resources.toString(getResource("progression.query.record-sheet-for-application.json"), defaultCharset())).getJsonArray("payloads");
        when(progressionService.generateTrialRecordSheetPayloadForApplication(event, caseId, List.of(offenceId1)))
                .thenReturn(queryResponse);

        when(fileService.storePayload(any(), anyString(), eq("RecordSheet"))).thenReturn((fileId1));

        eventProcessor.processForApplication(event);

        verify(fileService).storePayload(captor.capture(), anyString(), eq("RecordSheet"));
        final CourtExtractRequested result =  jsonToObjectConverter.convert(captor.getValue(), CourtExtractRequested.class);
        final CourtExtractRequested expected = jsonToObjectConverter.convert(stringToJsonObjectConverter.convert(Resources.toString(getResource("progression.query.expected.record-sheet-for-application.json"), defaultCharset())), CourtExtractRequested.class);
        assertThat(result, is(expected));

    }
}