package uk.gov.moj.cpp.progression.domain.event.defendant;

public class ProsecutionEvent {

    private final String ancillaryOrders;
    private final String others;

    public ProsecutionEvent(String ancillaryOrders, String others) {
        this.ancillaryOrders = ancillaryOrders;
        this.others = others;
    }

    public String getAncillaryOrders() {
        return ancillaryOrders;
    }

    public String getOthers() {
        return others;
    }

    public static final class ProsecutionEventBuilder {
        private String ancillaryOrders;
        private String others;

        private ProsecutionEventBuilder() {
        }

        public static ProsecutionEventBuilder aProsecutionEvent() {
            return new ProsecutionEventBuilder();
        }

        public ProsecutionEventBuilder ancillaryOrders(String ancillaryOrders) {
            this.ancillaryOrders = ancillaryOrders;
            return this;
        }

        public ProsecutionEventBuilder others(String others) {
            this.others = others;
            return this;
        }

        public ProsecutionEvent build() {
            ProsecutionEvent prosecution = new ProsecutionEvent(ancillaryOrders, others);
            return prosecution;
        }
    }
}