package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
public class CorrectHearingDaysWithoutCourtCentreApi {

    @Inject
    private Sender sender;

    static final String PROGRESSION_COMMAND_CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE = "progression.command.correct-hearing-days-without-court-centre";

    private static final Logger LOGGER = LoggerFactory.getLogger(CorrectHearingDaysWithoutCourtCentreApi.class);

    @Handles("progression.correct-hearing-days-without-court-centre")
    public void handleCorrectHearingDaysWithoutCourtCentre(final JsonEnvelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'progression.correct-hearing-days-without-court-centre' received with payload {}", envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_COMMAND_CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE),
                envelope.payload()));
    }
}
