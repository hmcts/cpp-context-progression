package uk.gov.moj.cpp.progression.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefenceEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProbationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProsecutionEvent;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefendantEventToDefendantConverter implements Converter<DefendantEvent, Defendant> {

    private static Logger logger = LoggerFactory.getLogger(DefendantEventToDefendantConverter.class);

    @Override
    public Defendant convert(DefendantEvent event) {

        logger.info("DEFENDANT:CONVERT:DefendantEvent");

        Defendant defendant = new Defendant();
        defendant.setId(event.getDefendantProgressionId());
        defendant.setDefendantId(event.getDefendantId());
        defendant.setSentenceHearingReviewDecision(true);
        defendant.setSentenceHearingReviewDecisionDateTime(LocalDate.now());
        AdditionalInformationEvent additionalInformationEvent = event.getAdditionalInformationEvent();
        if (additionalInformationEvent == null) return defendant;

        ProbationEvent probationEvent = additionalInformationEvent.getProbationEvent();
        if (probationEvent != null) {
            defendant.setPreSentenceReport(probationEvent.getPreSentenceReportEvent() == null ? null : probationEvent.getPreSentenceReportEvent().getProvideGuidance());
            defendant.setDrugAssessment(probationEvent.getPreSentenceReportEvent() == null ? null : probationEvent.getPreSentenceReportEvent().getDrugAssessment());
            defendant.setDangerousnessAssessment(probationEvent.getDangerousnessAssessment());
        }

        DefenceEvent defenceEvent = additionalInformationEvent.getDefenceEvent();
        if (defenceEvent != null) {
            defendant.setStatementOfMeans(defenceEvent.getStatementOfMeansEvent() == null ? null : defenceEvent.getStatementOfMeansEvent().getDetails());
            defendant.setMedicalDocumentation(defenceEvent.getMedicalDocumentationEvent() == null ? null : defenceEvent.getMedicalDocumentationEvent().getDetails());
            defendant.setDefenceOthers(defenceEvent.getOthersEvent() == null ? null : defenceEvent.getOthersEvent().getDetails());
        }

        ProsecutionEvent prosecutionEvent = additionalInformationEvent.getProsecutionEvent();
        if (prosecutionEvent != null) {
            defendant.setAncillaryOrders(prosecutionEvent.getAncillaryOrdersEvent() == null ? null : prosecutionEvent.getAncillaryOrdersEvent().getDetails());
            defendant.setProsecutionOthers(prosecutionEvent.getOthersEvent() == null ? null : prosecutionEvent.getOthersEvent().getDetails());
        }
        return defendant;
    }
}
