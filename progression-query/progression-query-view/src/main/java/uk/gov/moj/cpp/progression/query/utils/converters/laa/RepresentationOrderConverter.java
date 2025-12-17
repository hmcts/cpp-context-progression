package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.progression.query.laa.RepresentationOrder;
@SuppressWarnings("squid:S1168")
public class RepresentationOrderConverter extends LAAConverter {

    public RepresentationOrder convert(final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
        if (isNull(associatedDefenceOrganisation)) {
            return null;
        }

        return RepresentationOrder.representationOrder()
                .withApplicationReference(associatedDefenceOrganisation.getApplicationReference())
                .withEffectiveFromDate(ofNullable(associatedDefenceOrganisation.getAssociationStartDate()).map(Object::toString).orElse(null))
                .withLaaContractNumber(ofNullable(associatedDefenceOrganisation.getDefenceOrganisation()).map(DefenceOrganisation::getLaaContractNumber).orElse(null))
                .withEffectiveToDate(ofNullable(associatedDefenceOrganisation.getAssociationEndDate()).map(Object::toString).orElse(null))
                .build();

    }
}