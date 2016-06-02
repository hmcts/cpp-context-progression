package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.PTPHearingVacated;
import uk.gov.moj.cpp.progression.event.service.CaseService;

/**
 * 
 * @author jchondig
 *
 */
@ServiceComponent(EVENT_LISTENER)
public class PTPHearingVacatedEventListener {

	@Inject
	private CaseService caseService;

	@Inject
	JsonObjectToObjectConverter jsonObjectConverter;

	@Handles("progression.event.ptp-hearing-vacated")
    public void processEvent(final JsonEnvelope event	) {

		caseService.vacatePtpHeaing(
				jsonObjectConverter.convert(event.payloadAsJsonObject(), PTPHearingVacated.class),event.metadata().version().get());
	}
}
