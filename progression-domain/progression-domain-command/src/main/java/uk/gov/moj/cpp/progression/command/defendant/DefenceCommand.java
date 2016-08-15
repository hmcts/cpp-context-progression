package uk.gov.moj.cpp.progression.command.defendant;

public class DefenceCommand {
    private final StatementOfMeansCommand statementOfMeans;
    private final MedicalDocumentationCommand medicalDocumentation;
    private OthersCommand others;

    public DefenceCommand(StatementOfMeansCommand statementOfMeans, MedicalDocumentationCommand medicalDocumentation, OthersCommand others) {
        this.statementOfMeans = statementOfMeans;
        this.medicalDocumentation = medicalDocumentation;
        this.others = others;
    }

    public StatementOfMeansCommand getStatementOfMeansCommand() {
        return statementOfMeans;
    }

    public MedicalDocumentationCommand getMedicalDocumentationCommand() {
        return medicalDocumentation;
    }

    public OthersCommand getOthers() {
        return others;
    }

    public static final class DefenceCommandBuilder {
        private StatementOfMeansCommand statementOfMeans;
        private MedicalDocumentationCommand medicalDocumentation;
        private OthersCommand others;

        private DefenceCommandBuilder() {
        }

        public DefenceCommandBuilder statementOfMeans(StatementOfMeansCommand statementOfMeans) {
            this.statementOfMeans = statementOfMeans;
            return this;
        }

        public DefenceCommandBuilder medicalDocumentation(MedicalDocumentationCommand medicalDocumentation) {
            this.medicalDocumentation = medicalDocumentation;
            return this;
        }

        public DefenceCommandBuilder others(OthersCommand others) {
            this.others = others;
            return this;
        }

        public static DefenceCommandBuilder aDefenceCommand() {
            return new DefenceCommandBuilder();
        }

        public DefenceCommand build() {
            return new DefenceCommand(statementOfMeans, medicalDocumentation, others);
        }
    }
}