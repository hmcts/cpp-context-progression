package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantRequestRepository;

import javax.json.JsonObject;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantRequestEventListenerTest {

    @InjectMocks
    private DefendantRequestEventListener defendantRequestEventListener;

    @Mock
    private DefendantRequestRepository repository;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @BeforeEach
    public void setUp(){
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldProcessProsecutionCaseCreated(){
        final JsonObject defendantRequestCreatedPayload = createObjectBuilder().add("defendantRequest",
                createObjectBuilder()
                        .add("defendantId",randomUUID().toString())
                        .add("prosecutionCaseId",randomUUID().toString())
                        .add("referralReasonId",randomUUID().toString())
                        .add("summonsType","SJP_REFERRAL"))
                .build();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.defendant-request-created"),
                defendantRequestCreatedPayload);

        defendantRequestEventListener.processProsecutionCaseCreated(requestMessage);
    }
}