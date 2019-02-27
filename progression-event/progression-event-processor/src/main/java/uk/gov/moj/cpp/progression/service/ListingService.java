package uk.gov.moj.cpp.progression.service;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.SendCaseForListing;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.processor.CasesReferredToCourtProcessor;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ListingService {

    public static final String LISTING_COMMAND_SEND_CASE_FOR_LISTING = "listing.command.send-case-for-listing";

    private static final Logger LOGGER = LoggerFactory.getLogger(CasesReferredToCourtProcessor.class.getCanonicalName());

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Enveloper enveloper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    public void sendCaseForListing(final JsonEnvelope jsonEnvelope, final SendCaseForListing sendCaseForListing) {
        final JsonObject sendCaseForListingJson = objectToJsonObjectConverter.convert(sendCaseForListing);
        LOGGER.info(" Posting Send Case For Listing to listing '{}' ", sendCaseForListingJson);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, LISTING_COMMAND_SEND_CASE_FOR_LISTING).apply(sendCaseForListingJson));
    }

}
