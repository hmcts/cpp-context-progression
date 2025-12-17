package uk.gov.moj.cpp.progression.event.listener;


import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.event.converter.DefendantAddedToDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ServiceComponent(EVENT_LISTENER)
public class DefendantAddedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private DefendantAddedToDefendant defendantAddedConverter;

    @Inject
    private CaseProgressionDetailRepository caseRepository;

    @Inject
    private SearchProsecutionCase searchCase;

    @Handles("progression.events.defendant-added")
    @Transactional
    public void addDefendant(final JsonEnvelope envelope) {
        final DefendantAdded defendantAdded = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantAdded.class);

        final UUID caseId = defendantAdded.getCaseId();
        final String caseUrn = defendantAdded.getCaseUrn();
        final UUID defendantId = defendantAdded.getDefendantId();

        CaseProgressionDetail caseProgressionDetail = caseRepository.findBy(caseId);

        if(caseProgressionDetail==null){
            caseProgressionDetail= new CaseProgressionDetail();
            caseProgressionDetail.setCaseId(caseId);
            caseProgressionDetail.setCaseUrn(caseUrn);
            caseProgressionDetail.setStatus(CaseStatusEnum.INCOMPLETE);
            caseRepository.save(caseProgressionDetail);
        }
        caseProgressionDetail.addDefendant(defendantAddedConverter.convert(defendantAdded));
        makeSearchable(caseProgressionDetail, caseProgressionDetail.getDefendant(defendantId));
    }

    private void makeSearchable(final CaseProgressionDetail caseProgressionDetail, final Defendant defendant) {
        searchCase.makeSearchable(caseProgressionDetail, defendant);
    }
}
