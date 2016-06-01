package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.ProsecutionTrialEstimateAdded;
import uk.gov.moj.cpp.progression.event.service.CaseService;

@ServiceComponent(EVENT_LISTENER)
public class TrialTimeEstimatedByProsecutionEventListener {

	@Inject
	private CaseService caseService;

	@Inject
	JsonObjectToObjectConverter jsonObjectConverter;

	@Handles("progression.event.prosecution-trial-estimate-added")
	public void processEvent(final JsonEnvelope event) {

		caseService.addTrialEstimateProsecution(
				jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionTrialEstimateAdded.class));
	}
}
