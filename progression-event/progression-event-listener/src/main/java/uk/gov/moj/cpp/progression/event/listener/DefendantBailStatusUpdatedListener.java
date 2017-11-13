package uk.gov.moj.cpp.progression.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailStatusUpdatedForDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantBailDocument;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class DefendantBailStatusUpdatedListener {

    public static final String UNCONDITIONAL = "unconditional";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseProgressionDetailRepository caseRepository;

    @Handles("progression.events.bail-status-updated-for-defendant")
    @Transactional
    public void bailStatusUpdated(final JsonEnvelope envelope) {
        BailStatusUpdatedForDefendant event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(), BailStatusUpdatedForDefendant.class);

        CaseProgressionDetail caseDetail = caseRepository.findBy(event.getCaseId());
        Defendant defendant = caseDetail.getDefendant(event.getDefendantId());
        defendant.setBailStatus(event.getBailStatus());
        defendant.setCustodyTimeLimitDate(event.getCustodyTimeLimitDate());
        if(event.getBailDocument() != null) {
            updatedActiveDocument(event.getBailDocument(), defendant);
        }

    }


    private void updatedActiveDocument(BailDocument bailDocument, Defendant defendant) {
        DefendantBailDocument defendantBailDocument = new DefendantBailDocument();
        defendantBailDocument.setDocumentId(bailDocument.getMaterialId());
        defendantBailDocument.setId(bailDocument.getId());
        defendantBailDocument.setActive(Boolean.TRUE);
        defendant.addDefendantBailDocument(defendantBailDocument);
    }
}
