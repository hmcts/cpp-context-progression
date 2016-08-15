package uk.gov.moj.cpp.progression.command.defendant;

public class AdditionalInformationCommand {
    private final ProbationCommand probation;
    private final DefenceCommand defence;
    private final ProsecutionCommand prosecution;

    private AdditionalInformationCommand(ProbationCommand probation, DefenceCommand defence, ProsecutionCommand prosecution) {
        this.probation = probation;
        this.defence = defence;
        this.prosecution = prosecution;
    }

    public ProbationCommand getProbationCommand() {
        return probation;
    }

    public DefenceCommand getDefenceEvent() {
        return defence;
    }

    public ProsecutionCommand getProsecutionCommand() {
        return prosecution;
    }

    public static final class AdditionalInformationCommandBuilder {
        private ProbationCommand probation;
        private DefenceCommand defence;
        private ProsecutionCommand prosecution;

        private AdditionalInformationCommandBuilder() {
        }

        public static AdditionalInformationCommandBuilder anAdditionalInformationCommand() {
            return new AdditionalInformationCommandBuilder();
        }

        public AdditionalInformationCommandBuilder probation(ProbationCommand probation) {
            this.probation = probation;
            return this;
        }

        public AdditionalInformationCommandBuilder defence(DefenceCommand defence) {
            this.defence = defence;
            return this;
        }

        public AdditionalInformationCommandBuilder prosecution(ProsecutionCommand prosecution) {
            this.prosecution = prosecution;
            return this;
        }

        public AdditionalInformationCommand build() {
            return new AdditionalInformationCommand(probation, defence, prosecution);
        }
    }
}