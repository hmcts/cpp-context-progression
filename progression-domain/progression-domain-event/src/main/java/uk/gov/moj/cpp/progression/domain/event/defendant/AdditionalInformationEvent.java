package uk.gov.moj.cpp.progression.domain.event.defendant;

public class AdditionalInformationEvent {
    private final ProbationEvent probationEvent;
    private final DefenceEvent defenceEvent;
    private final ProsecutionEvent prosecutionEvent;

    private AdditionalInformationEvent(ProbationEvent probationEvent, DefenceEvent defence,
            ProsecutionEvent prosecution) {
        this.probationEvent = probationEvent;
        this.defenceEvent = defence;
        this.prosecutionEvent = prosecution;
    }

    public ProbationEvent getProbationEvent() {
        return probationEvent;
    }

    public DefenceEvent getDefenceEvent() {
        return defenceEvent;
    }

    public ProsecutionEvent getProsecutionEvent() {
        return prosecutionEvent;
    }

    public static final class AdditionalInformationEventBuilder {
        private transient ProbationEvent probationEvent;
        private transient DefenceEvent defenceEvent;
        private transient ProsecutionEvent prosecutionEvent;

        private AdditionalInformationEventBuilder() {
        }

        public static AdditionalInformationEventBuilder anAdditionalInformationEvent() {
            return new AdditionalInformationEventBuilder();
        }

        public AdditionalInformationEventBuilder probation(ProbationEvent probationEvent) {
            this.probationEvent = probationEvent;
            return this;
        }

        public AdditionalInformationEventBuilder defence(DefenceEvent defence) {
            this.defenceEvent = defence;
            return this;
        }

        public AdditionalInformationEventBuilder prosecution(ProsecutionEvent prosecution) {
            this.prosecutionEvent = prosecution;
            return this;
        }

        public AdditionalInformationEvent build() {
            return new AdditionalInformationEvent(probationEvent, defenceEvent, prosecutionEvent);
        }
    }
}