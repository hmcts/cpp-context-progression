package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.event.service.CaseService;

/**
 * 
 * @author jchondig
 *
 */
@ServiceComponent(EVENT_LISTENER)
public class CasePendingForScentenceHearingEventListener {

    @Inject
    private CaseService caseService;

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.events.case-pending-for-sentence-hearing")
    public void processEvent(final JsonEnvelope event) {
        CasePendingForSentenceHearing casePendingForSentenceHearing = jsonObjectConverter
                .convert(event.payloadAsJsonObject(), CasePendingForSentenceHearing.class);
        caseService.casePendingForSentenceHearing(casePendingForSentenceHearing, event.metadata().version().get());
    }
}
