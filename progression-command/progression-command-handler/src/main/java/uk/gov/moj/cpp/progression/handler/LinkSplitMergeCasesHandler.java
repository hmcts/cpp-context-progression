package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.progression.courts.LinkCases;
import uk.gov.justice.progression.courts.ValidateLinkCases;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.domain.CasesToLink;
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class LinkSplitMergeCasesHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LinkSplitMergeCasesHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    //below handler is for the validation of link cases request
    @Handles("progression.command.validate-link-cases")
    public void handleValidation(final Envelope<ValidateLinkCases> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.validate-link-cases payload: {}", envelope.payload());
        }
        final ValidateLinkCases validateLinkCases = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(validateLinkCases.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.validateLinkSplitOrMergeStreams(transformCasesToLink(validateLinkCases));
        appendEventsToStream(envelope, eventStream, events);
    }

    //below handler is for processing the link requests
    @Handles("progression.command.link-cases")
    public void handle(final Envelope<LinkCases> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.link-cases payload: {}", envelope.payload());
        }
        final LinkCases linkCases = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(linkCases.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.processLinkSplitOrMergeStreams(transformCasesToLink(linkCases));
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private List<CasesToLink> transformCasesToLink(final ValidateLinkCases validateLinkCases) {

        return extractCasesToLink(validateLinkCases.getCasesToLink().stream(), validateLinkCases.getProsecutionCaseId());
    }

    private List<CasesToLink> transformCasesToLink(final LinkCases linkCases) {

        return extractCasesToLink(linkCases.getCasesToLink().stream(), linkCases.getProsecutionCaseId());
    }

    private List<CasesToLink> extractCasesToLink(final Stream<uk.gov.justice.progression.courts.CasesToLink> stream, final UUID prosecutionCaseId) {
        return stream
                .map(caseToLink ->
                        CasesToLink.casesToLink().withLinkType(transformLinkType(caseToLink.getCaseLinkType()))
                                .withProsecutionCaseId(prosecutionCaseId)
                                .withCaseUrns(caseToLink.getCaseUrns())
                                .build()).collect(Collectors.toList());
    }

    private LinkType transformLinkType(final uk.gov.justice.progression.courts.CaseLinkType linkType) {
        if (linkType.equals(uk.gov.justice.progression.courts.CaseLinkType.LINK)) {
            return LinkType.LINK;
        } else if (linkType.equals(uk.gov.justice.progression.courts.CaseLinkType.SPLIT)) {
            return LinkType.SPLIT;
        } else if (linkType.equals(uk.gov.justice.progression.courts.CaseLinkType.MERGE)) {
            return LinkType.MERGE;
        } else {
            throw new UnsupportedOperationException("Unsupported link type.");
        }
    }

}
