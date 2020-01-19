package uk.gov.moj.cpp.progression.handler;

import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.stream.Stream;

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
import uk.gov.moj.cpp.progression.command.AssociateDefenceOrganisation;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.OrganisationDetails;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AssociateDefenceOrganisationHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private UsersGroupService usersGroupService;


    @Handles("progression.command.handler.associate-defence-organisation")
    public void handle(final Envelope<AssociateDefenceOrganisation> envelope) throws EventStreamException {
        final AssociateDefenceOrganisation associateDefenceOrganisation = envelope.payload();

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalArgumentException("No UserId Supplied"));

        final OrganisationDetails userOrgDetails = usersGroupService.getUserOrgDetails(envelope);

        final EventStream eventStream = eventSource.getStreamById(associateDefenceOrganisation.getDefendantId());
        final DefenceAssociationAggregate defenceAssociationAggregate = aggregateService.get(eventStream, DefenceAssociationAggregate.class);
        final Stream<Object> events =
                defenceAssociationAggregate.associateOrganisation(associateDefenceOrganisation.getDefendantId(),
                        userOrgDetails.getId(),
                        userOrgDetails.getName(),
                        associateDefenceOrganisation.getRepresentationType().toString(),
                        associateDefenceOrganisation.getLaaContractNumber(),
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
