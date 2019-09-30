package uk.gov.moj.cpp.application.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONValue;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationStatusChanged;
import uk.gov.justice.core.courts.ApplicationEjected;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
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
    private HearingApplicationRepository hearingApplicationRepository;

    @Mock
    private HearingRepository hearingRepository;

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

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private CourtApplicationEventListener eventListener;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    private static final String COURT_APPLICATIONS = "courtApplications";
    private static final String APPLICATION_STATUS = "applicationStatus";

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

    @Test
    public void shouldHandleApplicationEjectedEvent() throws IOException {
        final UUID applicationId = fromString("f5decee0-27b5-4dc7-8c42-66dfbc6168d6");
        final UUID hearingId = UUID.randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());

        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);

        final CourtApplication courtApplication =
                CourtApplication.courtApplication()
                        .withId(applicationId)
                        .build();

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(createPayloadForHearing());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey(applicationId, hearingId));
        hearingApplicationEntity.setHearing(hearingEntity);
        final List<CourtApplication> courtApplicationList = new ArrayList<>();
        courtApplicationList.add(courtApplication);

        Hearing hearing = Hearing.hearing().withId(hearingId)
                .withCourtApplications(courtApplicationList).build();

        when(hearingApplicationRepository.findByApplicationId(applicationId)).thenReturn(Collections.singletonList(hearingApplicationEntity));
        when(stringToJsonObjectConverter.convert(payload.toString())).thenReturn(payload);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, ApplicationEjected.class))
                .thenReturn(ApplicationEjected.applicationEjected()
                        .withApplicationId(applicationId)
                        .withRemovalReason("Legal")
                        .build());
        when(stringToJsonObjectConverter.convert(hearingEntity.getPayload())).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(payload, CourtApplication.class)).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);
        when(jsonObjectToObjectConverter.convert(jsonObject, Hearing.class)).thenReturn(hearing);
        when(objectToJsonObjectConverter.convert(any(Hearing.class))).thenReturn(Json.createObjectBuilder()
                .add("id", randomUUID().toString())
                .add(COURT_APPLICATIONS, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", applicationId.toString())
                                .add(APPLICATION_STATUS , ApplicationStatus.EJECTED.name())))
               .build());


        eventListener.processCourtApplicationEjected(envelope);
        verify(repository).save(argumentCaptor.capture());
        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        final HearingEntity updatedHearingEntity = hearingEntityArgumentCaptor.getValue();
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonNode hearingNode = mapper.valueToTree(JSONValue.parse(updatedHearingEntity.getPayload()));
        Assert.assertEquals("Check if the application status is ejected", ApplicationStatus.EJECTED.name(), hearingNode.path(COURT_APPLICATIONS).get(0).path(APPLICATION_STATUS).asText());

    }

    private String createPayloadForHearing() throws IOException {
        final StringWriter writer = new StringWriter();
        InputStream inputStream =CourtApplicationEventListenerTest.class.getResourceAsStream("/json/hearingDataCourtApplication.json");
        IOUtils.copy(inputStream, writer, UTF_8);
        inputStream.close();
        return writer.toString();

    }

}
