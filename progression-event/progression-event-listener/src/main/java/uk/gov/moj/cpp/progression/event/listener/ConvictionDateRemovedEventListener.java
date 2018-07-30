package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateRemoved;
import uk.gov.moj.cpp.progression.event.service.CaseService;

@ServiceComponent(EVENT_LISTENER)
public class ConvictionDateRemovedEventListener {

    @Inject
    private CaseService caseService;

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.events.offence-conviction-date-removed")
    public void processEvent(final JsonEnvelope event) {
        final ConvictionDateRemoved convictionDateRemoved = jsonObjectConverter.convert(event.payloadAsJsonObject(),
                ConvictionDateRemoved.class);
        caseService.setConvictionDate(convictionDateRemoved.getOffenceId(), null);
    }

}
