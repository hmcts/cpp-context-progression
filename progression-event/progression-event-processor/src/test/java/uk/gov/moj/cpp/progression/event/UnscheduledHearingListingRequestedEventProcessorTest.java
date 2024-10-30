package uk.gov.moj.cpp.progression.event;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.UnscheduledHearingListingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UnscheduledHearingListingRequestedEventProcessorTest {

    private static final UUID HEARING_ID = randomUUID();

    @InjectMocks
    private UnscheduledHearingListingRequestedEventProcessor processor;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Captor
    private ArgumentCaptor<Hearing> argumentCaptor;

    @Test
    public void shouldProcessUnscheduledHearingListingRequestedEvent(){
        final UnscheduledHearingListingRequested unscheduledHearingListingRequested = UnscheduledHearingListingRequested.unscheduledHearingListingRequested()
                .withHearing(Hearing.hearing().withId(HEARING_ID).build())
                .build();

        final JsonEnvelope event = createEnvelope("progression.event.unscheduled-hearing-listing-requested"
                , createObjectBuilder().add("hearing", createObjectBuilder().add("id", HEARING_ID.toString()))
                        .build());
        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(unscheduledHearingListingRequested);

        processor.process(event);

        verify(hearingResultUnscheduledListingHelper, times(1))
                .processUnscheduledCourtHearings(any(), argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), notNullValue());
        assertThat(argumentCaptor.getValue().getId(), is(HEARING_ID));
    }

}
