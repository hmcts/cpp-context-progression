package uk.gov.moj.cpp.progression.domain.event.defendant;

public class OtherEvent {
    private final String details;

    private OtherEvent(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public static final class OthersEventBuilder {
        private String details;

        private OthersEventBuilder() {
        }

        public static OthersEventBuilder anOthersEvent() {
            return new OthersEventBuilder();
        }

        public OthersEventBuilder details(String details) {
            this.details = details;
            return this;
        }

        public OtherEvent build() {
            OtherEvent others = new OtherEvent(details);
            return others;
        }
    }
}