package uk.gov.moj.cpp.progression.query.view.response;

public class AdditionalInformation {
    private Boolean noMoreInformationRequired;

    private Defence defence;

    private Probation probation;

    private Prosecution prosecution;

    public Defence getDefence() {
        return defence;
    }

    public void setDefence(Defence defence) {
        this.defence = defence;
    }

    public Probation getProbation() {
        return probation;
    }

    public void setProbation(Probation probation) {
        this.probation = probation;
    }

    public Prosecution getProsecution() {
        return prosecution;
    }

    public void setProsecution(Prosecution prosecution) {
        this.prosecution = prosecution;
    }

    public Boolean getNoMoreInformationRequired() {
        return noMoreInformationRequired;
    }

    public void setNoMoreInformationRequired(Boolean noMoreInformationRequired) {
        this.noMoreInformationRequired = noMoreInformationRequired;
    }
}
