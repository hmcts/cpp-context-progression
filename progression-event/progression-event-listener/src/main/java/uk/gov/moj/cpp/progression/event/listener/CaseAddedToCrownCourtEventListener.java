package uk.gov.moj.cpp.progression.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

/**
 * @author hshaik
 */
@ServiceComponent(EVENT_LISTENER)
public class CaseAddedToCrownCourtEventListener {

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;


    @Inject
    CaseProgressionDetailRepository repository;

    @Transactional
    @Handles("progression.events.case-added-to-crown-court")
    public void addedToCrownCourt(final JsonEnvelope event) {

        final CaseAddedToCrownCourt caseAddedToCrownCourt = jsonObjectConverter
                .convert(event.payloadAsJsonObject(), CaseAddedToCrownCourt.class);

        CaseProgressionDetail caseProgressionDetailDb = repository.findByCaseId(caseAddedToCrownCourt.getCaseId());
        caseProgressionDetailDb.setCourtCentreId(caseAddedToCrownCourt.getCourtCentreId());
    }
}
