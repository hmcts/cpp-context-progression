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
public class AssociateDefenceOrganisationForLAAHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("progression.command.handler.associate-defence-organisation-for-laa")
    public void handle(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalArgumentException("No UserId Supplied"));
        final UUID organisationId = fromString(payload.getString("organisationId"));
        final UUID defendantId = fromString(payload.getString("defendantId"));
        final String organisationName = payload.getString("organisationName");
        final String representationType = payload.getString("representationType");
        final String laaContractNumber = payload.getString("laaContractNumber");

        final EventStream eventStream = eventSource.getStreamById(defendantId);
        final DefenceAssociationAggregate defenceAssociationAggregate = aggregateService.get(eventStream, DefenceAssociationAggregate.class);
        final Stream<Object> events =
                defenceAssociationAggregate.associateOrganisationForRepOrder(defendantId,
                        organisationId,
                        organisationName,
                        representationType,
                        laaContractNumber,
                        userId);
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
