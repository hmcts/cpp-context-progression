package uk.gov.moj.cpp.defence.association.event.listener;

import static java.time.ZonedDateTime.parse;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationDefendant;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.persistence.NoResultException;

@ServiceComponent(EVENT_LISTENER)
public class DefenceAssociationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceAssociationEventListener.class);

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
        final String laaContractNumber = payload.getString("laaContractNumber" , null);

        final DefenceAssociationDefendant defenceAssociationDefendant
                = prepareDefenceAssociationEntity(defendantId, userId, defenceOrganisationId, parse(startDate),representationType, laaContractNumber);
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
                                                                        final String representationType,
                                                                        final String laaContractNumber) {
        final UUID defendantUUID = fromString(defendantId);
        DefenceAssociationDefendant defenceAssociationDefendant = findDefenceAssociationByDefendantId(defendantUUID);
        if(defenceAssociationDefendant == null) {
            defenceAssociationDefendant = new DefenceAssociationDefendant();
            defenceAssociationDefendant.setDefendantId(defendantUUID);
        }
        final DefenceAssociation defenceAssociation = new DefenceAssociation();
        defenceAssociation.setId(randomUUID());
        defenceAssociation.setUserId(fromString(requesterUserId));
        defenceAssociation.setOrgId(fromString(defenceOrganisationId));
        defenceAssociation.setStartDate(startDate);
        defenceAssociation.setRepresentationType(representationType);
        defenceAssociation.setDefenceAssociationDefendant(defenceAssociationDefendant);
        defenceAssociation.setLaaContractNumber(laaContractNumber);
        defenceAssociationDefendant.getDefenceAssociations().add(defenceAssociation);
        return defenceAssociationDefendant;
    }

    private DefenceAssociationDefendant findDefenceAssociationByDefendantId(final UUID defendantId) {
        try {
            return repository.findByDefendantId(defendantId);
        } catch (final NoResultException e) {
            LOGGER.info("No DefenceAssociation found with defendantId='{}'", defendantId, e);
        }
        return null;
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

        final Optional<DefenceAssociation> optionalCurrentDefenceAssociation = currentDefenceAssociations.stream()
                .filter(cda-> cda.getOrgId().equals(organisationId))
                .findAny();

        if (!optionalCurrentDefenceAssociation.isPresent()) {
            throw new IllegalArgumentException("Mismatched Organisation Ids");
        }

        return optionalCurrentDefenceAssociation.get();
    }

}
