package uk.gov.moj.cpp.application.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ApplicationStatus.FINALISED;
import static uk.gov.justice.core.courts.ApplicationStatus.IN_PROGRESS;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationStatusUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtApplicationsUpdatedEventListenerTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @InjectMocks
    private CourtApplicationsUpdatedEventListener listener;

    @BeforeEach
    void setUp() {
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    void shouldProcessApplicationStatusUpdated(){
        final UUID applicationId = UUID.randomUUID();
        final CourtApplicationEntity entity = new CourtApplicationEntity();
        entity.setApplicationId(applicationId);
        final CourtApplication courtApplication = CourtApplication.courtApplication().withApplicationStatus(FINALISED).build();
        entity.setPayload(objectToJsonObjectConverter.convert(courtApplication).toString());
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(entity);

        listener.processApplicationStatusUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.court-application-status-updated"),
                objectToJsonObjectConverter.convert(CourtApplicationStatusUpdated.courtApplicationStatusUpdated()
                        .withApplicationStatus(IN_PROGRESS)
                        .withId(applicationId)
                        .build()
                )));

        final CourtApplication expectedApplication = CourtApplication.courtApplication()
                .withApplicationStatus(IN_PROGRESS)
                .withId(applicationId)
                .build();
        entity.setPayload(objectToJsonObjectConverter.convert(expectedApplication).toString());
        verify(courtApplicationRepository).save(entity);
    }

}
