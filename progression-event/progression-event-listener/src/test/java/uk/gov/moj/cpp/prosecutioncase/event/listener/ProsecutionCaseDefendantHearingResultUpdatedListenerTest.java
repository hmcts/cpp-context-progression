package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantHearingResultUpdated;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingResultLineEntityRepository;

import java.util.Arrays;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseDefendantHearingResultUpdatedListenerTest {
    private UUID hearingId;
    private UUID hearingResultLineId;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Mock
    private JsonEnvelope envelope;


    @Captor
    private ArgumentCaptor<HearingEntity> argumentCaptorHearingEntity;

    @Captor
    private ArgumentCaptor<HearingResultLineEntity> argumentCaptorHearingResultLineEntity;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;


    @InjectMocks
    private ProsecutionCaseDefendantHearingResultUpdatedListener eventListener;


    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingResultLineEntityRepository hearingResultLineEntityRepository;

    @BeforeEach
    public void setUp() {
        hearingId = UUID.randomUUID();
        hearingResultLineId = UUID.randomUUID();
    }


    @Test
    public void shouldHandleProsecutionCaseDefendantHearingResultEvent() throws Exception {

        final HearingResultLineEntity hearingResultLineEntity = new HearingResultLineEntity();
        hearingResultLineEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingResultLineEntity.setId(hearingResultLineId);

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.addResultLine(hearingResultLineEntity);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(Json.createObjectBuilder().build());
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantHearingResultUpdated.class)).thenReturn(ProsecutionCaseDefendantHearingResultUpdated.prosecutionCaseDefendantHearingResultUpdated().withHearingId(hearingId).withSharedResultLines(Arrays.asList(SharedResultLine.sharedResultLine().withId(UUID.randomUUID()).build())).build());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);


        eventListener.process(envelope);
        verify(hearingResultLineEntityRepository).remove(argumentCaptorHearingResultLineEntity.capture());
        verify(hearingRepository).save(argumentCaptorHearingEntity.capture());
        assertThat(argumentCaptorHearingResultLineEntity.getValue().getId(), is(hearingResultLineId));
        assertThat(argumentCaptorHearingEntity.getValue().getHearingId(), is(hearingId));

    }
}