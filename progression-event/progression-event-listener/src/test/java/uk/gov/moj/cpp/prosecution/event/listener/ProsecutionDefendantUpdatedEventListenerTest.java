package uk.gov.moj.cpp.prosecution.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.event.listener.ProsecutionCaseDefendantUpdatedEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

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
public class ProsecutionDefendantUpdatedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ProsecutionCaseRepository repository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated;

    @Mock
    private Defendant defendant;

    @Mock
    private ProsecutionCaseEntity prosecutionCaseEntity;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private ProsecutionCase prosecutionCase;

    @InjectMocks
    private ProsecutionCaseDefendantUpdatedEventListener eventListener;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private ObjectMapper objectMapper;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    @Before
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
    }


    @Test
    public void shouldHandleProsecutionCaseDefendantUpdatedEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class))
                .thenReturn(prosecutionCaseDefendantUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(defendant.getId()).thenReturn(UUID.randomUUID());
        when(prosecutionCaseDefendantUpdated.getDefendant()).thenReturn(defendant);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendant.getId().toString()).build())
                                .build())
                        .build()).build();

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class))
                .thenReturn(prosecutionCase);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(defendant)).thenReturn(jsonObject);
        when(objectToJsonObjectConverter.convert(prosecutionCase)).thenReturn(jsonObject);
        when(repository.findByCaseId(defendant.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);
        eventListener.processProsecutionCaseDefendantUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());

    }
}
