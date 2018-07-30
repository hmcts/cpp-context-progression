package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.listener;


import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.CASE_ID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.listing.event.CaseSentForListing;
import uk.gov.moj.cpp.progression.activiti.service.ActivitiService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseSentForListingEventListener {
    protected static final String RECIEVE_LISTING_CREATED_CONFIRMATION = "recieveListingCreatedConfirmation";
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseSentForListingEventListener.class.getName());

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ActivitiService activitiService;

    @Handles(CaseSentForListing.EVENT_NAME)
    public void processEvent(final JsonEnvelope jsonEnvelope) {
        if (jsonEnvelope.payloadAsJsonObject().containsKey(CaseSentForListing.CASE_ID)) {
            final CaseSentForListing caseSentForListing = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), CaseSentForListing.class);
            activitiService.signalProcessByActivitiIdAndFieldName(RECIEVE_LISTING_CREATED_CONFIRMATION, CASE_ID, caseSentForListing.getCaseId());
        } else {
            LOGGER.error("Wrong CaseSentForListing Event Received metadata {} payload {}", jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());
        }
    }

}
