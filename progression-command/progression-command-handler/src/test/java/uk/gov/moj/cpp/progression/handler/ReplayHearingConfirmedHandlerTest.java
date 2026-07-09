package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.progression.courts.HearingConfirmedReplayed;
import uk.gov.justice.progression.courts.MarkedHearingConfirmedForReplay;
import uk.gov.justice.progression.courts.ReplayHearingConfirmed;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReplayHearingConfirmedHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            MarkedHearingConfirmedForReplay.class,
            HearingConfirmedReplayed.class);

    @InjectMocks
    private ReplayHearingConfirmedHandler replayHearingConfirmedHandler;

    @Test
    void shouldHandleCommand() {
        assertThat(new ReplayHearingConfirmedHandler(), isHandler(COMMAND_HANDLER)
                .with(method("replayHearingConfirmed")
                        .thatHandles("progression.command.replay-hearing-confirmed")));
    }

    @Test
    void shouldReplayHearingConfirmedAndAppendEventsToStream() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final HearingAggregate hearingAggregate = new HearingAggregate();

        when(eventSource.getStreamById(hearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(hearingId)
                .build();

        final ReplayHearingConfirmed replayHearingConfirmed = ReplayHearingConfirmed.replayHearingConfirmed()
                .withConfirmedHearing(confirmedHearing)
                .withSendNotificationToParties(false)
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.command.replay-hearing-confirmed")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<ReplayHearingConfirmed> envelope = Envelope.envelopeFrom(metadata, replayHearingConfirmed);

        replayHearingConfirmedHandler.replayHearingConfirmed(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.marked-hearing-confirmed-for-replay"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.confirmedHearing", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.sendNotificationToParties", notNullValue())))
                )
        ));
    }

    @Test
    void shouldInvokeEventSourceAndAggregateServiceWhenReplayHearingConfirmed() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final HearingAggregate hearingAggregate = new HearingAggregate();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final ReplayHearingConfirmed replayHearingConfirmed = ReplayHearingConfirmed.replayHearingConfirmed()
                .withConfirmedHearing(ConfirmedHearing.confirmedHearing().withId(hearingId).build())
                .withSendNotificationToParties(true)
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.command.replay-hearing-confirmed")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<ReplayHearingConfirmed> envelope = Envelope.envelopeFrom(metadata, replayHearingConfirmed);

        replayHearingConfirmedHandler.replayHearingConfirmed(envelope);

        verify(eventSource).getStreamById(hearingId);
        verify(aggregateService).get(eventStream, HearingAggregate.class);
    }
}
