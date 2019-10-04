package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.fromString;

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
import uk.gov.moj.cpp.progression.command.DisassociateDefenceOrganisation;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.OrganisationDetails;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserGroupDetails;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

@ServiceComponent(Component.COMMAND_HANDLER)
public class DisassociateDefenceOrganisationHandler {

    private static final List<String> allowedGroupsForDisassociation = Arrays.asList("Court Clerks", "Court Administrators", "Crown Court Admin", "Listing Officers", "Legal Advisers");

    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;
    @Inject
    private Enveloper enveloper;
    @Inject
    private UsersGroupService usersGroupService;

    @Handles("progression.command.handler.disassociate-defence-organisation")
    public void handle(final Envelope<DisassociateDefenceOrganisation> envelope) throws EventStreamException {
        final DisassociateDefenceOrganisation disassociateDefenceOrganisation = envelope.payload();

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalArgumentException("No UserId Supplied"));

        final OrganisationDetails userOrgDetails = usersGroupService.getUserOrgDetails(envelope);
        validateDisassociationCommand(disassociateDefenceOrganisation.getOrganisationId(),
                userOrgDetails.getId(), envelope);

        final EventStream eventStream = eventSource.getStreamById(disassociateDefenceOrganisation.getDefendantId());
        final DefenceAssociationAggregate defenceAssociationAggregate = aggregateService.get(eventStream, DefenceAssociationAggregate.class);
        final Stream<Object> events =
                defenceAssociationAggregate.disassociateOrganisation(disassociateDefenceOrganisation.getDefendantId(),
                        disassociateDefenceOrganisation.getOrganisationId(), fromString(userId));
        appendEventsToStream(envelope, eventStream, events);
    }

    private void validateDisassociationCommand(final UUID organisationId,
                                               final UUID userOrganisationId,
                                               final Envelope<DisassociateDefenceOrganisation> envelope) {

        if (!organisationId.equals(userOrganisationId)
                && !isHMCTSUser(envelope)) {
            throw new IllegalArgumentException("The given Organisation is not qualified to perform this disassociation");
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope,
                                      final EventStream eventStream,
                                      final Stream<Object> events) throws EventStreamException {

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private boolean isHMCTSUser(final Envelope<DisassociateDefenceOrganisation> envelope) {
        final List<UserGroupDetails> retrievedUserGroupDetails = usersGroupService.getUserGroupsForUser(envelope);
        return retrievedUserGroupDetails.stream().anyMatch(userGroupDetails ->
                allowedGroupsForDisassociation.contains(userGroupDetails.getGroupName())
        );
    }
}
