package uk.gov.moj.cpp.progression.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class InterpreterUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseProgressionDetailRepository caseRepository;

    @Handles("progression.events.interpreter-for-defendant-updated")
    @Transactional
    public void interpreterUpdated(final JsonEnvelope envelope) {
        final InterpreterUpdatedForDefendant event = jsonObjectToObjectConverter.convert(
                        envelope.payloadAsJsonObject(), InterpreterUpdatedForDefendant.class);
        final CaseProgressionDetail caseDetail = caseRepository.findBy(event.getCaseId());
        final Defendant defendant = caseDetail.getDefendant(event.getDefendantId());
        // This should not happen (because of cancel). But just in case.
        if (event.getInterpreter() == null) {
            defendant.setInterpreter(null);
        }
        else {
            defendant.setInterpreter(new InterpreterDetail(event.getInterpreter().getNeeded(),
                            event.getInterpreter().getLanguage()));
        }
    }



}
