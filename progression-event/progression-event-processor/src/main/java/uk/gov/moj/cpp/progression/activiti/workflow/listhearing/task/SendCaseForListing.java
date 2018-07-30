package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.task;

import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.CASE_ID;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.SEND_CASE_FOR_LISTING_PAYLOAD;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.progression.helper.JsonHelper;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
@Named
public class SendCaseForListing implements JavaDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendCaseForListing.class.getName());

    private static final String LISTING_COMMAND_SEND_CASE_FOR_LISTING = "listing.command.send-case-for-listing";

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("listing.command.send-case-for-listing-dummy")
    public void doesNothing(final JsonEnvelope jsonEnvelope) {
        //required by framework
    }

    @Override
    public void execute(final DelegateExecution execution) throws Exception {
        final ListingCase listingCase = (ListingCase) execution.getVariable(SEND_CASE_FOR_LISTING_PAYLOAD);
        final String userId = execution.getVariable(USER_ID).toString();
        final String caseId = execution.getVariable(CASE_ID).toString();

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(listingCase);
        LOGGER.info("send case for listing json: {}", jsonObject);

        final JsonEnvelope postRequestEnvelope = JsonHelper.assembleEnvelopeWithPayloadAndMetaDetails(jsonObject, LISTING_COMMAND_SEND_CASE_FOR_LISTING, caseId, userId);
        sender.send(postRequestEnvelope);
    }

}
