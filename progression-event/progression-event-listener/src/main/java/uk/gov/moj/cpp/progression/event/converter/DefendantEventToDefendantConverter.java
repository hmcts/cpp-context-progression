package uk.gov.moj.cpp.progression.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefenceEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProbationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProsecutionEvent;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefendantEventToDefendantConverter implements Converter<DefendantAdditionalInformationAdded, Defendant> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantEventToDefendantConverter.class);

    @Override
    public Defendant convert(DefendantAdditionalInformationAdded event) {
        LOGGER.info("DEFENDANT:CONVERT:DefendantEvent");
        Defendant defendant = new Defendant();
        defendant.setDefendantId(event.getDefendantId());
        defendant.setSentenceHearingReviewDecision(true);
        defendant.setSentenceHearingReviewDecisionDateTime(LocalDateTime.now());
        return populateAdditionalInformation(defendant, event);
    }

    public Defendant populateAdditionalInformation(Defendant defendant, DefendantAdditionalInformationAdded event) {
        AdditionalInformationEvent additionalInformationEvent = event.getAdditionalInformationEvent();

        if (additionalInformationEvent == null) {
            return defendant;
        }

        processProbationEvent(defendant, additionalInformationEvent);
        processDefenceEvent(defendant, additionalInformationEvent);
        processProsecutionEvent(defendant, additionalInformationEvent);

        return defendant;
    }

    private void processProsecutionEvent(Defendant defendant, AdditionalInformationEvent additionalInformationEvent) {
        ProsecutionEvent prosecutionEvent = additionalInformationEvent.getProsecutionEvent();
        if (prosecutionEvent != null) {
            defendant.setAncillaryOrders(prosecutionEvent.getAncillaryOrdersEvent() == null
                    ? null
                    : prosecutionEvent.getAncillaryOrdersEvent().getDetails());
            defendant.setIsAncillaryOrders(prosecutionEvent.getAncillaryOrdersEvent() == null
                    ? null
                    : prosecutionEvent.getAncillaryOrdersEvent().getIsAncillaryOrders());
            defendant.setProsecutionOthers(prosecutionEvent.getOtherDetails());
        }
    }

    private void processDefenceEvent(Defendant defendant, AdditionalInformationEvent additionalInformationEvent) {
        DefenceEvent defenceEvent = additionalInformationEvent.getDefenceEvent();
        if (defenceEvent != null) {
            defendant.setStatementOfMeans(defenceEvent.getStatementOfMeansEvent() == null
                    ? null
                    : defenceEvent.getStatementOfMeansEvent().getDetails());
            defendant.setIsStatementOffMeans(defenceEvent.getStatementOfMeansEvent() == null
                    ? null
                    : defenceEvent.getStatementOfMeansEvent().getIsStatementOfMeans());
            defendant.setMedicalDocumentation(defenceEvent.getMedicalDocumentationEvent() == null
                    ? null
                    : defenceEvent.getMedicalDocumentationEvent().getDetails());
            defendant.setIsMedicalDocumentation(defenceEvent.getMedicalDocumentationEvent() == null
                    ? null
                    : defenceEvent.getMedicalDocumentationEvent().getIsMedicalDocumentation());
            defendant.setDefenceOthers(defenceEvent.getOtherDetails());
        }
    }

    private void processProbationEvent(Defendant defendant, AdditionalInformationEvent additionalInformationEvent) {
        ProbationEvent probationEvent = additionalInformationEvent.getProbationEvent();
        if (probationEvent != null) {
            defendant.setIsPSRRequested(probationEvent.getPreSentenceReportEvent() == null
                    ? null
                    : probationEvent.getPreSentenceReportEvent().getPsrIsRequested());
            defendant.setDrugAssessment(probationEvent.getPreSentenceReportEvent() == null
                    ? null
                    : probationEvent.getPreSentenceReportEvent().getDrugAssessment());
            defendant.setProvideGuidance(probationEvent.getPreSentenceReportEvent() == null
                    ? null
                    : probationEvent.getPreSentenceReportEvent().getProvideGuidance());
            defendant.setDangerousnessAssessment(probationEvent.getDangerousnessAssessment());
        }
    }
}
