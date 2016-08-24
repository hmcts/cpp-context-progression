package uk.gov.moj.cpp.progression.query.view.converter;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.query.view.response.AdditionalInformation;
import uk.gov.moj.cpp.progression.query.view.response.AncillaryOrders;
import uk.gov.moj.cpp.progression.query.view.response.Defence;
import uk.gov.moj.cpp.progression.query.view.response.DefendantView;
import uk.gov.moj.cpp.progression.query.view.response.MedicalDocumentation;
import uk.gov.moj.cpp.progression.query.view.response.PreSentenceReport;
import uk.gov.moj.cpp.progression.query.view.response.Probation;
import uk.gov.moj.cpp.progression.query.view.response.Prosecution;
import uk.gov.moj.cpp.progression.query.view.response.StatementOfMeans;

public class DefendantToDefendantViewConverter {

    public DefendantView convert(Defendant defendant) {
        DefendantView defendantView = new DefendantView();
        defendantView.setCaseProgressionId(defendant.getCaseProgressionDetail().getId().toString());
        defendantView.setDefendantId(defendant.getDefendantId().toString());
        defendantView.setDefendantProgressionId(defendant.getId().toString());
        defendantView.setSentenceHearingReviewDecision(
                        defendant.getSentenceHearingReviewDecision());
        defendantView.setSentenceHearingReviewDecisionDateTime(
                        defendant.getSentenceHearingReviewDecisionDateTime());
        AdditionalInformation additionalInformation = convertAdditionalInformation(defendant);
        defendantView.setAdditionalInformation(additionalInformation);
        return defendantView;
    }


    private AdditionalInformation convertAdditionalInformation(Defendant defendant) {
        AdditionalInformation additionalInformation = new AdditionalInformation();

        Defence defence = new Defence(defendant.getDefenceOthers(),
                        new StatementOfMeans(defendant.getStatementOfMeans(),
                                        defendant.getIsStatementOffMeans()),
                        new MedicalDocumentation(defendant.getMedicalDocumentation(),
                                        defendant.getIsMedicalDocumentation()));
        additionalInformation.setDefence(defence);
        Probation probation = new Probation(defendant.getDangerousnessAssessment(),
                        new PreSentenceReport(defendant.getProvideGuidance(),
                                        defendant.getIsPSRRequested(),
                                        defendant.getDrugAssessment()));
        additionalInformation.setProbation(probation);
        Prosecution prosecution = new Prosecution(defendant.getProsecutionOthers(),
                        new AncillaryOrders(defendant.getAncillaryOrders(),
                                        defendant.getIsAncillaryOrders()));
        additionalInformation.setProsecution(prosecution);

        return additionalInformation;


    }


}
