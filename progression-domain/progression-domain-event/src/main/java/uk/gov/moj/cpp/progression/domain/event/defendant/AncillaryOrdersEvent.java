package uk.gov.moj.cpp.progression.domain.event.defendant;

public class AncillaryOrdersEvent {
    private String details;
    private Boolean isAncillaryOrders;

    private AncillaryOrdersEvent(Boolean isAncillaryOrders, String details) {
        this.details = details;
        this.isAncillaryOrders = isAncillaryOrders;
    }

    public String getDetails() {
        return details;
    }

    public Boolean getIsAncillaryOrders() {
        return isAncillaryOrders;
    }

    public static final class AncillaryOrdersEventBuilder {
        private String details;
        private Boolean isAncillaryOrders;

        private AncillaryOrdersEventBuilder() {
        }

        public static AncillaryOrdersEventBuilder anAncillaryOrdersEvent() {
            return new AncillaryOrdersEventBuilder();
        }

        public AncillaryOrdersEventBuilder details(String details) {
            this.details = details;
            return this;
        }

        public AncillaryOrdersEventBuilder isAncillaryOrders(Boolean isAncillaryOrders) {
            this.isAncillaryOrders = isAncillaryOrders;
            return this;
        }

        public AncillaryOrdersEvent build() {
            AncillaryOrdersEvent others = new AncillaryOrdersEvent(isAncillaryOrders, details);
            return others;
        }
    }

}