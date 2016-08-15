package uk.gov.moj.cpp.progression.command.defendant;

public class OthersCommand {
    private final String details;

    private OthersCommand(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public static final class OthersCommandBuilder {
        private String details;

        private OthersCommandBuilder() {
        }

        public static OthersCommandBuilder anOthersAdded() {
            return new OthersCommandBuilder();
        }

        public OthersCommandBuilder details(String details) {
            this.details = details;
            return this;
        }

        public OthersCommand build() {
            OthersCommand others = new OthersCommand(details);
            return others;
        }
    }
}