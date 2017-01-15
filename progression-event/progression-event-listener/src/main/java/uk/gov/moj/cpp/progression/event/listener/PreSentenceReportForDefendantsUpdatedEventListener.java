package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsUpdated;
import uk.gov.moj.cpp.progression.event.service.CaseService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class PreSentenceReportForDefendantsUpdatedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreSentenceReportForDefendantsUpdatedEventListener.class);

    @Inject
    private CaseService caseService;

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    // TODO: Not getting to here .. Because the RAML is the event project is not defined for this..

    //     : Is it anything to do with the name - ie. the below versus
    //        progression.command.update-psr-for-defendants
    @Handles("progression.events.pre-sentence-report-for-defendants-updated")
    public void processEvent(final JsonEnvelope event) {
        LOGGER.info("**** In PreSentenceReportForDefendantsUpdatedEventListener ****");
        final PreSentenceReportForDefendantsUpdated psrForDefendantsUpdatedEvent =
                jsonObjectConverter.convert(event.payloadAsJsonObject(), PreSentenceReportForDefendantsUpdated.class);
        caseService.preSentenceReportForDefendantsUpdated(psrForDefendantsUpdatedEvent);
    }

}
