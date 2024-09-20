package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
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

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ListUnscheduledHearing;
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
            UnscheduledHearingListingRequested.class);

    private final UUID HEARING_ID = randomUUID();

    private HearingAggregate hearingAggregate;

    @BeforeEach
    public void setup() {
        hearingAggregate = new HearingAggregate();
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

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.unscheduled-hearing-listing-requested"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearing.id", is(HEARING_ID.toString()))
                                )
                        ))

                )
        );
    }
}
