package uk.gov.moj.cpp.progression.service;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.processor.CasesReferredToCourtProcessor;
import javax.inject.Inject;
import javax.json.JsonObject;


public class ListingService {

    public static final String LISTING_COMMAND_SEND_CASE_FOR_LISTING = "listing.command.list-court-hearing";

    private static final Logger LOGGER = LoggerFactory.getLogger(CasesReferredToCourtProcessor.class.getCanonicalName());

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Enveloper enveloper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public void listCourtHearing(final JsonEnvelope jsonEnvelope, final ListCourtHearing listCourtHearing) {
        final JsonObject listCourtHearingJson = objectToJsonObjectConverter.convert(listCourtHearing);
        LOGGER.info(" Posting Send Case For Listing to listing '{}' ", listCourtHearingJson);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, LISTING_COMMAND_SEND_CASE_FOR_LISTING).apply(listCourtHearingJson));
    }
}
