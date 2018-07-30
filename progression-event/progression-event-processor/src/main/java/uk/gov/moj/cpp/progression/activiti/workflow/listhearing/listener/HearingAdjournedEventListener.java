package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.listener;


import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant;
import uk.gov.moj.cpp.progression.activiti.workflow.listhearing.ListHearingService;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S3655")
@ServiceComponent(Component.EVENT_PROCESSOR)
public class HearingAdjournedEventListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HearingAdjournedEventListener.class.getName());

    private static final String SCHEDULE_ADJOURMENT = "Schedule Adjourment";


    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Inject
    private ListHearingService listHearingService;

    @Handles("public.hearing.adjourned")
    public void processEvent(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("Progression listened (public.hearing.adjourned): {}", jsonEnvelope.payloadAsJsonObject());
        final ListingCase listingCase = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ListingCase.class);

        final Map<String, Object> processMap = new HashMap<>();
        processMap.put(ProcessMapConstant.CASE_ID, listingCase.getCaseId());
        processMap.put(ProcessMapConstant.USER_ID, jsonEnvelope.metadata().userId().get());
        processMap.put(ProcessMapConstant.WHEN, SCHEDULE_ADJOURMENT);
        processMap.put(ProcessMapConstant.SEND_CASE_FOR_LISTING_PAYLOAD, listingCase);
        listHearingService.startProcess(processMap);
    }
}
