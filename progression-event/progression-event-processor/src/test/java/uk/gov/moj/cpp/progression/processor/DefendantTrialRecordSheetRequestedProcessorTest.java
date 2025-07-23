package uk.gov.moj.cpp.progression.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.DefendantTrialRecordSheetRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.service.FileService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SystemDocGeneratorService;

import javax.json.JsonObject;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
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
}