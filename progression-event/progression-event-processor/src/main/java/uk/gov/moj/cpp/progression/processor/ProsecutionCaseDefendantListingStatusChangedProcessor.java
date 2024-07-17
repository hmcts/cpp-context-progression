package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV3;
import uk.gov.justice.listing.courts.ListNextHearingsV3;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionCaseDefendantListingStatusChangedProcessor {

    static final String PROGRESSION_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V2 = "progression.event.prosecutionCase-defendant-listing-status-changed-v2";
    static final String PROGRESSION_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V3 = "progression.event.prosecutionCase-defendant-listing-status-changed-v3";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseDefendantListingStatusChangedProcessor.class.getCanonicalName());

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ListingService listingService;

    @Inject
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Handles(PROGRESSION_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V2)
    public void handleProsecutionCaseDefendantListingStatusChangedEvent(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received '{}' event with payload {}", PROGRESSION_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V2, jsonEnvelope.toObfuscatedDebugString());
        }

        final ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChanged = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ProsecutionCaseDefendantListingStatusChangedV2.class);

        final List<ProsecutionCase> prosecutionCases = prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases();
        final List<ListHearingRequest> listHearingRequests = prosecutionCaseDefendantListingStatusChanged.getListHearingRequests();
        final UUID hearingId = prosecutionCaseDefendantListingStatusChanged.getHearing().getId();
        final Boolean isGroupProceedings = prosecutionCaseDefendantListingStatusChanged.getHearing().getIsGroupProceedings();

        if (!CollectionUtils.isEmpty(listHearingRequests)) {
            listingService.listCourtHearing(jsonEnvelope, listCourtHearingTransformer.transform(jsonEnvelope, prosecutionCases, listHearingRequests, hearingId, isGroupProceedings));
        }
    }

    @Handles(PROGRESSION_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V3)
    public void handleProsecutionCaseDefendantListingStatusChangedEventV3(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received '{}' event with payload {}", PROGRESSION_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V3, jsonEnvelope.toObfuscatedDebugString());
        }

        final ProsecutionCaseDefendantListingStatusChangedV3 prosecutionCaseDefendantListingStatusChanged = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ProsecutionCaseDefendantListingStatusChangedV3.class);

        final ListNextHearingsV3 listNextHearings = prosecutionCaseDefendantListingStatusChanged.getListNextHearings();

        listingService.listNextCourtHearings(jsonEnvelope, listNextHearings);
    }
}
