package uk.gov.moj.cpp.progression.event;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.UnscheduledHearingListingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;
import uk.gov.moj.cpp.progression.helper.HearingUnscheduledListingHelper;

import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UnscheduledHearingListingRequestedEventProcessorTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();

    @InjectMocks
    private UnscheduledHearingListingRequestedEventProcessor processor;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Mock
    private HearingUnscheduledListingHelper hearingUnscheduledListingHelper;

    @Captor
    private ArgumentCaptor<Hearing> argumentCaptor;

    @Test
    public void shouldProcessUnscheduledHearingListingRequestedEvent(){
        final UnscheduledHearingListingRequested unscheduledHearingListingRequested = UnscheduledHearingListingRequested.unscheduledHearingListingRequested()
                .withHearing(Hearing.hearing().withId(HEARING_ID)
                        .withCourtApplications(List.of(CourtApplication.courtApplication()
                                .withId(APPLICATION_ID)
                                .withJudicialResults(List.of(JudicialResult.judicialResult().build()))
                                .build()))
                        .build())
                .build();

        final JsonArray courtApplications = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("id", APPLICATION_ID.toString())
                        .add("judicialResults", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("judicialResultId", randomUUID().toString())
                                        .build())
                                .build())
                        .build()).build();

        final JsonObject hearing = createObjectBuilder().add("hearing", createObjectBuilder()
                .add("id", HEARING_ID.toString())
                .add("courtApplications", courtApplications)
                .build()).build();

        final JsonEnvelope event = createEnvelope("progression.event.unscheduled-hearing-listing-requested", hearing);

        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(unscheduledHearingListingRequested);

        processor.process(event);

        verify(hearingResultUnscheduledListingHelper, times(1))
                .processUnscheduledCourtHearings(any(), argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), notNullValue());
        assertThat(argumentCaptor.getValue().getId(), is(HEARING_ID));
    }

    @Test
    public void shouldProcessUnscheduledHearingListingRequestedEventForNewHearing(){
        final UnscheduledHearingListingRequested unscheduledHearingListingRequested = UnscheduledHearingListingRequested.unscheduledHearingListingRequested()
                .withHearing(Hearing.hearing().withId(HEARING_ID)
                        .build())
                .build();

        final JsonObject hearing = createObjectBuilder().add("hearing", createObjectBuilder()
                .add("id", HEARING_ID.toString())
                .build()).build();

        final JsonEnvelope event = createEnvelope("progression.event.unscheduled-hearing-listing-requested", hearing);

        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(unscheduledHearingListingRequested);

        processor.process(event);

        verify(hearingUnscheduledListingHelper, times(1))
                .processUnscheduledHearings(any(), argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), notNullValue());
        assertThat(argumentCaptor.getValue().getId(), is(HEARING_ID));
    }

}
