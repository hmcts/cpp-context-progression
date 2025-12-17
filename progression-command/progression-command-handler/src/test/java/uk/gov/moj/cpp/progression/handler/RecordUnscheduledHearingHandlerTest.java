package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.RecordUnscheduledHearing;
import uk.gov.justice.core.courts.UnscheduledHearingRecorded;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecordUnscheduledHearingHandlerTest {
    @InjectMocks
    private RecordUnscheduledHearingHandler recordUnscheduledHearingHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            UnscheduledHearingRecorded.class);

    private HearingAggregate hearingAggregate;

    private final UUID HEARING_ID = randomUUID();

    private final UUID UNSCHEDULED_HEARING_ID = randomUUID();

    @BeforeEach
    public void setup() {
        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldHandleRecordUnscheduledHearingCommand() throws EventStreamException {
        final RecordUnscheduledHearing recordUnscheduledHearing = RecordUnscheduledHearing.recordUnscheduledHearing()
                .withHearingId(HEARING_ID)
                .withUnscheduledHearingIds(asList(UNSCHEDULED_HEARING_ID))
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.record-unscheduled-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<RecordUnscheduledHearing> envelope = envelopeFrom(metadata, recordUnscheduledHearing);

        recordUnscheduledHearingHandler.handleRecordUnscheduledHearing(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.unscheduled-hearing-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(HEARING_ID.toString())),
                                withJsonPath("$.unscheduledHearingIds.length()", is(1)),
                                withJsonPath("$.unscheduledHearingIds[0]", is(UNSCHEDULED_HEARING_ID.toString()))
                                )
                        ))

                )
        );
    }
}
