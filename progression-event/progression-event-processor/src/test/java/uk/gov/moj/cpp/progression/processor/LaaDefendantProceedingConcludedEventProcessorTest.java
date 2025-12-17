package uk.gov.moj.cpp.progression.processor;


import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.progression.courts.api.ProsecutionConcludedForLAA;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.exception.LaaAzureApimInvocationException;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.AzureFunctionService;
import uk.gov.moj.cpp.progression.transformer.DefendantProceedingConcludedTransformer;
import uk.gov.moj.cpp.progression.utils.FileUtil;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LaaDefendantProceedingConcludedEventProcessorTest {

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private AzureFunctionService azureFunctionService;

    @InjectMocks
    private LaaDefendantProceedingConcludedEventProcessor laaDefendantProceedingConcludedEventProcessor;

    @Mock
    private DefendantProceedingConcludedTransformer defendantProceedingConcludedTransformer;

    @Mock
    private ApplicationParameters applicationParameters;

    @BeforeEach
    public void setUp() {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
        ReflectionUtil.setField(laaDefendantProceedingConcludedEventProcessor, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
        ReflectionUtil.setField(laaDefendantProceedingConcludedEventProcessor, "objectToJsonObjectConverter", objectToJsonObjectConverter);
        when(defendantProceedingConcludedTransformer.getProsecutionConcludedRequest(anyList(), any(UUID.class), any(UUID.class))).thenReturn(new ProsecutionConcludedForLAA(emptyList()));
        when(applicationParameters.getRetryTimes()).thenReturn("3");
        when(applicationParameters.getRetryInterval()).thenReturn("1000");
    }

    @Test
    public void shouldHandleDefendantProceedingConcludedEventMessage() throws InterruptedException {
        final JsonObject proceedingConcludedPayload = new StringToJsonObjectConverter().convert(getProceedingConcludedPayload());
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.defendant-proceeding-concluded-changed"),
                proceedingConcludedPayload);

        when(azureFunctionService.concludeDefendantProceeding(anyString())).thenReturn(HttpStatus.SC_ACCEPTED);

        laaDefendantProceedingConcludedEventProcessor.processEvent(event);

        verify(azureFunctionService).concludeDefendantProceeding(anyString());
    }

    @Test
    public void shouldHandleDefendantProceedingConcludedEventAndThrowDefendantProceedingConcludedExceptionAfterAllRetries() {
        final JsonObject proceedingConcludedPayload = new StringToJsonObjectConverter().convert(getProceedingConcludedPayload());
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.defendant-proceeding-concluded-changed"),
                proceedingConcludedPayload);

        when(azureFunctionService.concludeDefendantProceeding(anyString())).thenReturn(HttpStatus.SC_GATEWAY_TIMEOUT);

        assertThrows(LaaAzureApimInvocationException.class, () -> laaDefendantProceedingConcludedEventProcessor.processEvent(event));
    }

    private String getProceedingConcludedPayload() {
        return FileUtil.getPayload("progression.event.defendant-proceeding-concluded-change.json");
    }
}
