package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.of;
import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class HearingUpdatedEventProcessorTest {

    @Captor
    private ArgumentCaptor<HearingUpdated> hearingUpdatedArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Hearing> hearingArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> applicationIdsArgumentCaptor;

    @Captor
    private ArgumentCaptor<HearingListingStatus> hearingListingStatusArgumentCaptor;

    @InjectMocks
    private HearingUpdatedEventProcessor eventProcessor;

    @Mock
    private ProgressionService progressionService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void initMocks() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleHearingConfirmedEventMessage() {
        final UUID hearingId = UUID.randomUUID();
        final HearingUpdated hearingUpdated = HearingUpdated.hearingUpdated()
                .withUpdatedHearing(ConfirmedHearing.confirmedHearing()
                        .withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase()
                                .withId(UUID.randomUUID())
                                .build()))
                        .withCourtApplicationIds(Arrays.asList(UUID.randomUUID()))
                        .build())
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .build();

        final Optional<JsonObject> hearingJson = of(createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .build());

        JsonEnvelope jsonEnvelope = getJsonEnvelope(hearingUpdated);
        when(progressionService.getHearing(jsonEnvelope, hearingId.toString())).thenReturn(hearingJson);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);

        eventProcessor.publishHearingDetailChangedPublicEvent(jsonEnvelope);

        verify(progressionService, times(1)).updateHearingListingStatusToHearingUpdate(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture());
        assertEquals(hearing, hearingArgumentCaptor.getValue());

        verify(progressionService, times(1)).linkApplicationsToHearing(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());

        verify(progressionService, times(1)).publishHearingDetailChangedPublicEvent(envelopeArgumentCaptor.capture(), hearingUpdatedArgumentCaptor.capture());
        assertEquals(hearingUpdated.getUpdatedHearing().getId(), hearingUpdatedArgumentCaptor.getValue().getUpdatedHearing().getId());
    }

    private JsonEnvelope getJsonEnvelope(final HearingUpdated hearingUpdated) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.listing.hearing-updated"),
                objectToJsonObjectConverter.convert(hearingUpdated));
    }

}