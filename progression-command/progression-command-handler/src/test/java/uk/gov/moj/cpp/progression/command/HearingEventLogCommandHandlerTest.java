package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.progression.courts.AaagHearingEventLogsDocumentCreated;
import uk.gov.justice.progression.courts.HearingEventLogsDocumentCreated;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.api.CreateHearingEventLogDocument;
import uk.gov.moj.cpp.progression.handler.HearingEventLogCommandHandler;

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
public class HearingEventLogCommandHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingEventLogsDocumentCreated.class);

    @InjectMocks
    private HearingEventLogCommandHandler hearingEventLogCommandHandler;


    private CaseAggregate aggregate;


    @BeforeEach
    public void setup() {
        aggregate = new CaseAggregate();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new HearingEventLogCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.create-hearing-event-log-document")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final CreateHearingEventLogDocument hearingEventLogsDocument = CreateHearingEventLogDocument.createHearingEventLogDocument().withCaseId(randomUUID()).build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.create-hearing-event-log-document")
                .withId(randomUUID())
                .build();

        final Envelope<CreateHearingEventLogDocument> envelope = envelopeFrom(metadata, hearingEventLogsDocument);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);

        hearingEventLogCommandHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope hearingResultedEnvelope = (JsonEnvelope) envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-event-logs-document-created")).findFirst().get();

        MatcherAssert.assertThat(hearingResultedEnvelope.metadata().name(), is("progression.event.hearing-event-logs-document-created"));

    }


    @Test
    public void shouldProcessAaagCommand() throws Exception {
        final AaagHearingEventLogsDocumentCreated hearingEventLogsDocument = AaagHearingEventLogsDocumentCreated.aaagHearingEventLogsDocumentCreated().withApplicationId(randomUUID())
               .withCaseId(randomUUID()).build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.create-aaag-hearing-event-log-document")
                .withId(randomUUID())
                .build();

        final Envelope<AaagHearingEventLogsDocumentCreated> envelope = envelopeFrom(metadata, hearingEventLogsDocument);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);

        hearingEventLogCommandHandler.handleAaag(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope hearingResultedEnvelope = (JsonEnvelope) envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-event-logs-document-created")).findFirst().get();

        MatcherAssert.assertThat(hearingResultedEnvelope.metadata().name(), is("progression.event.hearing-event-logs-document-created"));

    }
}
