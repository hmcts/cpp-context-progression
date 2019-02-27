package uk.gov.moj.cpp.progression.domain.event.defendant;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class AncillaryOrdersEvent {
    private final String details;
    private final Boolean isAncillaryOrders;

    private AncillaryOrdersEvent(final Boolean isAncillaryOrders, final String details) {
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

        public AncillaryOrdersEventBuilder setDetails(final String details) {
            this.details = details;
            return this;
        }

        public AncillaryOrdersEventBuilder setIsAncillaryOrders(final Boolean isAncillaryOrders) {
            this.isAncillaryOrders = isAncillaryOrders;
            return this;
        }

        public AncillaryOrdersEvent build() {
            return new AncillaryOrdersEvent(isAncillaryOrders, details);
        }
    }

}