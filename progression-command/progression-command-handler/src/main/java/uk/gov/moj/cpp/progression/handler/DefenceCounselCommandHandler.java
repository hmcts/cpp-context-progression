package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;


import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.progression.command.AddHearingDefenceCounsel;
import uk.gov.moj.cpp.progression.command.RemoveHearingDefenceCounsel;
import uk.gov.moj.cpp.progression.command.UpdateHearingDefenceCounsel;

@ServiceComponent(Component.COMMAND_HANDLER)
public class DefenceCounselCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceCounselCommandHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("progression.command.handler.add-hearing-defence-counsel")
    public void handleAddHearingDefenceCounsel(final Envelope<AddHearingDefenceCounsel> addHearingDefenceCounselEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.handler.add-hearing-defence-counsel {}", addHearingDefenceCounselEnvelope.payload());

        final AddHearingDefenceCounsel addHearingDefenceCounsel = addHearingDefenceCounselEnvelope.payload();

        final EventStream eventStream = eventSource.getStreamById(addHearingDefenceCounsel.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.addDefenceCounselToHearing(addHearingDefenceCounsel.getDefenceCounsel());
        appendEventsToStream(addHearingDefenceCounselEnvelope, eventStream, events);
    }

    @Handles("progression.command.handler.update-hearing-defence-counsel")
    public void handleUpdateHearingDefenceCounsel(final Envelope<UpdateHearingDefenceCounsel> updateHearingDefenceCounselEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.handler.update-hearing-defence-counsel {}", updateHearingDefenceCounselEnvelope.payload());

        final UpdateHearingDefenceCounsel updateHearingDefenceCounsel = updateHearingDefenceCounselEnvelope.payload();

        final EventStream eventStream = eventSource.getStreamById(updateHearingDefenceCounsel.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateHearingWithDefenceCounsel(updateHearingDefenceCounsel.getDefenceCounsel());
        appendEventsToStream(updateHearingDefenceCounselEnvelope, eventStream, events);
    }

    @Handles("progression.command.handler.remove-hearing-defence-counsel")
    public void handleRemoveHearingDefenceCounsel(final Envelope<RemoveHearingDefenceCounsel> removeHearingDefenceCounselEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.handler.remove-hearing-defence-counsel {}", removeHearingDefenceCounselEnvelope.payload());

        final RemoveHearingDefenceCounsel removeHearingDefenceCounsel = removeHearingDefenceCounselEnvelope.payload();

        final EventStream eventStream = eventSource.getStreamById(removeHearingDefenceCounsel.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.removeDefenceCounselFromHearing(removeHearingDefenceCounsel.getId());
        appendEventsToStream(removeHearingDefenceCounselEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
