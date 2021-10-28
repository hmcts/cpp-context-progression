package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class ListingDefendantAddedToCourtProceedingsProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingDefendantAddedToCourtProceedingsProcessor.class);

    @Inject
    private ProgressionService progressionService;

    @Handles("public.listing.new-defendant-added-for-court-proceedings")
    public void process(final Envelope<PublicListingNewDefendantAddedForCourtProceedings> envelope) {
        final PublicListingNewDefendantAddedForCourtProceedings eventPayload = envelope.payload();
        final UUID hearingId = eventPayload.getHearingId();
        LOGGER.info("Defendant '{}' on case '{}' added for court proceedings for hearing '{}'", eventPayload.getDefendantId(), eventPayload.getCaseId(), hearingId);
        progressionService.prepareSummonsDataForAddedDefendant(envelope);

        progressionService.populateHearingToProbationCaseworker(envelope.metadata(), hearingId);
    }
}
