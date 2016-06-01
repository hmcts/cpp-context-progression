package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.event.service.CaseService;

/**
 * @author hshaik
 *
 */
@ServiceComponent(EVENT_LISTENER)
public class DirectionIssuedEventListener {

	@Inject
	private CaseService caseService;

	@Inject
	JsonObjectToObjectConverter jsonObjectConverter;

	@Handles("progression.event.direction-issued")
    public void processEvent(final JsonEnvelope event) {

		caseService.directionIssued(
				jsonObjectConverter.convert(event.payloadAsJsonObject(), DirectionIssued.class));
	}
}
