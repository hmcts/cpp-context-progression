package uk.gov.moj.cpp.progression.domain.event.defendant;

public class ProsecutionEvent {

    private final AncillaryOrdersEvent ancillaryOrdersEvent;
    private final OtherEvent others;

    public ProsecutionEvent(AncillaryOrdersEvent ancillaryOrdersEvent, OtherEvent others) {
        this.ancillaryOrdersEvent = ancillaryOrdersEvent;
        this.others = others;
    }

    public AncillaryOrdersEvent getAncillaryOrdersEvent() {
        return ancillaryOrdersEvent;
    }

    public OtherEvent getOthersEvent() {
        return others;
    }

    public static final class ProsecutionEventBuilder {
        private AncillaryOrdersEvent ancillaryOrdersEvent;
        private OtherEvent others;

        private ProsecutionEventBuilder() {
        }

        public static ProsecutionEventBuilder aProsecutionEvent() {
            return new ProsecutionEventBuilder();
        }

        public ProsecutionEventBuilder ancillaryOrders(AncillaryOrdersEvent ancillaryOrdersEvent) {
            this.ancillaryOrdersEvent = ancillaryOrdersEvent;
            return this;
        }

        public ProsecutionEventBuilder others(OtherEvent others) {
            this.others = others;
            return this;
        }

        public ProsecutionEvent build() {
            ProsecutionEvent prosecution = new ProsecutionEvent(ancillaryOrdersEvent, others);
            return prosecution;
        }
    }
}