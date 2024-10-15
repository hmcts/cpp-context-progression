package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtApplicationPayment;
import uk.gov.justice.core.courts.CourtFeeForCivilApplicationUpdated;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtFeeForCivilApplicationUpdatedEventProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @InjectMocks
    private CourtFeeForCivilApplicationUpdatedEventProcessor processor;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void processCourtFeeForCivilApplication() {
        final UUID applicationId = UUID.randomUUID();
        final CourtFeeForCivilApplicationUpdated courtFeeForCivilApplicationUpdated = CourtFeeForCivilApplicationUpdated.courtFeeForCivilApplicationUpdated()
                .withApplicationId(applicationId)
                .withCourtApplicationPayment(CourtApplicationPayment.courtApplicationPayment()
                        .withPaymentReference("TestRef001")
                        .build())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(courtFeeForCivilApplicationUpdated);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-fee-for-civil-application-updated"),
                payload);

        processor.processCourtFeeForCivilApplication(requestMessage);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.court-fee-for-civil-application-updated"));
        JsonObject actualPayload = publicEvent.payload();
        assertThat(actualPayload.getString("applicationId"), equalTo(applicationId.toString()));
    }
}
