package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CivilFeesAdded;
import uk.gov.justice.core.courts.CivilFeesUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.FeeStatus;
import uk.gov.moj.cpp.progression.domain.constant.FeeType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CivilFeeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CivilFeeRepository;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class CivilFeesUpdatedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CivilFeesUpdatedEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private CivilFeeRepository civilFeeRepository;

    @Handles("progression.event.civil-fees-updated")
    public void processCivilFeesUpdated(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.civil-fees-updated {} ", event.toObfuscatedDebugString());
        }

        final CivilFeesUpdated civilFeesUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CivilFeesUpdated.class);

        final CivilFeeEntity civilFeeEntity = civilFeeRepository.findBy(civilFeesUpdated.getFeeId());
        if (nonNull(civilFeeEntity)) {
            civilFeeEntity.setFeeStatus(FeeStatus.valueOf(civilFeesUpdated.getFeeStatus().name()));
            civilFeeEntity.setPaymentReference(civilFeesUpdated.getPaymentReference());
            civilFeeEntity.setFeeType(FeeType.valueOf(civilFeesUpdated.getFeeType()));
            civilFeeRepository.save(civilFeeEntity);
        }
    }

    @Handles("progression.event.civil-fees-added")
    public void processCivilFeeAdded(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.civil-fees-updated {} ", event.toObfuscatedDebugString());
        }

        final CivilFeesAdded civilFeesAdded = jsonObjectConverter.convert(event.payloadAsJsonObject(), CivilFeesAdded.class);

        final CivilFeeEntity newCivilFeeEntity = new CivilFeeEntity();
        newCivilFeeEntity.setFeeId(civilFeesAdded.getFeeId());
        newCivilFeeEntity.setFeeStatus(FeeStatus.valueOf(civilFeesAdded.getFeeStatus().name()));
        newCivilFeeEntity.setPaymentReference(civilFeesAdded.getPaymentReference());
        newCivilFeeEntity.setFeeType(FeeType.valueOf(civilFeesAdded.getFeeType()));
        civilFeeRepository.save(newCivilFeeEntity);
    }
}
