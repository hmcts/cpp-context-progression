package uk.gov.moj.cpp.progression.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefenceSolicitorFirmUpdatedForDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class DefenceSolicitorFirmUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseProgressionDetailRepository caseRepository;

    @Handles("progression.events.defence-solicitor-firm-for-defendant-updated")
    @Transactional
    public void defenceSolicitorFirmUpdated(final JsonEnvelope envelope) {
        DefenceSolicitorFirmUpdatedForDefendant event =
                        jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(),
                                        DefenceSolicitorFirmUpdatedForDefendant.class);

        CaseProgressionDetail caseDetail = getCase(event.getCaseId());
        Defendant defendant = getDefendant(caseDetail, event.getDefendantId());
        defendant.setDefenceSolicitorFirm(event.getDefenceSolicitorFirm());
    }

    private CaseProgressionDetail getCase(UUID caseId) {
        return caseRepository.findBy(caseId);
    }

    private Defendant getDefendant(CaseProgressionDetail caseDetail, UUID defendantId) {
        return caseDetail.getDefendant(defendantId);
    }
}
