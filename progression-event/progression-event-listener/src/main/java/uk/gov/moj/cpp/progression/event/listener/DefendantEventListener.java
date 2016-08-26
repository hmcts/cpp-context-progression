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
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;

@ServiceComponent(EVENT_LISTENER)
public class DefendantEventListener {

    private static Logger logger = LoggerFactory.getLogger(DefendantEventListener.class);

    @Inject
    private uk.gov.moj.cpp.progression.event.service.CaseService caseService;

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.events.defendant-additional-information-added")
    public void addAdditionalInformationForDefendant(final JsonEnvelope envelope) {

        logger.info("DEFENDANT:LISTENER");

        JsonObject payload = envelope.payloadAsJsonObject();
        caseService.addAdditionalInformationForDefendant(
                jsonObjectConverter.convert(payload, DefendantAdditionalInformationAdded.class));

    }
}
