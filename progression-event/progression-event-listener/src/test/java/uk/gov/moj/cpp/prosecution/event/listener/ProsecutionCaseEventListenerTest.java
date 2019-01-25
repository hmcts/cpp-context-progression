package uk.gov.moj.cpp.prosecution.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.event.listener.ProsecutionCaseEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.util.Collections;
import java.util.UUID;

import javax.json.JsonObject;

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
public class ProsecutionCaseEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ProsecutionCaseRepository repository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private ProsecutionCaseCreated prosecutionCaseCreated;

    @Mock
    private ProsecutionCase prosecutionCase;

    @Mock
    private Defendant defendant;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private ProsecutionCaseEventListener eventListener;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    @Mock
    private SearchProsecutionCase searchCase;

    @Before
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
    }

    @Test
    public void shouldHandleProsecutionCaseCreatedEvent() {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseCreated.class))
                .thenReturn(prosecutionCaseCreated);
        when(envelope.metadata()).thenReturn(metadata);
        when(prosecutionCase.getId()).thenReturn(UUID.randomUUID());
        when(prosecutionCase.getDefendants()).thenReturn(Collections.singletonList(defendant));
        when(prosecutionCaseCreated.getProsecutionCase()).thenReturn(prosecutionCase);
        when(objectToJsonObjectConverter.convert(prosecutionCase)).thenReturn(jsonObject);
        eventListener.processProsecutionCaseCreated(envelope);
        verify(repository).save(argumentCaptor.capture());
    }
}
