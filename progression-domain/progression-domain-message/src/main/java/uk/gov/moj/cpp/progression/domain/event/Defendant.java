package uk.gov.moj.cpp.progression.domain.event;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import uk.gov.moj.cpp.progression.domain.event.defendant.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
public class Defendant implements Serializable {

    private UUID id;
    private Boolean sentenceHearingReviewDecision;
    private Boolean isAdditionalInfoAvilable;
    private Person person;
    private String bailStatus;
    private ZonedDateTime custodyTimeLimit;
    private String defenceOrganisation;
    private Interpreter interpreter;
    public Defendant() {
        super();
    }

    public Defendant(final UUID id) {
        super();
        this.id = id;
    }

    public Defendant(final UUID id, final Person person, final String bailStatus, final ZonedDateTime custodyTimeLimit, final String defenceOrganisation, final Interpreter interpreter) {
        this.id = id;
        this.person = person;
        this.bailStatus = bailStatus;
        this.custodyTimeLimit = custodyTimeLimit;
        this.defenceOrganisation = defenceOrganisation;
        this.interpreter = interpreter;
    }

    /**
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    public Boolean getSentenceHearingReviewDecision() {
        return sentenceHearingReviewDecision;
    }

    public void setSentenceHearingReviewDecision(final Boolean sentenceHearingReviewDecision) {
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
    }

    public Boolean getIsAdditionalInfoAvilable() {
        return isAdditionalInfoAvilable;
    }

    public void setIsAdditionalInfoAvilable(final Boolean isAdditionalInfoAvilable) {
        this.isAdditionalInfoAvilable = isAdditionalInfoAvilable;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public String getBailStatus() {
        return bailStatus;
    }

    public ZonedDateTime getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public String getDefenceOrganisation() {
        return defenceOrganisation;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }
}
