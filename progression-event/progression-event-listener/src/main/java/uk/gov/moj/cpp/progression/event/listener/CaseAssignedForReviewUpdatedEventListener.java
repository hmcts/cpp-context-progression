package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated;
import uk.gov.moj.cpp.progression.event.service.CaseService;

/**
 * 
 * @author jchondig
 *
 */
@ServiceComponent(EVENT_LISTENER)
public class CaseAssignedForReviewUpdatedEventListener {

    @Inject
    private CaseService caseService;

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.events.case-assigned-for-review-updated")
    public void processEvent(final JsonEnvelope event) {
        final CaseAssignedForReviewUpdated caseAssignedForReviewUpdated = jsonObjectConverter
                        .convert(event.payloadAsJsonObject(), CaseAssignedForReviewUpdated.class);
        caseService.caseAssignedForReview(caseAssignedForReviewUpdated);
    }
}
