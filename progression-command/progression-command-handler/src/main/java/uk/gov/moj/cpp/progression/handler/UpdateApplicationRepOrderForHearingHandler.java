package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.fromString;

import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
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

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateApplicationRepOrderForHearingHandler {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UpdateApplicationRepOrderForHearingHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "hearingId";
    private static final String SUBJECT_ID = "subjectId";
    private static final String ASSOCIATED_DEFENCE_ORGANISATION = "associatedDefenceOrganisation";

    @Handles("progression.command.handler.update-application-rep-order-for-hearing")
    public void handle(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.handler.update-application-rep-order-for-hearing {}", envelope.payload());
        }
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final UUID applicationId = fromString(payload.getString(APPLICATION_ID));
        final UUID subjectId = fromString(payload.getString(SUBJECT_ID));
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = payload.containsKey(ASSOCIATED_DEFENCE_ORGANISATION) ? jsonObjectToObjectConverter.convert(payload.getJsonObject(ASSOCIATED_DEFENCE_ORGANISATION), AssociatedDefenceOrganisation.class) : null;

        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateApplicationRepOrder(hearingId, applicationId, subjectId, associatedDefenceOrganisation);
        appendEventsToStream(envelope, eventStream, events);
    }

    @SuppressWarnings("java:S1874")
    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
