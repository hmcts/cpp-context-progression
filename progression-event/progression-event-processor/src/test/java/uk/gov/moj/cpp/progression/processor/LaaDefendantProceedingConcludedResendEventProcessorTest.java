package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.utils.PayloadUtil.convertFromFile;

import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedResent;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.service.AzureFunctionService;
import uk.gov.moj.cpp.progression.transformer.DefendantProceedingConcludedTransformer;
import uk.gov.moj.cpp.progression.utils.FileUtil;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LaaDefendantProceedingConcludedResendEventProcessorTest {

    @InjectMocks
    private LaaDefendantProceedingConcludedResendEventProcessor laaDefendantProceedingConcludedResendEventProcessor;

    @Spy
    private DefendantProceedingConcludedTransformer proceedingConcludedConverter = new DefendantProceedingConcludedTransformer();

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private AzureFunctionService azureFunctionService;

    @Test
    public void shouldResendProceedingConcludedUpdateToLaa() throws IOException {

        final UUID caseId2 = randomUUID();
        when(azureFunctionService.concludeDefendantProceeding(any())).thenReturn(200);
        laaDefendantProceedingConcludedResendEventProcessor.resendProceedingConcludedToLaa(getJsonEnvelope());
        final String expectedPayload = FileUtil.getPayload("expected-laa-prosecution-concluded.json");
        verify(azureFunctionService).concludeDefendantProceeding(expectedPayload);
    }

    private JsonEnvelope getJsonEnvelope() throws IOException {

        final Metadata eventMetadata = metadataBuilder().withName("progression.event.laa-defendant-proceeding-concluded-changed")
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID())
                .build();

        final LaaDefendantProceedingConcludedResent laaDefendantProceedingConcludedResent = convertFromFile("progression.event.laa-defendant-proceeding-concluded-resent.json", LaaDefendantProceedingConcludedResent.class);

        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

        return JsonEnvelope.envelopeFrom(eventMetadata, objectToJsonObjectConverter.convert(laaDefendantProceedingConcludedResent));

    }
}