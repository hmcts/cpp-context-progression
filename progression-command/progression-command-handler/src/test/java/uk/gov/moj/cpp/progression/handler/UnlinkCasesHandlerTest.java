package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.progression.courts.Cases;
import uk.gov.justice.progression.courts.UnlinkCases;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.events.CasesUnlinked;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UnlinkCasesHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(UnlinkCases.class,
            CasesUnlinked.class);

    @InjectMocks
    private UnlinkCasesHandler unlinkCasesHandler;


    private CaseAggregate caseAggregate;

    @Test
    public void shouldHandleUnlinkCommand() {
        assertThat(unlinkCasesHandler, isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.unlink-cases")
                ));
    }

    @Test
    public void shouldHandleUnlinkCases() throws EventStreamException {
        caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        final List<Cases> cases = new ArrayList<>();
        cases.add(Cases.cases()
                .withCaseUrn("Case123")
                .withCaseId(randomUUID())
                .withLinkGroupId(randomUUID())
                .build());
        final UnlinkCases unlinkCases = UnlinkCases.unlinkCases()
                .withProsecutionCaseId(randomUUID())
                .withCases(cases)
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.unlink-cases")
                .withId(randomUUID())
                .build();

        final Envelope<UnlinkCases> envelope = envelopeFrom(metadata, unlinkCases);

        unlinkCasesHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  resultEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.cases-unlinked")).findFirst().get();

        MatcherAssert.assertThat(resultEnvelope.payloadAsJsonObject()
                , notNullValue());
    }
}
