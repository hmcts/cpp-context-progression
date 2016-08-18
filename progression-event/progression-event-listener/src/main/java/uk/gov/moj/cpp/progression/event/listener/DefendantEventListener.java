package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantEvent;
import uk.gov.moj.cpp.progression.event.converter.DefendantEventToDefendantConverter;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.progression.persistence.repository.DefendantRepository;

@ServiceComponent(EVENT_LISTENER)
public class DefendantEventListener {

    private static Logger logger = LoggerFactory.getLogger(DefendantEventListener.class);

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    DefendantEventToDefendantConverter defendantEventToDefendantConverter;

    @Inject
    DefendantRepository defendantRepository;

    @Handles("progression.events.additionalInformation-added")
    public void addDefendant(final JsonEnvelope envelope) {

        logger.info("DEFENDANT:LISTENER");

        JsonObject payload = envelope.payloadAsJsonObject();
        DefendantEvent defendantEvent = jsonObjectConverter.convert(payload, DefendantEvent.class);
        Defendant defendant = defendantEventToDefendantConverter.convert(defendantEvent);
        logger.info("-------------------------------------------");
        logger.info(defendant.toString());
        // defendantRepository.save(defendant);
    }
}
