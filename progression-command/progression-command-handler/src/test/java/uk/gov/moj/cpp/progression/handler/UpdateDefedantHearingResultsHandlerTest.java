package uk.gov.moj.cpp.progression.handler;


import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.ProsecutionCaseDefendantHearingResultUpdated;
import uk.gov.justice.core.courts.UpdateDefendantHearingResult;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateDefedantHearingResultsHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private UpdateDefedantHearingResultsHandler handler;

    private HearingAggregate aggregate;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(ProsecutionCaseDefendantHearingResultUpdated.class);

    @BeforeEach
    public void setup() {
        aggregate = new HearingAggregate();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateDefedantHearingResultsHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.update-defendant-hearing-result")
                ));
    }

    @Test
    public void shouldHandleUpdateDefendantHearingResult() throws EventStreamException {
        final UpdateDefendantHearingResult updateDefendantHearingResult = UpdateDefendantHearingResult.updateDefendantHearingResult()
                .withHearingId(randomUUID())
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-defendant-hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateDefendantHearingResult> envelope = envelopeFrom(metadata, updateDefendantHearingResult);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);
        handler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  resultEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.prosecutionCase-defendant-hearing-result-updated")).findFirst().get();

        MatcherAssert.assertThat(resultEnvelope.payloadAsJsonObject()
                , notNullValue());
    }

}
