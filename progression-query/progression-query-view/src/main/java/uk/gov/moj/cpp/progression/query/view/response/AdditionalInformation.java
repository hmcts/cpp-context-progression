package uk.gov.moj.cpp.progression.query.view.response;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class AdditionalInformation {
    private Boolean noMoreInformationRequired;

    private Defence defence;

    private Probation probation;

    private Prosecution prosecution;

    public Defence getDefence() {
        return defence;
    }

    public void setDefence(final Defence defence) {
        this.defence = defence;
    }

    public Probation getProbation() {
        return probation;
    }

    public void setProbation(final Probation probation) {
        this.probation = probation;
    }

    public Prosecution getProsecution() {
        return prosecution;
    }

    public void setProsecution(final Prosecution prosecution) {
        this.prosecution = prosecution;
    }

    public Boolean getNoMoreInformationRequired() {
        return noMoreInformationRequired;
    }

    public void setNoMoreInformationRequired(final Boolean noMoreInformationRequired) {
        this.noMoreInformationRequired = noMoreInformationRequired;
    }
}
