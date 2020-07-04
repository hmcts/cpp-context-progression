package uk.gov.moj.cpp.progression.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.core.courts.SlotsBookedForApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"squid:S1607"})
public class SlotsBookedForApplicationEventProcessorTest {

    @InjectMocks
    private SlotsBookedForApplicationEventProcessor eventProcessor;

    @Mock
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Mock
    private SlotsBookedForApplication slotsBookedForApplication;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ListingService listingService;
    @Mock
    private ProgressionService progressionService;
    @Mock
    private CourtApplication courtApplication;
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonEnvelope finalEnvelope;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleSlotsBookedForApplicationEventMessageWithNewHearing() {

        final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing().build();

        courtApplication = CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .build();

        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withBookedSlots(Collections.singletonList(RotaSlot.rotaSlot()
                        .withCourtRoomId(new Random().nextInt())
                        .withCourtScheduleId(UUID.randomUUID().toString())
                        .withDuration(15)
                        .withOucode("AA001")
                        .withSession("AM")
                        .withStartTime(ZonedDateTime.now()).build()))
                .withCourtApplications(Arrays.asList(courtApplication)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), SlotsBookedForApplication.class))
                .thenReturn(slotsBookedForApplication);
        when(slotsBookedForApplication.getHearingRequest()).thenReturn(hearingListingNeeds);
        when(progressionService.getHearing(any(), any())).thenReturn(Optional.empty());
        when(listCourtHearingTransformer.transform(any())).thenReturn(listCourtHearing);

        this.eventProcessor.process(jsonEnvelope);
        verify(progressionService).updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);
        verify(progressionService).updateCourtApplicationStatus(jsonEnvelope, courtApplication.getId(), ApplicationStatus.UN_ALLOCATED);
        verify(listingService).listCourtHearing(jsonEnvelope, listCourtHearing);

    }

}
