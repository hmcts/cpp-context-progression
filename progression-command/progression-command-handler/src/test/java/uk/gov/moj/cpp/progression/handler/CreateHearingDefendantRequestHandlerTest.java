package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.CreateHearingDefendantRequest;
import uk.gov.justice.core.courts.HearingDefendantRequestCreated;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.progression.courts.BoxworkHearingLinked;
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
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateHearingDefendantRequestHandlerTest {

    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventStream boxworkEventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingDefendantRequestCreated.class,
            BoxworkHearingLinked.class);

    @InjectMocks
    private CreateHearingDefendantRequestHandler handler;

    @Test
    public void shouldCreateHearingDefendantRequest() throws EventStreamException {
        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final CreateHearingDefendantRequest payload = CreateHearingDefendantRequest.createHearingDefendantRequest()
                .withDefendantRequests(Arrays.asList(ListDefendantRequest
                        .listDefendantRequest()
                        .withDefendantId(DEFENDANT_ID)
                        .withProsecutionCaseId(CASE_ID)
                        .build()))
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.create-hearing-defendant-request")
                .withId(randomUUID())
                .build();

        final Envelope<CreateHearingDefendantRequest> envelope = envelopeFrom(metadata, payload);

        handler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-defendant-request-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendantRequests[0].defendantId", is(DEFENDANT_ID.toString())),
                                withJsonPath("$.defendantRequests[0].prosecutionCaseId", is(CASE_ID.toString()))
                                )
                        ))

                )
        );
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new CreateHearingDefendantRequestHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.create-hearing-defendant-request")
                ));
    }

    @Test
    public void shouldLinkBoxworkHearingToFirstHearingWhenSummonsApprovedOutcomePresent() throws EventStreamException {
        final UUID firstHearingId = randomUUID();
        final UUID boxworkHearingId = randomUUID();

        final HearingAggregate firstHearingAggregate = new HearingAggregate();
        final HearingAggregate boxworkHearingAggregate = new HearingAggregate();

        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(eventSource.getStreamById(boxworkHearingId)).thenReturn(boxworkEventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(firstHearingAggregate);
        when(aggregateService.get(boxworkEventStream, HearingAggregate.class)).thenReturn(boxworkHearingAggregate);

        final SummonsApprovedOutcome summonsApprovedOutcome = SummonsApprovedOutcome.summonsApprovedOutcome()
                .withHearingId(boxworkHearingId)
                .build();

        final CreateHearingDefendantRequest payload = CreateHearingDefendantRequest.createHearingDefendantRequest()
                .withHearingId(firstHearingId)
                .withDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(DEFENDANT_ID)
                        .withProsecutionCaseId(CASE_ID)
                        .withSummonsApprovedOutcome(summonsApprovedOutcome)
                        .build()))
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.command.create-hearing-defendant-request")
                .withId(randomUUID())
                .build();

        handler.handle(envelopeFrom(metadata, payload));

        final Stream<JsonEnvelope> boxworkStream = verifyAppendAndGetArgumentFrom(boxworkEventStream);
        assertThat(boxworkStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.boxwork-hearing-linked"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.boxworkHearingId", is(boxworkHearingId.toString())),
                                withJsonPath("$.firstHearingId", is(firstHearingId.toString()))
                        )))
        ));
    }

    @Test
    public void shouldNotLinkBoxworkHearingWhenAlreadyLinked() throws EventStreamException {
        final UUID firstHearingId = randomUUID();
        final UUID boxworkHearingId = randomUUID();

        final HearingAggregate firstHearingAggregate = new HearingAggregate();
        final HearingAggregate boxworkHearingAggregate = new HearingAggregate();
        ReflectionUtil.setField(boxworkHearingAggregate, "firstHearingId", randomUUID());

        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(eventSource.getStreamById(boxworkHearingId)).thenReturn(boxworkEventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(firstHearingAggregate);
        when(aggregateService.get(boxworkEventStream, HearingAggregate.class)).thenReturn(boxworkHearingAggregate);

        final SummonsApprovedOutcome summonsApprovedOutcome = SummonsApprovedOutcome.summonsApprovedOutcome()
                .withHearingId(boxworkHearingId)
                .build();

        final CreateHearingDefendantRequest payload = CreateHearingDefendantRequest.createHearingDefendantRequest()
                .withHearingId(firstHearingId)
                .withDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(DEFENDANT_ID)
                        .withProsecutionCaseId(CASE_ID)
                        .withSummonsApprovedOutcome(summonsApprovedOutcome)
                        .build()))
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.command.create-hearing-defendant-request")
                .withId(randomUUID())
                .build();

        handler.handle(envelopeFrom(metadata, payload));

        verify(boxworkEventStream, never()).append(any());
    }

    @Test
    public void shouldNotAttemptBoxworkLinkingWhenNoSummonsApprovedOutcome() throws EventStreamException {
        final UUID firstHearingId = randomUUID();
        final HearingAggregate firstHearingAggregate = new HearingAggregate();

        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(firstHearingAggregate);

        final CreateHearingDefendantRequest payload = CreateHearingDefendantRequest.createHearingDefendantRequest()
                .withHearingId(firstHearingId)
                .withDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(DEFENDANT_ID)
                        .withProsecutionCaseId(CASE_ID)
                        .build()))
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.command.create-hearing-defendant-request")
                .withId(randomUUID())
                .build();

        handler.handle(envelopeFrom(metadata, payload));

        verify(boxworkEventStream, never()).append(any());
    }

}