package uk.gov.moj.cpp.progression.handler;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ProcessHearingExtended;
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
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.stream.Stream;

@SuppressWarnings({"squid:S00112", "squid:S2629"})
@ServiceComponent(Component.COMMAND_HANDLER)
public class ExtendHearingHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ExtendHearingHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.extend-hearing")
    public void handle(final Envelope<ExtendHearing> extendHearingEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.extend-hearing {}", extendHearingEnvelope.payload());

        final ExtendHearing extendHearing = extendHearingEnvelope.payload();
        final HearingListingNeeds hearingRequest = extendHearing.getHearingRequest();
        final EventStream eventStream = eventSource.getStreamById(hearingRequest.getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events;
        if (isNotEmpty(hearingRequest.getProsecutionCases())) {
            events = hearingAggregate.extendHearing(hearingRequest, extendHearing);
        } else {
            events = hearingAggregate.extendHearing(hearingRequest);
        }
        appendEventsToStream(extendHearingEnvelope, eventStream, events);
    }

    @Handles("progression.command.process-hearing-extended")
    public void handleProcessHearingExtended(final Envelope<ProcessHearingExtended> processHearingExtendedEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.process-hearing-extended {}", processHearingExtendedEnvelope.payload());

        final ProcessHearingExtended processHearingExtended = processHearingExtendedEnvelope.payload();

        final EventStream eventStream = eventSource.getStreamById(processHearingExtended.getHearingRequest().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> event = hearingAggregate.processHearingExtended(processHearingExtended.getHearingRequest(), processHearingExtended.getShadowListedOffences());
        appendEventsToStream(processHearingExtendedEnvelope, eventStream, event);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
