package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.listener;


import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.HEARING_ID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.activiti.service.ActivitiService;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class HearingInitiatedEventListener {
    protected static final String HEARING_INITIATED_CONFIRMATION = "recieveHearingInitiatedConfirmation";
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingInitiatedEventListener.class.getName());

    @Inject
    private ActivitiService activitiService;

    @Handles("public.hearing.initiated")
    public void processEvent(final JsonEnvelope jsonEnvelope) {
        if (jsonEnvelope.payloadAsJsonObject().containsKey(HEARING_ID)) {
            final String hearingId = jsonEnvelope.payloadAsJsonObject().getString(HEARING_ID);
            activitiService.signalProcessByActivitiIdAndFieldName(HEARING_INITIATED_CONFIRMATION, HEARING_ID, hearingId);
        } else {
            LOGGER.error("Wrong hearingInitiated Event Received metadata {} payload {}", jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());
        }
    }

}
