package uk.gov.moj.cpp.progression.query.view.response;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class DefendantView {
    private final ZonedDateTime sentenceHearingReviewDecisionDateTime;
    private final String defendantId;
    private final AdditionalInformation additionalInformation;
    private final Boolean sentenceHearingReviewDecision;
    private final Person person;
    private final List<OffenceView> offences;
    private final UUID caseId;
    private final String policeDefendantId;
    private final String bailStatus;
    private List<DefendantBailDocumentView> defendantBailDocuments;
    private final String defenceSolicitorFirm;
    private InterpreterDetail interpreter;
    private final LocalDate custodyTimeLimitDate;
    private UUID personId;

    public DefendantView(final Defendant defendant) {
        this.sentenceHearingReviewDecisionDateTime = defendant.getSentenceHearingReviewDecisionDateTime();
        this.defendantId = defendant.getDefendantId().toString();
        this.additionalInformation = convertAdditionalInformation(defendant);
        this.sentenceHearingReviewDecision = defendant.getSentenceHearingReviewDecision();
        this.person = convertPerson(defendant.getPerson());
        this.personId = defendant.getPerson().getPersonId();
        this.offences = constructDefendantOffencesView(defendant).getOffences();
        this.caseId = defendant.getCaseProgressionDetail().getCaseId();
        this.policeDefendantId = defendant.getPoliceDefendantId();
        this.bailStatus = defendant.getBailStatus();
        if (defendant.getDefendantBailDocuments() != null) {
            this.defendantBailDocuments =
                    defendant.getDefendantBailDocuments().stream()
                            .filter(s -> s.getActive().equals(Boolean.TRUE))
                            .map(s -> new DefendantBailDocumentView(s.getId(),
                                    s.getDocumentId(), s.getActive()))
                            .collect(toList());
        }
        this.defenceSolicitorFirm = defendant.getDefenceSolicitorFirm();
        if (defendant.getInterpreter() == null) {
            this.interpreter = new InterpreterDetail();
        } else {
            this.interpreter = new InterpreterDetail();
            this.interpreter.setNeeded(defendant.getInterpreter().getNeeded());
            this.interpreter.setLanguage(defendant.getInterpreter().getLanguage());
        }
        this.custodyTimeLimitDate = defendant.getCustodyTimeLimitDate();

    }

    private AdditionalInformation convertAdditionalInformation(final Defendant defendant) {
        final AdditionalInformation information = new AdditionalInformation();
        information.setNoMoreInformationRequired(defendant.getIsNoMoreInformationRequired());
        final Defence defence = new Defence(defendant.getDefenceOthers(),
                new StatementOfMeans(defendant.getStatementOfMeans(),
                        defendant.getIsStatementOffMeans()),
                new MedicalDocumentation(defendant.getMedicalDocumentation(),
                        defendant.getIsMedicalDocumentation()));
        information.setDefence(defence);
        final Probation probation = new Probation(defendant.getDangerousnessAssessment(),
                new PreSentenceReport(defendant.getProvideGuidance(),
                        defendant.getIsPSRRequested(),
                        defendant.getDrugAssessment()));
        information.setProbation(probation);
        final Prosecution prosecution = new Prosecution(defendant.getProsecutionOthers(),
                new AncillaryOrders(defendant.getAncillaryOrders(),
                        defendant.getIsAncillaryOrders()));
        information.setProsecution(prosecution);

        return information;


    }

    private Person convertPerson(final uk.gov.moj.cpp.progression.persistence.entity.Person person) {
        return new Person()
                .builder()
                .personId(person.getPersonId())
                .title(person.getTitle())
                .dateOfBirth(person.getDateOfBirth())
                .firstName(person.getFirstName())
                .lastName(person.getLastName())
                .nationality(person.getNationality())
                .gender(person.getGender())
                .homeTelephone(person.getHomeTelephone())
                .workTelephone(person.getWorkTelephone())
                .fax(person.getFax())
                .email(person.getEmail())
                .address(person.getAddress())
                .build();
    }

    private static OffencesView constructDefendantOffencesView(final Defendant defendant) {
        final Set<OffenceDetail> offences = defendant.getOffences();
        if (offences == null) {
            return new OffencesView(new ArrayList<>());
        } else {
            return new OffencesView(offences.stream().map(OffenceView::new).collect(toList()));
        }
    }

    public ZonedDateTime getSentenceHearingReviewDecisionDateTime() {
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

    public Person getPerson() {
        return person;
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

    public InterpreterDetail getInterpreter() {
        return interpreter;
    }

    public LocalDate getCustodyTimeLimitDate() {
        return custodyTimeLimitDate;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(final UUID personId) {
        this.personId = personId;
    }
}
