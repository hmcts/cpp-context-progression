package uk.gov.moj.cpp.progression.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.core.courts.BoxworkApplicationReferred;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"squid:S1607"})
public class BoxWorkApplicationReferredTest {

    @InjectMocks
    private BoxWorkApplicationReferredEventProcessor eventProcessor;

    @Mock
    private BoxworkApplicationReferred boxworkApplicationReferred;

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
    private Initiate initiate;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Spy
    private final Enveloper enveloper = createEnveloper();
    private static final String HEARING_INITIATE_COMMAND = "hearing.initiate";
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonEnvelope finalEnvelope;
    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();


    @Test
    public void shouldHandleBoxWorkApplicationReferredEventMessageForExistingHearing() throws Exception {

        HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withCourtApplications(Arrays.asList(courtApplication)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), BoxworkApplicationReferred.class))
                .thenReturn(boxworkApplicationReferred);

        when(objectToJsonObjectConverter.convert(Mockito.any(CourtApplication.class))).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(Hearing.class))).thenReturn(Hearing.hearing().build());

        Hearing hearing = Hearing.hearing()
                .withCourtCentre(generateBasicCourtCentre())
                .withIsBoxHearing(true).withId(UUID.randomUUID())
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                        .withId(UUID.randomUUID()).withDueDate(LocalDate.now()).build()))
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString("2015-01-12T05:27:17.210Z")).build()))
                .build();



        when(progressionService.transformBoxWorkApplication(boxworkApplicationReferred)).thenReturn(hearing);
        when(boxworkApplicationReferred.getHearingRequest()).thenReturn(hearingListingNeeds);
        doNothing().when(progressionService).updateHearingListingStatusToHearingInitiated(anyObject(), anyObject());
        doNothing().when(progressionService).updateCourtApplicationStatus(anyObject(), any(UUID.class), anyObject());
        when(enveloper.withMetadataFrom(jsonEnvelope, HEARING_INITIATE_COMMAND )).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(enveloper.withMetadataFrom(jsonEnvelope, BoxWorkApplicationReferredEventProcessor.PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED))
                .thenReturn(enveloperFunction);

        this.eventProcessor.processBoxWorkApplication(jsonEnvelope);

        verify(sender,times(2)).send(finalEnvelope);
        verify(progressionService, times(1))
                .transformBoxWorkApplication(any());
        verify(progressionService, times(1))
                .updateCourtApplicationStatus(anyObject(), any(UUID.class), anyObject());

    }

    private CourtCentre generateBasicCourtCentre() {
        return CourtCentre.courtCentre()
                .withId(COURT_CENTRE_ID)
                .build();
    }

}