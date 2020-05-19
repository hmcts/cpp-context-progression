package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.progression.courts.UnlinkCases;
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
import uk.gov.moj.cpp.progression.domain.CaseToUnlink;

import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UnlinkCasesHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UnlinkCasesHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.unlink-cases")
    public void handle(final Envelope<UnlinkCases> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.unlink-cases payload: {}", envelope.payload());
        }
        final UnlinkCases unlinkCases = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(unlinkCases.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.unlinkCases(unlinkCases.getProsecutionCaseId(), unlinkCases.getProsecutionCaseUrn(), transform(unlinkCases));
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private List<CaseToUnlink> transform(final UnlinkCases unlinkCases) {

        return unlinkCases.getCases().stream()
                .map(caseToUnlink ->
                        CaseToUnlink.caseToUnlink().withCaseId(caseToUnlink.getCaseId())
                                .withCaseUrn(caseToUnlink.getCaseUrn())
                                .withLinkGroupId(caseToUnlink.getLinkGroupId())
                                .build()).collect(Collectors.toList());
    }


}
