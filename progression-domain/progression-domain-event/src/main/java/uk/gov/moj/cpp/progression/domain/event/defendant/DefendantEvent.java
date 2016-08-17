package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

@Event("progression.events.defendant-added")
public class DefendantEvent {
    private final UUID defendantProgressionId;
    private final UUID defendantId;
    private final UUID caseProgressionId;
    private final AdditionalInformationEvent additionalInformationEvent;

    public DefendantEvent(UUID defendantProgressionId, UUID defendantId, UUID caseProgressionId,
            AdditionalInformationEvent additionalInformationEvent) {
        this.defendantProgressionId = defendantProgressionId;
        this.defendantId = defendantId;
        this.additionalInformationEvent = additionalInformationEvent;
        this.caseProgressionId = caseProgressionId;
    }

    public UUID getDefendantProgressionId() {
        return defendantProgressionId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public AdditionalInformationEvent getAdditionalInformationEvent() {
        return additionalInformationEvent;
    }

    public static final class DefendantEventBuilder {
        private UUID defendantProgressionId;
        private UUID defendantId;
        private UUID caseProgressionId;
        private AdditionalInformationEvent additionalInformationEvent;

        private DefendantEventBuilder() {
        }

        public static DefendantEventBuilder aDefendantEvent() {
            return new DefendantEventBuilder();
        }

        public DefendantEventBuilder defendantProgressionId(UUID defendantProgressionId) {
            this.defendantProgressionId = defendantProgressionId;
            return this;
        }

        public DefendantEventBuilder defendantId(UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public DefendantEventBuilder caseProgressionId(UUID caseProgressionId) {
            this.caseProgressionId = caseProgressionId;
            return this;
        }

        public DefendantEventBuilder additionalInformation(AdditionalInformationEvent additionalInformationEvent) {
            this.additionalInformationEvent = additionalInformationEvent;
            return this;
        }

        public DefendantEvent build() {
            DefendantEvent defendantEvent = new DefendantEvent(defendantProgressionId, defendantId, caseProgressionId,
                    additionalInformationEvent);
            return defendantEvent;
        }
    }
}
