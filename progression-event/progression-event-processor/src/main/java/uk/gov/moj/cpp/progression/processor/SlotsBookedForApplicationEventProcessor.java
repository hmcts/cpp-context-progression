package uk.gov.moj.cpp.progression.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.SlotsBookedForApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_PROCESSOR)
public class SlotsBookedForApplicationEventProcessor {

    @Inject
    ProgressionService progressionService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private ListingService listingService;
    @Inject
    private ListCourtHearingTransformer listCourtHearingTransformer;

    private static final Logger LOGGER = LoggerFactory.getLogger(SlotsBookedForApplicationEventProcessor.class.getName());

    @SuppressWarnings("squid:S2259")
    @Handles("progression.event.slots-booked-for-application")
    public void process(final JsonEnvelope jsonEnvelope) {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final SlotsBookedForApplication slotsBookedForApplication = jsonObjectToObjectConverter.convert(payload, SlotsBookedForApplication.class);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Slots booked for application received with payload - {}", payload);
        }

        final CourtApplication courtApplication = slotsBookedForApplication
                .getHearingRequest().getCourtApplications().stream()
                .findFirst().orElse(null);

        final ApplicationReferredToCourt applicationReferredToCourt = ApplicationReferredToCourt.applicationReferredToCourt()
                .withHearingRequest(slotsBookedForApplication.getHearingRequest())
                .build();

        final ListCourtHearing listCourtHearing = listCourtHearingTransformer.transform(applicationReferredToCourt);

        // first create the hearing in progression
        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);

        // then update application status and list hearing
        progressionService.updateCourtApplicationStatus(jsonEnvelope, courtApplication.getId(), ApplicationStatus.UN_ALLOCATED);
        listingService.listCourtHearing(jsonEnvelope, listCourtHearing);

    }
}
