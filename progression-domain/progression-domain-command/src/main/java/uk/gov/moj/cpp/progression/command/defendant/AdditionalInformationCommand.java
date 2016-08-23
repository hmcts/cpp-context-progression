package uk.gov.moj.cpp.progression.command.defendant;

public class AdditionalInformationCommand {
    private ProbationCommand probation;
    private DefenceCommand defence;
    private ProsecutionCommand prosecution;

    public ProbationCommand getProbation() {
        return probation;
    }

    public void setProbation(ProbationCommand probation) {
        this.probation = probation;
    }

    public DefenceCommand getDefence() {
        return defence;
    }

    public void setDefence(DefenceCommand defence) {
        this.defence = defence;
    }

    public ProsecutionCommand getProsecution() {
        return prosecution;
    }

    public void setProsecution(ProsecutionCommand prosecution) {
        this.prosecution = prosecution;
    }

    @Override
    public String toString() {
        return "AdditionalInformationCommand{" +
                "probation=" + probation +
                ", defence=" + defence +
                ", prosecution=" + prosecution +
                '}';
    }
}