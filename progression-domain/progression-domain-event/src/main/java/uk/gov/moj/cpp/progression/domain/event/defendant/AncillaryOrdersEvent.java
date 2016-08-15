package uk.gov.moj.cpp.progression.domain.event.defendant;

public class AncillaryOrdersEvent {
    private String details;

    private AncillaryOrdersEvent(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public static final class AncillaryOrdersEventBuilder {
        private String details;

        private AncillaryOrdersEventBuilder() {
        }

        public static AncillaryOrdersEventBuilder anAncillaryOrdersEvent() {
            return new AncillaryOrdersEventBuilder();
        }

        public AncillaryOrdersEventBuilder details(String details) {
            this.details = details;
            return this;
        }

        public AncillaryOrdersEvent build() {
            AncillaryOrdersEvent others = new AncillaryOrdersEvent(details);
            return others;
        }
    }

}