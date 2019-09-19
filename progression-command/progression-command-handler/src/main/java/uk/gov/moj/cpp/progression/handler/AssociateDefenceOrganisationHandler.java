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
import uk.gov.moj.cpp.progression.command.AssociateDefenceOrganisation;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.OrganisationDetails;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AssociateDefenceOrganisationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssociateDefenceOrganisationHandler.class.getName());
    public static final String ORGANISATION_NAME = "organisationName";
    public static final String ORGANISATION_ID = "organisationId";

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
        LOGGER.debug("progression.command.handler.associate-defence-organisation {}", envelope);
        final AssociateDefenceOrganisation associateDefenceOrganisation = envelope.payload();

        //validate that the user that requested the association belongs to the organisation
        final OrganisationDetails userOrgDetails = usersGroupService.getUserOrgDetails(envelope);
        validateAssociationCommand(associateDefenceOrganisation.getOrganisationId(), userOrgDetails.getId());

        final EventStream eventStream = eventSource.getStreamById(associateDefenceOrganisation.getDefendantId());
        final DefenceAssociationAggregate defenceAssociationAggregate = aggregateService.get(eventStream, DefenceAssociationAggregate.class);
        final Stream<Object> events =
                defenceAssociationAggregate.associateOrganization(associateDefenceOrganisation.getDefendantId(),
                        associateDefenceOrganisation.getOrganisationId(),
                        userOrgDetails.getName(),
                        associateDefenceOrganisation.getRepresentationType().toString());
        if (events != null) {
            appendEventsToStream(envelope, eventStream, events);
        }
    }

    private void validateAssociationCommand(final UUID organisationId, final UUID userOrganisationId) {
        if (!organisationId.equals(userOrganisationId)) {
            LOGGER.error("The user does not belong to the requested organisation");
            throw new IllegalArgumentException("The user does not belong to the requested organisation");
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
