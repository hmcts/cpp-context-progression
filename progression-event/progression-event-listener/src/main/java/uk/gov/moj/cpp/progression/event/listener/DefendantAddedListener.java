package uk.gov.moj.cpp.progression.event.listener;


import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.event.converter.DefendantAddedToDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.time.LocalDate.now;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class DefendantAddedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private DefendantAddedToDefendant defendantAddedConverter;

    @Inject
    private CaseProgressionDetailRepository caseRepository;

    @Handles("progression.events.defendant-added")
    @Transactional
    public void addDefendant(final JsonEnvelope envelope) {
        DefendantAdded defendantAdded = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantAdded.class);

        UUID caseId = defendantAdded.getCaseId();
        String caseUrn = defendantAdded.getCaseUrn();

        CaseProgressionDetail caseProgressionDetail = caseRepository.findBy(caseId);

        if(caseProgressionDetail==null){
            caseProgressionDetail= new CaseProgressionDetail();
            caseProgressionDetail.setCaseId(caseId);
            caseProgressionDetail.setCaseUrn(caseUrn);
            caseProgressionDetail.setId(caseId);
            caseProgressionDetail.setStatus(CaseStatusEnum.INCOMPLETE);
            caseRepository.save(caseProgressionDetail);
        }

        caseProgressionDetail.addDefendant(defendantAddedConverter.convert(defendantAdded));
    }




}
