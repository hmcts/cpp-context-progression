package uk.gov.moj.cpp.defence.association.event.listener;

import static java.time.ZonedDateTime.parse;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationDefendant;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_LISTENER)
public class DefenceAssociationEventListener {

    @Inject
    private DefenceAssociationRepository repository;


    @Handles("progression.event.defence-organisation-associated")
    public void processOrganisationAssociated(final JsonEnvelope event) {

        final JsonObject payload = event.payloadAsJsonObject();
        final String defendantId = payload.getString("defendantId");
        final String userId = payload.getString("userId");
        final String defenceOrganisationId = payload.getString("organisationId");
        final String startDate = payload.getString("startDate");
        final String representationType = payload.getString("representationType");

        final DefenceAssociationDefendant defenceAssociationDefendant
                = prepareDefenceAssociationEntity(defendantId, userId, defenceOrganisationId, parse(startDate),representationType);
        repository.save(defenceAssociationDefendant);

    }

    @Handles("progression.event.defence-organisation-disassociated")
    public void processOrganisationDisassociated(final JsonEnvelope event) {

        final JsonObject payload = event.payloadAsJsonObject();
        final UUID defendantId = fromString(payload.getString("defendantId"));

        final UUID organisationId = fromString(payload.getString("organisationId"));

        final String endDate = payload.getString("endDate");

        final DefenceAssociationDefendant defenceAssociationDefendant = repository.findByDefendantId(defendantId);

        final DefenceAssociationDefendant updatedDefenceAssociationDefendant = disassociateOrganisation(defenceAssociationDefendant, organisationId, parse(endDate));

        repository.save(updatedDefenceAssociationDefendant);

    }

    private DefenceAssociationDefendant prepareDefenceAssociationEntity(final String defendantId,
                                                                        final String requesterUserId,
                                                                        final String defenceOrganisationId,
                                                                        final ZonedDateTime startDate,
                                                                        final String representationType) {

        final DefenceAssociationDefendant defenceAssociationDefendant = new DefenceAssociationDefendant();
        defenceAssociationDefendant.setDefendantId(fromString(defendantId));

        final DefenceAssociation defenceAssociation = new DefenceAssociation();
        defenceAssociation.setId(randomUUID());
        defenceAssociation.setUserId(fromString(requesterUserId));
        defenceAssociation.setOrgId(fromString(defenceOrganisationId));
        defenceAssociation.setStartDate(startDate);
        defenceAssociation.setRepresentationType(representationType);
        defenceAssociation.setDefenceAssociationDefendant(defenceAssociationDefendant);
        defenceAssociationDefendant.getDefenceAssociations().add(defenceAssociation);
        return defenceAssociationDefendant;
    }

    private DefenceAssociationDefendant disassociateOrganisation(final DefenceAssociationDefendant defenceAssociationDefendant, final UUID organisationId, final ZonedDateTime endDate) {

        final Set<DefenceAssociation> defenceAssociations = defenceAssociationDefendant
                .getDefenceAssociations();

        final DefenceAssociation currentDefenceAssociation = getCurrentDefenceAssociation(organisationId, defenceAssociations);
        currentDefenceAssociation.setEndDate(endDate);
        defenceAssociations.add(currentDefenceAssociation);
        defenceAssociationDefendant.setDefenceAssociations(defenceAssociations);

        return defenceAssociationDefendant;
    }

    private DefenceAssociation getCurrentDefenceAssociation(final UUID organisationId, final Set<DefenceAssociation> defenceAssociations) {
        final List<DefenceAssociation> currentDefenceAssociations = defenceAssociations
                .stream()
                .filter(d -> d.getEndDate() == null)
                .collect(toList());

        if (currentDefenceAssociations.isEmpty() || currentDefenceAssociations.size() > 1) {
            throw new IllegalStateException("No association found or More than one current association");
        }

        final DefenceAssociation currentDefenceAssociation = currentDefenceAssociations.get(0);

        if (!currentDefenceAssociation.getOrgId().toString().equals(organisationId.toString())) {
            throw new IllegalArgumentException("Mismatched Organisation Ids");
        }

        return currentDefenceAssociation;
    }

}
