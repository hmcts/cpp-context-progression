package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ListUnscheduledHearing;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.UnscheduledHearingListingRequested;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListUnscheduledHearingHandlerTest {
    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private ListUnscheduledHearingHandler listUnscheduledHearingHandler;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            UnscheduledHearingListingRequested.class, ProsecutionCaseDefendantListingStatusChangedV2.class);

    private final UUID HEARING_ID = randomUUID();

    @BeforeEach
    public void setup() {
        HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldHandleListUnscheduledHearingCommand() throws EventStreamException {
        final ListUnscheduledHearing listUnscheduledHearing = ListUnscheduledHearing.listUnscheduledHearing()
                .withHearing(Hearing.hearing().withId(HEARING_ID).build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.list-unscheduled-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<ListUnscheduledHearing> envelope = envelopeFrom(metadata, listUnscheduledHearing);

        listUnscheduledHearingHandler.handleUnscheduledHearing(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).toList();

        assertThat(events.size(), is(2));
        assertThat(events.get(0).metadata().name(), is("progression.event.prosecutionCase-defendant-listing-status-changed-v2"));
        assertThat(events.get(1).metadata().name(), is("progression.event.unscheduled-hearing-listing-requested"));

    }
}
