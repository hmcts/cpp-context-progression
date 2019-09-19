package uk.gov.moj.cpp.defence.asscociation.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationHistory;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class DefenceAssociationAccessEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceAssociationAccessEventListener.class);

    @Inject
    private DefenceAssociationRepository repository;


    @Handles("progression.event.defence-organisation-associated")
    public void processOrganisationAssociated(final JsonEnvelope event) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(event.payloadAsJsonString().getString());
        }

        final String defendantId = event.payloadAsJsonObject().getString("defendantId");
        final String requesterUserId = event.metadata().userId().get();
        final String defenceOrganisationId = event.payloadAsJsonObject().getString("organisationId");

        final DefenceAssociation defenceAssociation = prepareDefenceAssociationEntity(defendantId, requesterUserId, defenceOrganisationId);
        repository.save(defenceAssociation);

    }

    private DefenceAssociation prepareDefenceAssociationEntity(final String defendantId, final String requesterUserId, final String defenceOrganisationId) {

        final DefenceAssociation defenceAssociation = new DefenceAssociation();
        defenceAssociation.setDefendantId(UUID.fromString(defendantId));

        final DefenceAssociationHistory defenceAssociationHistory = new DefenceAssociationHistory();
        defenceAssociationHistory.setId(UUID.randomUUID());
        defenceAssociationHistory.setGrantorUserId(UUID.fromString(requesterUserId));
        defenceAssociationHistory.setGrantorOrgId(UUID.fromString(defenceOrganisationId));
        defenceAssociationHistory.setStartDate(ZonedDateTime.now());
        defenceAssociationHistory.setDefenceAssociation(defenceAssociation);
        defenceAssociation.getDefenceAssociationHistories().add(defenceAssociationHistory);
        return defenceAssociation;
    }

}
