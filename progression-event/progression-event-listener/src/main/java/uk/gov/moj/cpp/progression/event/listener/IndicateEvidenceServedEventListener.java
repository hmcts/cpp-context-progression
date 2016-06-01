package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;
import javax.transaction.Transactional;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.IndicateEvidenceServed;
import uk.gov.moj.cpp.progression.event.converter.IndicateEvidenceServedToIndicateStatementConverter;
import uk.gov.moj.progression.persistence.repository.IndicateStatementRepository;

/**
 * @author jchondig
 *
 */
@ServiceComponent(EVENT_LISTENER)
public class IndicateEvidenceServedEventListener {


	@Inject
	JsonObjectToObjectConverter jsonObjectConverter;
	
	@Inject
	IndicateEvidenceServedToIndicateStatementConverter entityConverter;

	@Inject
	IndicateStatementRepository repository;

	@Transactional
	@Handles("progression.event.indicate-evidence-served")
    public void processEvent(final JsonEnvelope event) {
		
		IndicateEvidenceServed caseSentToCrownCourt = jsonObjectConverter.convert(event.payloadAsJsonObject(), IndicateEvidenceServed.class);
		
		repository.save(entityConverter.convert(caseSentToCrownCourt));
    }
}
