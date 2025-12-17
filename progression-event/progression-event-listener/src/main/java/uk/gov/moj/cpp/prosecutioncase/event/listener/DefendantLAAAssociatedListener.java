package uk.gov.moj.cpp.prosecutioncase.event.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAAssociationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantLAAAssociationRepository;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.persistence.NoResultException;
import java.util.UUID;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class DefendantLAAAssociatedListener {

    private static final String LAA_CONTRACT_NUMBER = "laaContractNumber";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String IS_ASSOCIATED_BY_LAA = "isAssociatedByLAA";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantLAAAssociatedListener.class);


    @Inject
    private DefendantLAAAssociationRepository defendantLAAAssociationRepository;

    @Handles("progression.event.defendant-laa-associated")
    public void defendantLAAAssociated(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event  progression.event.defendant-laa-associated {} ", event.toObfuscatedDebugString());
        }
        final JsonObject  payload = event.payloadAsJsonObject();
        final UUID defendantId = fromString(payload.getString(DEFENDANT_ID));
        final String laaContractNumber = payload.getString(LAA_CONTRACT_NUMBER);
        final boolean isAssociatedByLAA = payload.getBoolean(IS_ASSOCIATED_BY_LAA);
        final DefendantLAAKey defendantLAAKey = new DefendantLAAKey(defendantId, laaContractNumber);
        DefendantLAAAssociationEntity defendantLAAAssociationEntity = findDefenceAssociationByDefendantId(defendantLAAKey);
        if(defendantLAAAssociationEntity == null) {
            defendantLAAAssociationEntity = new DefendantLAAAssociationEntity();
            defendantLAAAssociationEntity.setDefendantLAAKey(defendantLAAKey);
        }
        defendantLAAAssociationEntity.setAssociatedByLAA(isAssociatedByLAA);
        defendantLAAAssociationRepository.save(defendantLAAAssociationEntity);
    }

    private DefendantLAAAssociationEntity findDefenceAssociationByDefendantId(final DefendantLAAKey defendantLAAKey) {
        try {
            return defendantLAAAssociationRepository.findBy(defendantLAAKey);
        } catch (final NoResultException e) {
            LOGGER.info("No Defendant LAA Association found with defendantId='{}'", defendantLAAKey.getDefendantId(), e);
        }
        return null;
    }

}
