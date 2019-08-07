package uk.gov.moj.cpp.application.event.listener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationStatusChanged;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import javax.json.JsonObject;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;


@RunWith(MockitoJUnitRunner.class)
public class CourtApplicationEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    
    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    
    @Mock
    private CourtApplicationRepository repository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private CourtApplicationCreated courtApplicationCreated;

    @Mock
    private CourtApplicationUpdated courtApplicationUpdated;

    @Mock
    private CourtApplicationEntity courtApplicationEntity;

    @Mock
    private CourtApplication courtApplication;
    
    @Mock
    private SearchProsecutionCase searchApplication;

    @Captor
    private ArgumentCaptor<CourtApplicationEntity> argumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private CourtApplicationEventListener eventListener;

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
    public void shouldHandleCourtApplicationCreatedEvent() {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        
        when(jsonObjectToObjectConverter.convert(payload, CourtApplicationCreated.class))
                .thenReturn(courtApplicationCreated);
        when(envelope.metadata()).thenReturn(metadata);
        when(courtApplication.getId()).thenReturn(UUID.randomUUID());
        when(courtApplicationCreated.getCourtApplication()).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        eventListener.processCourtApplicationCreated(envelope);
        verify(repository).save(argumentCaptor.capture());
    }

    @Test
    public void shouldHandleCourtApplicationStatusChangedEvent() {
    	final UUID applicationId = UUID.randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
    	persistedEntity.setApplicationId(applicationId);
    	persistedEntity.setPayload(payload.toString());

        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);
        when(stringToJsonObjectConverter.convert(payload.toString())).thenReturn(payload);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, CourtApplicationStatusChanged.class))
        .thenReturn(CourtApplicationStatusChanged.courtApplicationStatusChanged()
                .withId(applicationId)
                .withApplicationStatus(ApplicationStatus.LISTED)
                .build());
        when(jsonObjectToObjectConverter.convert(payload, CourtApplication.class)).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);

        eventListener.processCourtApplicationStatusChanged(envelope);
        verify(repository).save(argumentCaptor.capture());
        
    }

    @Test
    public void shouldHandleCourtApplicationUpdatedEvent() {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        when(jsonObjectToObjectConverter.convert(payload, CourtApplicationUpdated.class))
                .thenReturn(courtApplicationUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(courtApplication.getId()).thenReturn(UUID.randomUUID());
        when(courtApplicationUpdated.getCourtApplication()).thenReturn(courtApplication);
        when(repository.findByApplicationId(any())).thenReturn(courtApplicationEntity);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        eventListener.processCourtApplicationUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());
    }

}
