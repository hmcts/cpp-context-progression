package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.DefendantsToRemove;
import uk.gov.justice.core.courts.HearingUpdatedForPartialAllocation;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
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
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateHearingForPartialAllocationHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(HearingUpdatedForPartialAllocation.class);

    @InjectMocks
    private UpdateHearingForPartialAllocationHandler updateHearingForPartialAllocationHandler;

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateHearingForPartialAllocationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.update-hearing-for-partial-allocation")
                ));
    }

    @Test
    public void shouldProcessCommand() throws EventStreamException {

        final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = createUpdateHearingForPartialAllocationPayload();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-hearing-for-partial-allocation")
                .withId(updateHearingForPartialAllocation.getHearingId())
                .build();

        final Envelope<UpdateHearingForPartialAllocation> envelope = envelopeFrom(metadata, updateHearingForPartialAllocation);

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        updateHearingForPartialAllocationHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-updated-for-partial-allocation"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", notNullValue()))
                        ))
                )
        );
    }

    private static UpdateHearingForPartialAllocation createUpdateHearingForPartialAllocationPayload() {

        return UpdateHearingForPartialAllocation.updateHearingForPartialAllocation()
                .withHearingId(randomUUID())
                .withProsecutionCasesToRemove(Arrays.asList(
                        ProsecutionCasesToRemove.prosecutionCasesToRemove()
                                .withCaseId(randomUUID())
                                .withDefendantsToRemove(Arrays.asList(
                                        DefendantsToRemove.defendantsToRemove()
                                                .withDefendantId(randomUUID())
                                                .withOffencesToRemove(Arrays.asList(
                                                        OffencesToRemove.offencesToRemove()
                                                                .withOffenceId(randomUUID()).build()
                                                ))
                                                .build()
                                ))
                                .build()
                )).build();
    }

}

