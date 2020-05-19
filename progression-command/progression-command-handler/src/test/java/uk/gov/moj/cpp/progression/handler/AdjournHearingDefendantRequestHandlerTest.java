package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.AdjournHearingDefendantRequest;
import uk.gov.justice.core.courts.HearingDefendantRequestAdjourned;
import uk.gov.justice.core.courts.HearingDefendantRequestCreated;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import java.util.UUID;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.class)
public class AdjournHearingDefendantRequestHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(HearingDefendantRequestAdjourned.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private AdjournHearingDefendantRequestHandler adjournHearingDefendantRequestHandler;

    private HearingAggregate hearingAggregate;

    @Before
    public void setup() {
        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new AdjournHearingDefendantRequestHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.adjourn-hearing-request")
                ));
    }

    @Test()
    public void shouldProcessCommand() throws Exception {

        final UUID adjournedHearingId = randomUUID();
        final UUID currentHearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();

        final AdjournHearingDefendantRequest adjournHearingDefendantRequest = AdjournHearingDefendantRequest.adjournHearingDefendantRequest()
                .withAdjournedHearingId(adjournedHearingId)
                .withCurrentHearingId(currentHearingId)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.adjourn-hearing-request")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope<AdjournHearingDefendantRequest> envelope = envelopeFrom(metadata, adjournHearingDefendantRequest);

        HearingDefendantRequestCreated hearingDefendantRequestCreated = HearingDefendantRequestCreated.hearingDefendantRequestCreated()
                .withDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(defendantId)
                        .withProsecutionCaseId(prosecutionCaseId)
                        .build()))
                .build();
        hearingAggregate.apply(hearingDefendantRequestCreated);
        adjournHearingDefendantRequestHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-defendant-request-adjourned"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.adjournedHearingId", is(adjournedHearingId.toString())),
                                withJsonPath("$.currentHearingId", is(currentHearingId.toString())),
                                withJsonPath("$.defendantRequests[0].prosecutionCaseId", is(prosecutionCaseId.toString())),
                                withJsonPath("$.defendantRequests[0].defendantId", is(defendantId.toString()))
                                )
                        ))
                )
        );
    }
}