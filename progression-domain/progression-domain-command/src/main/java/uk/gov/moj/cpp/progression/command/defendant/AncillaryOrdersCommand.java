package uk.gov.moj.cpp.progression.command.defendant;

public class AncillaryOrdersCommand {
    private String details;

    private AncillaryOrdersCommand(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public static final class AncillaryOrdersCommandBuilder {
        private String details;

        private AncillaryOrdersCommandBuilder() {
        }

        public static AncillaryOrdersCommandBuilder anAncillaryOrdersCommandAdded() {
            return new AncillaryOrdersCommandBuilder();
        }

        public AncillaryOrdersCommandBuilder details(String details) {
            this.details = details;
            return this;
        }

        public AncillaryOrdersCommand build() {
            AncillaryOrdersCommand others = new AncillaryOrdersCommand(details);
            return others;
        }
    }

}