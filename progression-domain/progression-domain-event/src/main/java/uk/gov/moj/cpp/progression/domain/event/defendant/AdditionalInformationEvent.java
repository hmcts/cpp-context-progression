package uk.gov.moj.cpp.progression.domain.event.defendant;

public class AdditionalInformationEvent {
    private final ProbationEvent probationEvent;
    private final DefenceEvent defence;
    private final ProsecutionEvent prosecution;

    private AdditionalInformationEvent(ProbationEvent probationEvent, DefenceEvent defence, ProsecutionEvent prosecution) {
        this.probationEvent = probationEvent;
        this.defence = defence;
        this.prosecution = prosecution;
    }

    public ProbationEvent getProbationEvent() {
        return probationEvent;
    }

    public DefenceEvent getDefenceEvent() {
        return defence;
    }

    public ProsecutionEvent getProsecutionEvent() {
        return prosecution;
    }

    public static final class AdditionalInformationEventBuilder {
        private ProbationEvent probationEvent;
        private DefenceEvent defence;
        private ProsecutionEvent prosecution;

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
            this.defence = defence;
            return this;
        }

        public AdditionalInformationEventBuilder prosecution(ProsecutionEvent prosecution) {
            this.prosecution = prosecution;
            return this;
        }

        public AdditionalInformationEvent build() {
            return new AdditionalInformationEvent(probationEvent, defence, prosecution);
        }
    }
}