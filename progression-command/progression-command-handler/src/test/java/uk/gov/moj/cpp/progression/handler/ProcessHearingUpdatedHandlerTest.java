package uk.gov.moj.cpp.progression.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingUpdatedProcessed;
import uk.gov.justice.core.courts.ProcessHearingUpdated;
import uk.gov.justice.progression.courts.HearingResulted;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProcessHearingUpdatedHandlerTest {

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(HearingUpdatedProcessed.class);


    private HearingAggregate hearingAggregate;

    @InjectMocks
    private ProcessHearingUpdatedHandler processHearingUpdatedHandler;

    @Before
    public void setup() {

        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldHandProcessHearingUpdated() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final ProcessHearingUpdated processHearingUpdated = ProcessHearingUpdated.processHearingUpdated()
                .withConfirmedHearing(ConfirmedHearing.confirmedHearing().withId(hearingId).build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.process-hearing-updated")
                .withId(randomUUID())
                .build();
        hearingAggregate.apply(HearingResulted.hearingResulted().withHearing(Hearing.hearing().withId(hearingId).build()).build());

        final Envelope<ProcessHearingUpdated> envelope = envelopeFrom(metadata, processHearingUpdated);
        processHearingUpdatedHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-updated-processed"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.confirmedHearing.id", is(hearingId.toString())),
                                withJsonPath("$.hearing.id", is(hearingId.toString())))
                        ))
                )
        );
    }
}
