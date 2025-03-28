package uk.gov.justice.api.resource.dto;


import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AmendmentRecord {
    private String amendedBy;
    private String validatedBy;
    private ZonedDateTime amendmentDate;
    private ZonedDateTime validationDate;
    private AmendmentReason amendmentReason;
    private List<ResultPrompt> resultPromptsRecord;


    @JsonCreator
    public AmendmentRecord(final String amendedBy, final String validatedBy, final ZonedDateTime amendmentDate,
                           final ZonedDateTime validationDate, final AmendmentReason amendmentReason, final List<ResultPrompt> resultPromptsRecord) {
        this.amendedBy = amendedBy;
        this.validatedBy = validatedBy;
        this.amendmentDate = amendmentDate;
        this.validationDate = validationDate;
        this.amendmentReason = amendmentReason;
        this.resultPromptsRecord = resultPromptsRecord;
    }

    public String getAmendedBy() {
        return amendedBy;
    }

    public void setAmendedBy(final String amendedBy) {
        this.amendedBy = amendedBy;
    }

    public String getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(final String validatedBy) {
        this.validatedBy = validatedBy;
    }

    public ZonedDateTime getAmendmentDate() {
        return amendmentDate;
    }

    public void setAmendmentDate(final ZonedDateTime amendmentDate) {
        this.amendmentDate = amendmentDate;
    }

    public ZonedDateTime getValidationDate() {
        return validationDate;
    }

    public void setValidationDate(final ZonedDateTime validationDate) {
        this.validationDate = validationDate;
    }

    public AmendmentReason getAmendmentReason() {
        return amendmentReason;
    }

    public void setAmendmentReason(final AmendmentReason amendmentReason) {
        this.amendmentReason = amendmentReason;
    }

    public List<ResultPrompt> getResultPromptsRecord() {
        return resultPromptsRecord;
    }

    public void setResultPromptsRecord(final List<ResultPrompt> resultPromptsRecord) {
        this.resultPromptsRecord = resultPromptsRecord;
    }

    public static AmendmentRecord.Builder amendmentRecord() {
        return new AmendmentRecord.Builder();
    }

    public static class Builder {
        private String amendedBy;
        private String validatedBy;
        private ZonedDateTime amendmentDate;
        private ZonedDateTime validationDate;
        private AmendmentReason amendmentReason;
        private List<ResultPrompt> resultPromptsRecord;

        public Builder withAmendedBy(final String amendedBy) {
            this.amendedBy = amendedBy;
            return this;
        }

        public Builder withValidatedBy(final String validatedBy) {
            this.validatedBy = validatedBy;
            return this;
        }

        public Builder withAmendmentDate(final ZonedDateTime amendmentDate) {
            this.amendmentDate = amendmentDate;
            return this;
        }

        public Builder withValidationDate(final ZonedDateTime validationDate) {
            this.validationDate = validationDate;
            return this;
        }

        public Builder withAmendmentReason(final AmendmentReason amendmentReason) {
            this.amendmentReason = amendmentReason;
            return this;
        }

        public Builder withResultPromptsRecord(final List<ResultPrompt> resultPromptsRecord) {
            this.resultPromptsRecord = resultPromptsRecord;
            return this;
        }

        public AmendmentRecord build() {
            return new AmendmentRecord(amendedBy, validatedBy, amendmentDate, validationDate, amendmentReason, resultPromptsRecord);
        }
    }
}