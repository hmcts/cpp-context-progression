package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.test.FileUtil.jsonFromString;


import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.court.EventHearingRemoveDuplicateApplicationBdf;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.util.FileUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class HearingConfirmedEventListenerTest {

    @Mock
    private Envelope<Initiate> initiateEnvelope;

    @Mock
    private HearingRepository hearingRepository;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    @InjectMocks
    private HearingConfirmedEventListener hearingConfirmedEventListener;

    private final StringToJsonObjectConverter converter = new StringToJsonObjectConverter();
    @Mock
    private Envelope<EventHearingRemoveDuplicateApplicationBdf> bdfEnvelope;
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldProcessHearingInitiatedConfirmDateEvent() {
        final UUID hearingId = randomUUID();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);

        when(initiateEnvelope.payload()).thenReturn(Initiate.initiate().withHearing(Hearing.hearing()
                                                                                    .withId(hearingId).build()).build());
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        hearingConfirmedEventListener.processHearingInitiatedEnrichedEvent(initiateEnvelope);

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());

        final LocalDate confirmedDate = hearingEntityArgumentCaptor.getValue().getConfirmedDate();

        assertThat(confirmedDate.toString(), is(LocalDate.now().toString()));
    }

    @Test
    public void shouldUpdateHearingWithUniqueCourtApplications() throws IOException {
        final String hearingPayloadString = FileUtil.getPayload("json/hearing-with-unique-application.json");
        JsonObject jsonObject = converter.convert(hearingPayloadString);

        final EventHearingRemoveDuplicateApplicationBdf hearingRemoveDuplicateApplicationBdf = this.jsonObjectToObjectConverter.convert(jsonObject, EventHearingRemoveDuplicateApplicationBdf.class);
        final UUID hearingId = hearingRemoveDuplicateApplicationBdf.getHearing().getId();
        final HearingEntity hearingEntity = createHearingEntity(hearingId);

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(bdfEnvelope.payload()).thenReturn(hearingRemoveDuplicateApplicationBdf);
        hearingConfirmedEventListener.processHearingRemoveDuplicateApplicationBdfEvent(bdfEnvelope);

        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();
        HearingEntity savedHearingEntity = savedHearingEntities.stream().filter(savedDbHeairng -> savedDbHeairng.getHearingId().equals(hearingId))
                .findFirst().get();
        final Hearing savedHearingObject = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearingObject, notNullValue());
        assertTrue(savedHearingObject.getCourtApplications().size() == 1);
    }

    @Test
    public void shouldUpdateHearingWithUniqueCourtApplicationsbyBdfWithoutApplication() throws IOException {
        final String hearingPayloadString = FileUtil.getPayload("json/hearing-without-application.json");
        JsonObject jsonObject = converter.convert(hearingPayloadString);

        final EventHearingRemoveDuplicateApplicationBdf hearingRemoveDuplicateApplicationBdf = this.jsonObjectToObjectConverter.convert(jsonObject, EventHearingRemoveDuplicateApplicationBdf.class);
        final UUID hearingId = hearingRemoveDuplicateApplicationBdf.getHearing().getId();
        final HearingEntity hearingEntity = createHearingEntity(hearingId);

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(bdfEnvelope.payload()).thenReturn(hearingRemoveDuplicateApplicationBdf);
        hearingConfirmedEventListener.processHearingRemoveDuplicateApplicationBdfEvent(bdfEnvelope);

        verify(this.hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> savedHearingEntities = hearingEntityArgumentCaptor.getAllValues();
        HearingEntity savedHearingEntity = savedHearingEntities.stream().filter(savedDbHeairng -> savedDbHeairng.getHearingId().equals(hearingId))
                .findFirst().get();
        final Hearing savedHearingObject = this.jsonObjectToObjectConverter.convert(jsonFromString(savedHearingEntity.getPayload()), Hearing.class);

        assertThat(savedHearingObject, notNullValue());
        assertTrue(savedHearingObject.getCourtApplications() == null);
    }

    private HearingEntity createHearingEntity(final UUID hearingId) throws IOException {
        String hearingPayload =  FileUtil.getPayload("json/hearing-with-duplicate-applications.json");
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_RESULTED);
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setResultLines(new HashSet<>());
        hearingEntity.setPayload(hearingPayload);
        return hearingEntity;
    }
}
