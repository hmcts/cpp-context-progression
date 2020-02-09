package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.cpp.progression.events.DefendantDefenceAssociationLockedForLaa;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationDisassociated;
import uk.gov.moj.cpp.progression.events.DefendantLaaAssociated;
import uk.gov.moj.cpp.progression.events.RepresentationType;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1068", "squid:S1450"})
public class DefenceAssociationAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceAssociationAggregate.class);
    private static final long serialVersionUID = 7313423272698212460L;
    private static final String UTC = "UTC";
    private static final ZoneId UTC_ZONE_ID = ZoneId.of(UTC);
    private UUID associatedOrganisationId;
    private List<UUID> disassociatedOrganisationIds;
    private Map<UUID,Boolean> defendantAssociationLockMap = new HashMap<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DefenceOrganisationAssociated.class).apply(e ->
                        this.setAssociatedOrganisationId(e.getOrganisationId())
                ),
                when(DefenceOrganisationDisassociated.class).apply(e ->
                        {
                            populatedDisassociatedOrganisationIds(e);
                            this.setAssociatedOrganisationId(null);
                        }
                ),
                when(DefendantDefenceAssociationLockedForLaa.class).apply(e->
                        defendantAssociationLockMap.put(e.getDefendantId(), e.getLockedByRepOrder())
                ),
                otherwiseDoNothing()
        );
    }

    private void populatedDisassociatedOrganisationIds(final DefenceOrganisationDisassociated e) {
        if (disassociatedOrganisationIds == null) {
            disassociatedOrganisationIds = new ArrayList<>();
        }
        this.disassociatedOrganisationIds.add(e.getOrganisationId());
    }

    public Stream<Object> associateOrganisation(final UUID defendantId, final UUID organisationId,
                                                final String organisationName,
                                                final String representationType,
                                                final String laaContractNumber,
                                                final String userId) {
        LOGGER.debug("A defence organisation is associated to defendant");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        boolean lockFlag = false;
        if(defendantAssociationLockMap.containsKey(defendantId)) {
            lockFlag = defendantAssociationLockMap.get(defendantId);
        }
        if(organisationId != null && !organisationId.equals(associatedOrganisationId) && !lockFlag) {
            streamBuilder.add(DefenceOrganisationAssociated.defenceOrganisationAssociated()
                    .withOrganisationId(organisationId)
                    .withOrganisationName(organisationName)
                    .withDefendantId(defendantId)
                    .withStartDate(ZonedDateTime.now(UTC_ZONE_ID))
                    .withRepresentationType(RepresentationType.valueOf(representationType))
                    .withLaaContractNumber(laaContractNumber)
                    .withUserId(UUID.fromString(userId))
                    .build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> disassociateOrganisation(final UUID defendantId, final UUID organisationId, final UUID caseId, final UUID userId) {
        LOGGER.debug("A defence organisation is disassociated to defendant");
        return apply(Stream.of(DefenceOrganisationDisassociated.defenceOrganisationDisassociated()
                .withOrganisationId(organisationId)
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withUserId(userId)
                .withEndDate(ZonedDateTime.now(UTC_ZONE_ID))
                .build()));
    }

    public Stream<Object> associateOrganisationForRepOrder(final UUID defendantId, final UUID organisationId,
                                                           final String organisationName,
                                                           final String representationType,
                                                           final String laaContractNumber,
                                                           final String userId) {
        LOGGER.debug("A defence organisation is associated to defendant by Representation Order");
        final Stream.Builder<Object> streamBuilder = Stream.builder();

            streamBuilder.add(DefenceOrganisationAssociated.defenceOrganisationAssociated()
                    .withOrganisationId(organisationId)
                    .withOrganisationName(organisationName)
                    .withDefendantId(defendantId)
                    .withStartDate(ZonedDateTime.now(UTC_ZONE_ID))
                    .withRepresentationType(RepresentationType.valueOf(representationType))
                    .withLaaContractNumber(laaContractNumber)
                    .withUserId(UUID.fromString(userId))
                    .build());
        return apply(streamBuilder.build());

    }

    public Stream<Object> handleOrphanedDefendantAssociation(final UUID organisationId, final String organisationName,
                                                             final UUID defendantId, final String laaContractNumber, final String userId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        //Raise event to set the is Associated flag to true for orphaned Defendant as it is associated now
        streamBuilder.add(DefendantLaaAssociated.defendantLaaAssociated()
                .withDefendantId(defendantId)
                .withLaaContractNumber(laaContractNumber)
                .withIsAssociatedByLAA(true)
                .build());
        streamBuilder.add(DefenceOrganisationAssociated.defenceOrganisationAssociated()
                .withOrganisationId(organisationId)
                .withOrganisationName(organisationName)
                .withDefendantId(defendantId)
                .withStartDate(ZonedDateTime.now(UTC_ZONE_ID))
                .withRepresentationType(RepresentationType.REPRESENTATION_ORDER)
                .withLaaContractNumber(laaContractNumber)
                .withUserId(UUID.fromString(userId))
                .build());
        return apply(streamBuilder.build());
    }

    public Stream<Object> handleDefendantDefenceAssociationLocked(final UUID prosecutionCaseId, final UUID defendantId, final boolean lockedByRepOrder) {
        LOGGER.debug("Applying lock on defendant for association so that it can't be associated by any other defence organisation {}", defendantId);
        return apply(Stream.of(DefendantDefenceAssociationLockedForLaa.defendantDefenceAssociationLockedForLaa()
                 .withDefendantId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withLockedByRepOrder(lockedByRepOrder)
                .build()));
    }

    public UUID getAssociatedOrganisationId() {
        return associatedOrganisationId;
    }

    public void setAssociatedOrganisationId(final UUID associatedOrganisationId) {
        this.associatedOrganisationId = associatedOrganisationId;
    }
}
