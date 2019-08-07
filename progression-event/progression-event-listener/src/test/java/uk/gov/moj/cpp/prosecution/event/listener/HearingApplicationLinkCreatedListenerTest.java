package uk.gov.moj.cpp.prosecution.event.listener;

import org.apache.http.HeaderIterator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationLinkCreated;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.event.listener.HearingApplicationLinkCreatedListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@RunWith(MockitoJUnitRunner.class)
public class HearingApplicationLinkCreatedListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    
    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    
    @Mock
    private HearingApplicationRepository repository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject jsonObject;

    @Captor
    private ArgumentCaptor<HearingApplicationEntity> argumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private HearingApplicationLinkCreatedListener eventListener;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    @Mock
    private HearingRepository hearingRepository;

    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();

    @Before
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
    }

    @Test
    public void shouldHandleHearingApplicationLinkCreatedEvent() {

        final Hearing hearing = Hearing.hearing().withId(HEARING_ID).build();

        HearingApplicationLinkCreated hearingApplicationLinkCreated
                = HearingApplicationLinkCreated.hearingApplicationLinkCreated ()
                .withApplicationId(APPLICATION_ID)
                .withHearing(hearing)
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, HearingApplicationLinkCreated.class))
                .thenReturn(hearingApplicationLinkCreated);
        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearingEntity);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(Json.createObjectBuilder().build());
        eventListener.process(envelope);
        verify(repository).save(argumentCaptor.capture());
    }
}
