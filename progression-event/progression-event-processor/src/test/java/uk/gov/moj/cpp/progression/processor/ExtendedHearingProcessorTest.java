package uk.gov.moj.cpp.progression.processor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"squid:S1607"})
public class ExtendedHearingProcessorTest {

    @InjectMocks
    private ExtendedHearingProcessor eventProcessor;

    @Mock
    private HearingExtended hearingExtended;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private Sender sender;
    @Mock
    private ProgressionService progressionService;
    @Mock
    private CourtApplication courtApplication;
    @Mock
    private ProsecutionCase prosecutionCase;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Spy
    private final Enveloper enveloper = createEnveloper();
    static final String PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonEnvelope finalEnvelope;
    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleHearingExtendedEventMessageForExistingHearingForApplication() {

        HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withCourtApplications(Arrays.asList(courtApplication)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingExtended.class)).thenReturn(hearingExtended);
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtApplication.class))).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(Hearing.class))).thenReturn(Hearing.hearing().build());
        when(hearingExtended.getHearingRequest()).thenReturn(hearingListingNeeds);
        when(progressionService.getHearing(any(), any())).thenReturn(Optional.of(Json.createObjectBuilder().add("hearing", Json.createObjectBuilder().build()).build()));
        when(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED )).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        this.eventProcessor.process(jsonEnvelope);
        verify(sender).send(finalEnvelope);
        verify(progressionService).updateDefendantYouthForProsecutionCase(any(), anyList());
    }

    @Test
    public void shouldHandleHearingExtendedEventMessageForExistingHearingForCase() {


        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withProsecutionCases(Arrays.asList(prosecutionCase)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingExtended.class)).thenReturn(hearingExtended);
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtApplication.class))).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(Hearing.class))).thenReturn(Hearing.hearing().build());
        when(hearingExtended.getHearingRequest()).thenReturn(hearingListingNeeds);
        when(progressionService.getHearing(any(), any())).thenReturn(Optional.of(Json.createObjectBuilder().add("hearing", Json.createObjectBuilder().build()).build()));
        when(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED )).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        this.eventProcessor.process(jsonEnvelope);
        verify(sender).send(finalEnvelope);
        verify(progressionService, times(1)).linkProsecutionCasesToHearing(any(JsonEnvelope.class),any(UUID.class),any(List.class));
        verify(progressionService).updateDefendantYouthForProsecutionCase(any(), anyList());
    }

}

