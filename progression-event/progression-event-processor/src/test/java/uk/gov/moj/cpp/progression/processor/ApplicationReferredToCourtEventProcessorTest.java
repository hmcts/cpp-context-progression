package uk.gov.moj.cpp.progression.processor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"squid:S1607"})
public class ApplicationReferredToCourtEventProcessorTest {

    @InjectMocks
    private ApplicationReferredToCourtEventProcessor eventProcessor;

    @Mock
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Mock
    private ApplicationReferredToCourt applicationReferredToCourt;

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
    public void shouldHandleApplicationReferredToCourtEventMessageForExistingHearing() {

        ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing().build();
        courtApplication = CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .build();
        HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withCourtApplications(Arrays.asList(courtApplication)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ApplicationReferredToCourt.class))
                .thenReturn(applicationReferredToCourt);
        when(applicationReferredToCourt.getHearingRequest()).thenReturn(hearingListingNeeds);
        when(progressionService.getHearing(any(), any())).thenReturn(Optional.of(Json.createObjectBuilder().add("hearing", Json.createObjectBuilder().build
                ()).build()));
        when(listCourtHearingTransformer.transform(any())).thenReturn(listCourtHearing);

        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        this.eventProcessor.process(jsonEnvelope);
        verify(listingService).listCourtHearing(jsonEnvelope, listCourtHearing);
    }

    @Test
    public void shouldHandleApplicationReferredToCourtEventMessageForNewHearing() {

        ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing().build();
        courtApplication = CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .build();
        HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withCourtApplications(Arrays.asList(courtApplication)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ApplicationReferredToCourt.class))
                .thenReturn(applicationReferredToCourt);
        when(applicationReferredToCourt.getHearingRequest()).thenReturn(hearingListingNeeds);
        when(progressionService.getHearing(any(), any())).thenReturn(Optional.empty());
        when(listCourtHearingTransformer.transform(any())).thenReturn(listCourtHearing);

        this.eventProcessor.process(jsonEnvelope);
        verify(listingService).listCourtHearing(jsonEnvelope, listCourtHearing);
    }

}