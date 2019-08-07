package uk.gov.moj.cpp.progression.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.DEFENDANT_ID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ServiceComponent(EVENT_PROCESSOR)
public class OffencesDoesNotHaveRequiredModeOfTrialListener {

    static final String STRUCTURE_EVENTS_DEFENDANT_OFFENCES_DOES_NOT_HAVE_REQUIRED_MODEOFTRIAL = "public.progression.events.defendant-offences-does-not-have-required-modeoftrial";
    private static final Logger LOGGER = LoggerFactory.getLogger(OffencesDoesNotHaveRequiredModeOfTrialListener.class.getCanonicalName());

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("progression.events.defendant-offences-does-not-have-required-modeoftrial")
    public void handleOffencesDoesNotHaveRequiredModeOfTrialEvent(final JsonEnvelope jsonEnvelope) {
        final JsonObject privateEventPayload = jsonEnvelope.payloadAsJsonObject();
        LOGGER.debug("Received offences does not have required modeoftrial for caseId: " + privateEventPayload.getString(CASE_ID));

        final JsonObject publicEventPayload = Json.createObjectBuilder()
                .add(CASE_ID, privateEventPayload.getString(CASE_ID))
                .add(DEFENDANT_ID, privateEventPayload.getString(DEFENDANT_ID)).build();

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, STRUCTURE_EVENTS_DEFENDANT_OFFENCES_DOES_NOT_HAVE_REQUIRED_MODEOFTRIAL).apply(publicEventPayload));
    }



}