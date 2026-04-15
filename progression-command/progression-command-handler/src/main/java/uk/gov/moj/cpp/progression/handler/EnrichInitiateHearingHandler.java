package uk.gov.moj.cpp.progression.handler;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.CommandEnrichHearingInitiate;
import uk.gov.justice.core.courts.ProsecutionCase;
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
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S3655")
@ServiceComponent(Component.COMMAND_HANDLER)
public class EnrichInitiateHearingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichInitiateHearingHandler.class.getName());
    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;
    @Inject
    private Enveloper enveloper;

    @Handles("progression.command-enrich-hearing-initiate")
    public void enrichHearingInitiate(final Envelope<CommandEnrichHearingInitiate> initiateEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command-enrich-hearing-initiate {}", initiateEnvelope.payload());
        final CommandEnrichHearingInitiate initiate = initiateEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(initiate.getHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.enrichInitiateHearing(initiate.getHearing());
        if (events != null) {
            appendEventsToStream(initiateEnvelope.metadata(), eventStream, events);
        }

        addHearingToCase(initiate, initiateEnvelope.metadata());
    }

    private void addHearingToCase(final CommandEnrichHearingInitiate initiate, final Metadata metadata) throws EventStreamException {
        final List<ProsecutionCase> prosecutionCases = ofNullable(initiate.getHearing().getProsecutionCases()).orElse(Collections.emptyList());

       for(ProsecutionCase pc : prosecutionCases) {
            final EventStream caseEventStream = eventSource.getStreamById(pc.getId());
            final CaseAggregate caseAggregate = aggregateService.get(caseEventStream, CaseAggregate.class);

            final Stream<Object> events = caseAggregate.enrichHearingInitiate(initiate.getHearing());
            if (events != null) {
                appendEventsToStream(metadata, caseEventStream, events);
            }
        }
    }

    private void appendEventsToStream(final Metadata metadata, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadata, JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
