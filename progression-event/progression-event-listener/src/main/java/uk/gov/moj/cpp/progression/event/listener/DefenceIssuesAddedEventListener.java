package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.DefenceIssuesAdded;

/**
 * @author jchondig
 *
 */
@ServiceComponent(EVENT_LISTENER)
public class DefenceIssuesAddedEventListener {

	@Inject
	private uk.gov.moj.cpp.progression.event.service.CaseService caseService;

	@Inject
	JsonObjectToObjectConverter jsonObjectConverter;

	@Handles("progression.event.defence-issues-added")
	public void processEvent(final JsonEnvelope event) {

		caseService.addDefenceIssues(
				jsonObjectConverter.convert(event.payloadAsJsonObject(), DefenceIssuesAdded.class));
	}
}
