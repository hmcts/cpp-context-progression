package uk.gov.moj.cpp.progression.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;


public class PartialMatchDefendant implements Serializable {

    private static final long serialVersionUID = -7706350655650006710L;

    private final UUID defendantId;

    private final UUID prosecutionCaseId;

    private final String defendantName;

    private final String caseReference;

    private final String payload;

    private final ZonedDateTime caseReceivedDatetime;

    private PartialMatchDefendant(UUID defendantId, UUID prosecutionCaseId, String defendantName, String caseReference, String payload, ZonedDateTime caseReceivedDatetime) {
        this.defendantId = defendantId;
        this.prosecutionCaseId = prosecutionCaseId;
        this.defendantName = defendantName;
        this.caseReference = caseReference;
        this.payload = payload;
        this.caseReceivedDatetime = caseReceivedDatetime;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public String getPayload() {
        return payload;
    }

    public ZonedDateTime getCaseReceivedDatetime() {
        return caseReceivedDatetime;
    }


    @Override
    public String toString() {
        return "CPSearchDefendant{" +
                "defendantId=" + defendantId +
                ", prosecutionCaseId=" + prosecutionCaseId +
                ", defendantName='" + defendantName + '\'' +
                ", caseReference='" + caseReference + '\'' +
                ", payload='" + payload + '\'' +
                ", caseReceivedDatetime=" + caseReceivedDatetime +
                '}';
    }

    public static Builder partialMatchDefendant() {
        return new Builder();
    }

    public static class Builder {

        private UUID defendantId;

        private UUID prosecutionCaseId;

        private String defendantName;

        private String caseReference;

        private String payload;

        private ZonedDateTime caseReceivedDatetime;

        public Builder withDefendantId(final UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
            this.prosecutionCaseId = prosecutionCaseId;
            return this;
        }

        public Builder withDefendantName(final String defendantName) {
            this.defendantName = defendantName;
            return this;
        }

        public Builder withCaseReference(final String caseReference) {
            this.caseReference = caseReference;
            return this;
        }

        public Builder withPayload(final String payload) {
            this.payload = payload;
            return this;
        }

        public Builder withCaseReceivedDatetime(final ZonedDateTime caseReceivedDatetime) {
            this.caseReceivedDatetime = caseReceivedDatetime;
            return this;
        }

        public PartialMatchDefendant build() {
            return new PartialMatchDefendant(defendantId, prosecutionCaseId, defendantName, caseReference, payload, caseReceivedDatetime);
        }
    }
}
