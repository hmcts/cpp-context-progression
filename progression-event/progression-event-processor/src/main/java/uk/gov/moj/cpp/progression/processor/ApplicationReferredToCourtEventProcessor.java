package uk.gov.moj.cpp.progression.processor;


import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.Objects;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;


@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S2789", "squid:S1135"})
public class ApplicationReferredToCourtEventProcessor {

    @Inject
    ProgressionService progressionService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private ListingService listingService;
    @Inject
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Handles("progression.event.application-referred-to-court")
    public void process(final JsonEnvelope jsonEnvelope) {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final ApplicationReferredToCourt applicationReferredToCourt = jsonObjectToObjectConverter.convert(payload, ApplicationReferredToCourt.class);
        final CourtApplication courtApplication = applicationReferredToCourt
                                                    .getHearingRequest().getCourtApplications().stream()
                                                    .findFirst().orElse(null);

        if (Objects.nonNull(courtApplication)) {
                final ListCourtHearing listCourtHearing = listCourtHearingTransformer.transform(applicationReferredToCourt);
                listingService.listCourtHearing(jsonEnvelope, listCourtHearing);
                progressionService.updateCourtApplicationStatus(jsonEnvelope, courtApplication.getId(), ApplicationStatus.UN_ALLOCATED);
        }
    }
}
