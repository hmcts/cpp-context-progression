package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.event.service.CaseService;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ServiceComponent(EVENT_LISTENER)
public class PreSentenceReportForDefendantsRequestedEventListener {

    @Inject
    private CaseService caseService;

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.events.pre-sentence-report-for-defendants-requested")
    public void processEvent(final JsonEnvelope event) {
        final PreSentenceReportForDefendantsRequested psrForDefendantsRequestedEvent =
                jsonObjectConverter.convert(event.payloadAsJsonObject(), PreSentenceReportForDefendantsRequested.class);
        caseService.preSentenceReportForDefendantsRequested(psrForDefendantsRequestedEvent);
    }

}
