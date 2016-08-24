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

    @Handles("progression.events.defendant-additional-information-added")
    public void addDefendant(final JsonEnvelope envelope) {

        logger.info("DEFENDANT:LISTENER");

        JsonObject payload = envelope.payloadAsJsonObject();
        DefendantAdditionalInformationAdded defendantEvent = jsonObjectConverter.convert(payload,
                DefendantAdditionalInformationAdded.class);

        Defendant defendant = defendantRepository.findBy(defendantEvent.getDefendantProgressionId());
        if (null == defendant) {
            throw new IllegalArgumentException(
                    "No case progression defendant found with ID " + defendantEvent.getDefendantProgressionId());
        } else {
            defendant = defendantEventToDefendantConverter.populateAdditionalInformation(defendant, defendantEvent);
        }
        defendant.setSentenceHearingReviewDecision(true);
        defendant.setSentenceHearingReviewDecisionDateTime(defendantEvent.getSentenceHearingReviewDecisionDateTime());
        defendantRepository.save(defendant);
    }
}
