package uk.gov.moj.cpp.progression.handler;

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
import uk.gov.moj.cpp.progression.aggregate.DefenceAssociationAggregate;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.UUID.fromString;

@ServiceComponent(Component.COMMAND_HANDLER)
public class LockDefenceAssociationForLAAHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    private static final String DEFENDANT_ID = "defendantId";
    private static final String LOCKED_BY_REP_ORDER = "lockedByRepOrder";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";


    @Handles("progression.command.handler.lock-defence-association-for-laa")
    public void handle(final JsonEnvelope envelope) throws EventStreamException {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID prosecutionCaseId = fromString(payload.getString(PROSECUTION_CASE_ID));
        final UUID defendantId = fromString(payload.getString(DEFENDANT_ID));
        final boolean lockedByRepOrder = payload.getBoolean(LOCKED_BY_REP_ORDER);

        final EventStream eventStream = eventSource.getStreamById(defendantId);
        final DefenceAssociationAggregate defenceAssociationAggregate = aggregateService.get(eventStream, DefenceAssociationAggregate.class);

        final Stream<Object> events =
                defenceAssociationAggregate.handleDefendantDefenceAssociationLocked(prosecutionCaseId,defendantId, lockedByRepOrder);
        if (events != null) {
            appendEventsToStream(envelope, eventStream, events);
        }

    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
