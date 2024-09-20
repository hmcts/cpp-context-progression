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

import uk.gov.justice.progression.courts.CaseLinkType;
import uk.gov.justice.progression.courts.CasesToLink;
import uk.gov.justice.progression.courts.LinkCases;
import uk.gov.moj.cpp.progression.events.ValidateLinkCases;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
public class LinkSplitMergeCasesHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            LinkCases.class, ValidateLinkCases.class, uk.gov.moj.cpp.progression.events.LinkCases.class);

    @InjectMocks
    private LinkSplitMergeCasesHandler linkCasesHandler;


    private CaseAggregate caseAggregate;

    @BeforeEach
    public void setup() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldHandleLinkCommand() {
        assertThat(linkCasesHandler, isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.link-cases")
                ));
    }

    @Test
    public void shouldHandleValidateLinkCommand() {
        assertThat(linkCasesHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleValidation")
                        .thatHandles("progression.command.validate-link-cases")
                ));
    }

    @Test
    public void shouldHandleValidation() throws EventStreamException {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        final List<CasesToLink> casesToLink = new ArrayList<>();
        casesToLink.add(CasesToLink.casesToLink()
                .withCaseLinkType(CaseLinkType.LINK)
                .build());
        final uk.gov.justice.progression.courts.ValidateLinkCases validateLinkCases = uk.gov.justice.progression.courts.ValidateLinkCases.validateLinkCases()
                .withCasesToLink(casesToLink)
                .withProsecutionCaseId(UUID.randomUUID())
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.edit-case-note")
                .withId(randomUUID())
                .build();

        final Envelope<uk.gov.justice.progression.courts.ValidateLinkCases> envelope = envelopeFrom(metadata, validateLinkCases);
        linkCasesHandler.handleValidation(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  resultEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.validate-link-cases")).findFirst().get();

        MatcherAssert.assertThat(resultEnvelope.payloadAsJsonObject()
                , notNullValue());
    }

    @Test
    public void shouldHandle() throws EventStreamException {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        final List<CasesToLink> casesToLink = new ArrayList<>();
        casesToLink.add(CasesToLink.casesToLink()
                .withCaseLinkType(CaseLinkType.LINK)
                .build());
        final LinkCases linkCases = LinkCases.linkCases()
                .withCasesToLink(casesToLink)
                .withProsecutionCaseId(randomUUID())
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.link-cases")
                .withId(randomUUID())
                .build();

        final Envelope<LinkCases> envelope = envelopeFrom(metadata, linkCases);
        linkCasesHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  resultEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.link-cases")).findFirst().get();

        MatcherAssert.assertThat(resultEnvelope.payloadAsJsonObject()
                , notNullValue());
    }
}
