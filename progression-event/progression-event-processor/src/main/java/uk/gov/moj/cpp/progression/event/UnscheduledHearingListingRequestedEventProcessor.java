package uk.gov.moj.cpp.progression.event;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.UnscheduledHearingListingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;
import uk.gov.moj.cpp.progression.helper.HearingUnscheduledListingHelper;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class UnscheduledHearingListingRequestedEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultUnscheduledListingHelper.class.getName());

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Inject
    private HearingUnscheduledListingHelper hearingUnscheduledListingHelper;

    @Handles("progression.event.unscheduled-hearing-listing-requested")
    public void process(final JsonEnvelope event) {

        LOGGER.info("Unscheduled listing , Processing progression.event.unscheduled-hearing-listing-requested {}.", event.payloadAsJsonObject());
        final UnscheduledHearingListingRequested unscheduledHearingListingRequested = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), UnscheduledHearingListingRequested.class);

        if (isHearingResulted(unscheduledHearingListingRequested)) {
            hearingResultUnscheduledListingHelper.processUnscheduledCourtHearings(event, unscheduledHearingListingRequested.getHearing());
        } else {
            hearingUnscheduledListingHelper.processUnscheduledHearings(event, unscheduledHearingListingRequested.getHearing());
        }

    }

    private boolean isHearingResulted(final UnscheduledHearingListingRequested unscheduledHearingListingRequested) {
        return (nonNull(unscheduledHearingListingRequested.getHearing().getCourtApplications()) &&
                unscheduledHearingListingRequested.getHearing().getCourtApplications().stream().allMatch(ca -> nonNull(ca.getJudicialResults()))) ||
                (nonNull(unscheduledHearingListingRequested.getHearing().getProsecutionCases()) &&
                        unscheduledHearingListingRequested.getHearing().getProsecutionCases().stream()
                                .allMatch(pc -> pc.getDefendants().stream()
                                        .allMatch(defendant -> defendant.getOffences().stream()
                                                .allMatch(offence -> nonNull(offence.getJudicialResults())))));
    }

}
