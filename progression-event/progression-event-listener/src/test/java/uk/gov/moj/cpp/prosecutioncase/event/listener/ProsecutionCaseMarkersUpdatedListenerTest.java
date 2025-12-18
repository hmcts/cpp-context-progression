package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseMarkersUpdatedListenerTest {
    @Mock
    private ProsecutionCaseRepository repository;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor;

    @InjectMocks
    private ProsecutionCaseMarkersUpdatedListener eventListener;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @BeforeEach
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
        setField(this.jsonObjectToObjectConverter, "objectMapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldPersistCpsOrganisation() {
        final List<Marker> caseMarkers = null;
        final UUID prosecutionId = randomUUID();

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        final JsonObject caseMarker = JsonObjects.createObjectBuilder()
                .add("hearingId", randomUUID().toString()).build();
        final JsonObject prosecutionCase = JsonObjects.createObjectBuilder()
                .add("cpsOrganisation", "A01")
                .add("trialReceiptType", "Transfer")
                .build();
        prosecutionCaseEntity.setPayload(prosecutionCase.toString());
        when(repository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        JsonEnvelope envelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("referral").build(), JsonObjects.createObjectBuilder().add("payload", caseMarker).build());
        eventListener.processCaseMarkersUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());
        ProsecutionCaseEntity prosecutionCaseEntitySaved = argumentCaptor.getValue();
        assertTrue(prosecutionCaseEntitySaved.getPayload().contains("\"cpsOrganisation\":\"A01\""));
        assertTrue(prosecutionCaseEntitySaved.getPayload().contains("\"trialReceiptType\":\"Transfer\""));
    }
}
