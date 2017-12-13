package uk.gov.moj.cpp.progression.event.listener;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAllocationDecisionUpdated;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;
import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class DefendantAllocationDecisionUpdatedListener {

    private static final String CASE_ID_PROPERTY = "caseId";
    private static final String DEFENDANT_ID_PROPERTY = "defendantId";
    private static final String ALLOCATION_DECISION_PROPERTY = "allocationDecision";

    @Inject
    private CaseProgressionDetailRepository caseRepository;

    @Handles("progression.events.defendant-allocation-decision-updated")
    @Transactional
    public void allocationDecisionUpdated(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString(CASE_ID_PROPERTY));
        final UUID defendantId = UUID.fromString(payload.getString(DEFENDANT_ID_PROPERTY));
        final String allocationDecision = payload.getString(ALLOCATION_DECISION_PROPERTY);

        DefendantAllocationDecisionUpdated event = new DefendantAllocationDecisionUpdated(caseId, defendantId, allocationDecision);

        CaseProgressionDetail caseDetail = getCase(event.getCaseId());
        Defendant defendant = getDefendant(caseDetail, event.getDefendantId());
        defendant.setAllocationDecision(event.getAllocationDecision());
    }

    private CaseProgressionDetail getCase(UUID caseId) {
        return caseRepository.findBy(caseId);
    }

    private Defendant getDefendant(CaseProgressionDetail caseDetail, UUID defendantId) {
        return caseDetail.getDefendant(defendantId);
    }
}
