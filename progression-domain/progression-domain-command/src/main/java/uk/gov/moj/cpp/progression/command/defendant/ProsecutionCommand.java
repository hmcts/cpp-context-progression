package uk.gov.moj.cpp.progression.command.defendant;

public class ProsecutionCommand {

    private final AncillaryOrdersCommand ancillaryOrders;
    private final OthersCommand others;

    public ProsecutionCommand(AncillaryOrdersCommand ancillaryOrders, OthersCommand others) {
        this.ancillaryOrders = ancillaryOrders;
        this.others = others;
    }

    public AncillaryOrdersCommand getAncillaryOrdersCommand() {
        return ancillaryOrders;
    }

    public OthersCommand getOthersCommand() {
        return others;
    }

    public static final class ProsecutionCommandBuilder {
        private AncillaryOrdersCommand ancillaryOrders;
        private OthersCommand others;

        private ProsecutionCommandBuilder() {
        }

        public static ProsecutionCommandBuilder aProsecutionCommand() {
            return new ProsecutionCommandBuilder();
        }

        public ProsecutionCommandBuilder ancillaryOrders(AncillaryOrdersCommand ancillaryOrders) {
            this.ancillaryOrders = ancillaryOrders;
            return this;
        }

        public ProsecutionCommandBuilder others(OthersCommand others) {
            this.others = others;
            return this;
        }

        public ProsecutionCommand build() {
            ProsecutionCommand prosecution = new ProsecutionCommand(ancillaryOrders, others);
            return prosecution;
        }
    }
}