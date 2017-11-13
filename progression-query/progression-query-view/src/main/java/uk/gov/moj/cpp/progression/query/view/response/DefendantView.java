package uk.gov.moj.cpp.progression.query.view.response;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefendantView {
    private LocalDateTime sentenceHearingReviewDecisionDateTime;
    private String defendantId;
    private AdditionalInformation additionalInformation;
    private Boolean sentenceHearingReviewDecision;
    private UUID personId;
    private List<OffenceView> offences;
    private UUID caseId;
    private String policeDefendantId;
    private String bailStatus;
    private List<DefendantBailDocumentView> defendantBailDocuments;
    private String defenceSolicitorFirm;
    private String allocationDecision;
    private InterpreterDetail interpreter;
    private LocalDate custodyTimeLimitDate;
    private Integer numPreviousConvictions;

    public DefendantView(Defendant defendant) {
        this.sentenceHearingReviewDecisionDateTime = defendant.getSentenceHearingReviewDecisionDateTime();
        this.defendantId = defendant.getDefendantId().toString();
        this.additionalInformation = convertAdditionalInformation(defendant);
        this.sentenceHearingReviewDecision = defendant.getSentenceHearingReviewDecision();
        this.personId = defendant.getPersonId();
        this.offences = constructDefendantChargeView(defendant);
        this.caseId = defendant.getCaseProgressionDetail().getCaseId();
        this.policeDefendantId = defendant.getPoliceDefendantId();
        this.bailStatus = defendant.getBailStatus();
        if (defendant.getDefendantBailDocuments() != null) {
            this.defendantBailDocuments =
                    defendant.getDefendantBailDocuments().stream()
                            .filter(s -> s.getActive().equals(Boolean.TRUE))
                            .map(s -> new DefendantBailDocumentView(s.getId(),
                                    s.getDocumentId(), s.getActive()))
                            .collect(Collectors.toList());
        }
        this.defenceSolicitorFirm = defendant.getDefenceSolicitorFirm();
        this.allocationDecision = defendant.getAllocationDecision();
        if(defendant.getInterpreter() == null){
            this.interpreter = new InterpreterDetail();
        }
        else{
            this.interpreter = new InterpreterDetail();
            this.interpreter.setNeeded(defendant.getInterpreter().getNeeded());
            this.interpreter.setLanguage(defendant.getInterpreter().getLanguage());
        }
        this.custodyTimeLimitDate = defendant.getCustodyTimeLimitDate();
        this.numPreviousConvictions = defendant.getNumPreviousConvictions();

    }

    private AdditionalInformation convertAdditionalInformation(Defendant defendant) {
        AdditionalInformation information = new AdditionalInformation();
        information.setNoMoreInformationRequired(defendant.getIsNoMoreInformationRequired());
        Defence defence = new Defence(defendant.getDefenceOthers(),
                new StatementOfMeans(defendant.getStatementOfMeans(),
                        defendant.getIsStatementOffMeans()),
                new MedicalDocumentation(defendant.getMedicalDocumentation(),
                        defendant.getIsMedicalDocumentation()));
        information.setDefence(defence);
        Probation probation = new Probation(defendant.getDangerousnessAssessment(),
                new PreSentenceReport(defendant.getProvideGuidance(),
                        defendant.getIsPSRRequested(),
                        defendant.getDrugAssessment()));
        information.setProbation(probation);
        Prosecution prosecution = new Prosecution(defendant.getProsecutionOthers(),
                new AncillaryOrders(defendant.getAncillaryOrders(),
                        defendant.getIsAncillaryOrders()));
        information.setProsecution(prosecution);

        return information;


    }

    private static List<OffenceView> constructDefendantChargeView(Defendant defendant) {
        final Set<OffenceDetail> offences = defendant.getOffences();
        if (offences == null) {
            return new ArrayList<>();
        } else {
            List<OffenceView> offenceViewList = new ArrayList<>();
            offences.forEach(offence -> offenceViewList.add(new OffenceView(offence)));
            offenceViewList.sort(Comparator.comparing(OffenceView::getOffenceSequenceNumber));
            return offenceViewList;
        }
    }

    public LocalDateTime getSentenceHearingReviewDecisionDateTime() {
        return sentenceHearingReviewDecisionDateTime;
    }

    public String getDefendantId() {
        return defendantId;
    }

    public AdditionalInformation getAdditionalInformation() {
        return additionalInformation;
    }

    public Boolean getSentenceHearingReviewDecision() {
        return sentenceHearingReviewDecision;
    }

    public UUID getPersonId() {
        return personId;
    }

    public List<OffenceView> getOffences() {
        return offences;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getPoliceDefendantId() {
        return policeDefendantId;
    }

    public String getBailStatus() {
        return bailStatus;
    }

    public List<DefendantBailDocumentView> getDefendantBailDocuments() {
        return defendantBailDocuments;
    }

    public String getDefenceSolicitorFirm() {
        return defenceSolicitorFirm;
    }

    public String getAllocationDecision() {
        return allocationDecision;
    }

    public InterpreterDetail getInterpreter() {
        return interpreter;
    }

    public LocalDate getCustodyTimeLimitDate() {
        return custodyTimeLimitDate;
    }

    public Integer getNumPreviousConvictions() {
        return numPreviousConvictions;
    }
}
