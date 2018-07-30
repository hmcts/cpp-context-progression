package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.listener;


import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.hearing.InitiateHearing;
import uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.InitiateHearingService;
import uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.transformer.InitiateHearingTransformer;
import uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant;

@SuppressWarnings("squid:S3655")
@ServiceComponent(Component.EVENT_PROCESSOR)
public class HearingConfirmedEventListener {
    private static final Logger LOGGER =
                    LoggerFactory.getLogger(HearingConfirmedEventListener.class.getName());

    @Inject
    private InitiateHearingService initiateHearingService;

    @Handles("public.hearing-confirmed")
    public void processEvent(final JsonEnvelope jsonEnvelope) {
        if (jsonEnvelope.payloadAsJsonObject().containsKey(ProcessMapConstant.CASE_ID)) {
            final InitiateHearing initiateHearing =
                            InitiateHearingTransformer.transformToInitiateHearing(jsonEnvelope.payloadAsJsonObject());
            final Map<String, Object> processMap = new HashMap<>();
            processMap.put(ProcessMapConstant.CASE_ID, initiateHearing.getCases().get(0).getCaseId());
            processMap.put(ProcessMapConstant.USER_ID, jsonEnvelope.metadata().userId().get());
            processMap.put(ProcessMapConstant.HEARING_ID, initiateHearing.getHearing().getId());
            processMap.put(ProcessMapConstant.INITIATE_HEARING_PAYLOAD, initiateHearing);
            initiateHearingService.startProcess(processMap);
        } else {
            LOGGER.error("Wrong hearing confirmed Event Received metadata {} payload {}",
                            jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());
        }
    }
}
