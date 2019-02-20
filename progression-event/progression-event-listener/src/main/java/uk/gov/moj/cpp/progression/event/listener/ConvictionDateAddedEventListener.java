package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateAdded;
import uk.gov.moj.cpp.progression.event.service.CaseService;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ServiceComponent(EVENT_LISTENER)
public class ConvictionDateAddedEventListener {

    @Inject
    private CaseService caseService;

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.events.offence-conviction-date-changed")
    public void processEvent(final JsonEnvelope event) {
        final ConvictionDateAdded convictionDateAdded = jsonObjectConverter.convert(event.payloadAsJsonObject(),
                ConvictionDateAdded.class);
        caseService.setConvictionDate(convictionDateAdded.getOffenceId(), convictionDateAdded.getConvictionDate());
    }

}
