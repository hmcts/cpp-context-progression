package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.progression.courts.HearingMovedToUnallocated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HmiEventListenerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private HmiEventListener hmiEventListener;

    @Captor
    private ArgumentCaptor<HearingEntity> captorForHearingEntity;

    @Test
    public void shouldUpdateViewStore() {
        final UUID hearingId = UUID.randomUUID();

        final HearingMovedToUnallocated hearingMovedToUnallocated = HearingMovedToUnallocated.hearingMovedToUnallocated()
                .withHearing(Hearing.hearing().withId(hearingId).build()).build();

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearingMovedToUnallocated.getHearing()).toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_RESULTED);
        hearingEntity.setConfirmedDate(LocalDate.now());
        when(hearingRepository.findBy(eq(hearingId))).thenReturn(hearingEntity);


        hmiEventListener.handleHearingMovedToUnallocated(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-moved-to-unallocated"),
                objectToJsonObjectConverter.convert(hearingMovedToUnallocated)));

        verify(hearingRepository).save(captorForHearingEntity.capture());

        final HearingEntity resultEntity = captorForHearingEntity.getValue();
        assertThat(resultEntity.getHearingId(), is(hearingId));
        assertThat(resultEntity.getListingStatus(), is(HearingListingStatus.SENT_FOR_LISTING));
        assertThat(resultEntity.getConfirmedDate(), nullValue());
        assertThat(resultEntity.getPayload(), is(objectToJsonObjectConverter.convert(hearingMovedToUnallocated.getHearing()).toString()));

    }

}
