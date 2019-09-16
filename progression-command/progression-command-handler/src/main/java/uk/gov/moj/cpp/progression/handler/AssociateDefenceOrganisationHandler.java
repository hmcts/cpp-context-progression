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
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupQueryService;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AssociateDefenceOrganisationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssociateDefenceOrganisationHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private UsersGroupQueryService usersGroupQueryService;


    @Handles("progression.command.handler.associate-defence-organisation")
    public void handle(final Envelope<AssociateDefenceOrganisation> associateDefenceOrganisationEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.handler.associate-defence-organisation {}", associateDefenceOrganisationEnvelope);
        final String organisationNameFromUsersAndGroups = getOrganisationNameForThisUser(associateDefenceOrganisationEnvelope);

        final AssociateDefenceOrganisation associateDefenceOrganisation = associateDefenceOrganisationEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(associateDefenceOrganisation.getDefendantId());
        final DefenceAssociationAggregate defenceAssociationAggregate = aggregateService.get(eventStream, DefenceAssociationAggregate.class);
        final Stream<Object> events =
                defenceAssociationAggregate.associateOrganization(associateDefenceOrganisation.getDefendantId(),
                        associateDefenceOrganisation.getOrganisationId(),
                        organisationNameFromUsersAndGroups,
                        associateDefenceOrganisation.getRepresentationType().toString());
        if (events != null) {
            appendEventsToStream(associateDefenceOrganisationEnvelope, eventStream, events);
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private String getOrganisationNameForThisUser(
            final Envelope<AssociateDefenceOrganisation> associateDefenceOrganisationEnvelope) {

        final JsonEnvelope userGroupsRequestEnvelope =
                JsonEnvelope.envelopeFrom(associateDefenceOrganisationEnvelope.metadata(), JsonValue.NULL);

        final JsonObject userGroupsResponse =
                usersGroupQueryService.getOrganisationDetailsForUser(userGroupsRequestEnvelope);

        final String usersAndGroupsOrganisationId =
                userGroupsResponse.getString("organisationId");
        if (!UUID.fromString(usersAndGroupsOrganisationId)
                .equals(associateDefenceOrganisationEnvelope.payload().getOrganisationId())) {
            LOGGER.error("The Given Organisation ID is not relevant to the user");
            throw new IllegalArgumentException("The given Organisation ID does not belong to this particular user");
        }
        return userGroupsResponse.getString("organisationName");
    }

}
