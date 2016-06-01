package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;
import javax.transaction.Transactional;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.CaseSentToCrownCourt;
import uk.gov.moj.cpp.progression.event.converter.CaseSentToCrownCourtToCaseProgressionDetailConverter;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

@ServiceComponent(EVENT_LISTENER)
public class CaseSentToCrownCourtEventListener {

	@Inject
	JsonObjectToObjectConverter jsonObjectConverter;
	
	@Inject
	CaseSentToCrownCourtToCaseProgressionDetailConverter entityConverter;

	@Inject
	CaseProgressionDetailRepository repository;


	@Transactional
	@Handles("progression.event.case-sent-to-crown-court")
	public void sentToCrownCourt(final JsonEnvelope event) {
		
		CaseSentToCrownCourt caseSentToCrownCourt = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseSentToCrownCourt.class);
		
		repository.save(entityConverter.convert(caseSentToCrownCourt));
	}

}
