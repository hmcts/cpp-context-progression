package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefenceAccess;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.events.RepresentationType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1068", "squid:S1450"})
public class DefenceAssociationAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceAssociationAggregate.class);
    private static final long serialVersionUID = 7313423272698212459L;
    private UUID associatedOrganizationId;
    private final Set<DefenceAccess> defenceAccesses = new HashSet<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DefenceOrganisationAssociated.class).apply(e ->
                        this.associatedOrganizationId = e.getDefenceOrganisationId()
                ),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> associateOrganization(UUID defendantId, UUID organizationId,
                                                String representationType) {
        LOGGER.debug("A defence organization is associated to defendant");
        return apply(Stream.of(DefenceOrganisationAssociated.defenceOrganisationAssociated()
                .withDefenceOrganisationId(organizationId)
                .withDefendantId(defendantId)
                .withRepresentationType(RepresentationType.valueOf(representationType))
                .build()));
    }

}
