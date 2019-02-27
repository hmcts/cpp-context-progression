package uk.gov.moj.cpp.progression.domain.event.defendant;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class ProsecutionEvent {

    private final AncillaryOrdersEvent ancillaryOrdersEvent;
    private final String otherDetails;

    public ProsecutionEvent(final AncillaryOrdersEvent ancillaryOrdersEvent, final String otherDetails) {
        this.ancillaryOrdersEvent = ancillaryOrdersEvent;
        this.otherDetails = otherDetails;
    }

    public AncillaryOrdersEvent getAncillaryOrdersEvent() {
        return ancillaryOrdersEvent;
    }

    public String getOtherDetails() {
        return otherDetails;
    }

    public static final class ProsecutionEventBuilder {
        private AncillaryOrdersEvent ancillaryOrdersEvent;
        private String otherDetails;

        private ProsecutionEventBuilder() {
        }

        public static ProsecutionEventBuilder aProsecutionEvent() {
            return new ProsecutionEventBuilder();
        }

        public ProsecutionEventBuilder setAncillaryOrders(final AncillaryOrdersEvent ancillaryOrdersEvent) {
            this.ancillaryOrdersEvent = ancillaryOrdersEvent;
            return this;
        }

        public ProsecutionEventBuilder setOtherDetails(final String otherDetails) {
            this.otherDetails = otherDetails;
            return this;
        }

        public ProsecutionEvent build() {
            return new ProsecutionEvent(ancillaryOrdersEvent, otherDetails);
        }

        public AncillaryOrdersEvent getAncillaryOrdersEvent() {
            return ancillaryOrdersEvent;
        }

        public void setAncillaryOrdersEvent(final AncillaryOrdersEvent ancillaryOrdersEvent) {
            this.ancillaryOrdersEvent = ancillaryOrdersEvent;
        }

        public String getOtherDetails() {
            return otherDetails;
        }
    }
    
    
}