package uk.gov.justice.api.resource.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AmendmentReason {

    private UUID id;
    private Integer seqNo;
    private String reasonDescription;

    @JsonCreator
    public AmendmentReason(final UUID id, final Integer seqNo, final String reasonDescription) {
        this.id = id;
        this.seqNo = seqNo;
        this.reasonDescription = reasonDescription;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public Integer getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(final Integer seqNo) {
        this.seqNo = seqNo;
    }

    public String getReasonDescription() {
        return reasonDescription;
    }

    public void setReasonDescription(final String reasonDescription) {
        this.reasonDescription = reasonDescription;
    }

    public static AmendmentReason.Builder amendmentReason() {
        return new AmendmentReason.Builder();
    }

    public static class Builder {
        private UUID id;
        private Integer seqNo;
        private String reasonDescription;

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withSeqNo(final Integer seqNo) {
            this.seqNo = seqNo;
            return this;
        }

        public Builder withReasonDescription(final String reasonDescription) {
            this.reasonDescription = reasonDescription;
            return this;
        }

        public AmendmentReason build() {
            return new AmendmentReason(id, seqNo, reasonDescription);
        }

    }
}
