package uk.gov.moj.cpp.progression.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.UnscheduledHearingListingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(EVENT_PROCESSOR)
public class UnscheduledHearingListingRequestedEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultUnscheduledListingHelper.class.getName());

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Handles("progression.event.unscheduled-hearing-listing-requested")
    public void process(final JsonEnvelope event) {

        LOGGER.info("Unscheduled listing , Processing progression.event.unscheduled-hearing-listing-requested {}.", event.payloadAsJsonObject());
        final UnscheduledHearingListingRequested unscheduledHearingListingRequested = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), UnscheduledHearingListingRequested.class);
        hearingResultUnscheduledListingHelper.processUnscheduledCourtHearings(event, unscheduledHearingListingRequested.getHearing());
    }

}
