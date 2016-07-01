package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.event.service.CaseService;

/**
 * 
 * @author jchondig
 *
 */
@ServiceComponent(EVENT_LISTENER)
public class CaseReadyForScentenceHearingEventListener {

    @Inject
    private CaseService caseService;

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.events.case-ready-for-sentence-hearing")
    public void processEvent(final JsonEnvelope event) {
        CaseReadyForSentenceHearing caseReadyForSentenceHearing = jsonObjectConverter
                        .convert(event.payloadAsJsonObject(), CaseReadyForSentenceHearing.class);
        caseService.caseReadyForSentenceHearing(caseReadyForSentenceHearing,
                        event.metadata().version().get());
    }
}
