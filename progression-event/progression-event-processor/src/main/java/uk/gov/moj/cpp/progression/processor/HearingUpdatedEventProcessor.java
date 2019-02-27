package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingUpdatedEventProcessor {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("public.listing.hearing-updated")
    public void publishHearingDetailChangedPublicEvent(final JsonEnvelope jsonEnvelope) {
        final HearingUpdated hearingUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingUpdated.class);
        progressionService.updateHearingListingStatusToHearingUpdate(jsonEnvelope,hearingUpdated);
        progressionService.publishHearingDetailChangedPublicEvent(jsonEnvelope,hearingUpdated);
    }





}
