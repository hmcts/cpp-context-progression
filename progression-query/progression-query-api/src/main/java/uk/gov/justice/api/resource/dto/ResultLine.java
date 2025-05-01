package uk.gov.justice.api.resource.dto;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultLine {

    private UUID caseId;
    private UUID defendantId;
    private UUID offenceId;
    private UUID applicationId;
    private ZonedDateTime amendmentDate;
    private ZonedDateTime sharedDate;
    private LocalDate orderedDate;

    private UUID resultLineId;
    private UUID resultDefinitionId;
    private String shortCode;
    private String resultLevel;
    private Boolean deleted;
    private Boolean valid;
    private AmendmentReason amendmentReason;
    private AmendmentLog amendmentsLog;

    private List<ResultPrompt> resultPrompts;

    @JsonCreator
    public ResultLine(final UUID caseId, final UUID defendantId, final UUID offenceId, final UUID applicationId,
                      final ZonedDateTime amendmentDate, final ZonedDateTime sharedDate, final LocalDate orderedDate,
                      final UUID resultLineId, final UUID resultDefinitionId, final String shortCode, final String resultLevel,
                      final Boolean deleted, final Boolean valid, final AmendmentReason amendmentReason,
                      final AmendmentLog amendmentsLog, final List<ResultPrompt> resultPrompts) {

        this.caseId = caseId;
        this.defendantId = defendantId;
        this.offenceId = offenceId;
        this.applicationId = applicationId;
        this.amendmentDate = amendmentDate;
        this.sharedDate = sharedDate;
        this.orderedDate = orderedDate;
        this.resultLineId = resultLineId;
        this.resultDefinitionId = resultDefinitionId;
        this.deleted = deleted;
        this.valid = valid;
        this.shortCode = shortCode;
        this.resultLevel = resultLevel;
        this.amendmentReason = amendmentReason;
        this.amendmentsLog = amendmentsLog;
        this.resultPrompts = resultPrompts;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public void setOffenceId(final UUID offenceId) {
        this.offenceId = offenceId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final UUID applicationId) {
        this.applicationId = applicationId;
    }

    public ZonedDateTime getAmendmentDate() {
        return amendmentDate;
    }

    public void setAmendmentDate(final ZonedDateTime amendmentDate) {
        this.amendmentDate = amendmentDate;
    }

    public LocalDate getOrderedDate() {
        return orderedDate;
    }

    public void setOrderedDate(final LocalDate orderedDate) {
        this.orderedDate = orderedDate;
    }

    public ZonedDateTime getSharedDate() {
        return sharedDate;
    }

    public void setSharedDate(final ZonedDateTime sharedDate) {
        this.sharedDate = sharedDate;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(final String shortCode) {
        this.shortCode = shortCode;
    }

    public String getResultLevel() {
        return resultLevel;
    }

    public void setResultLevel(final String resultLevel) {
        this.resultLevel = resultLevel;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(final Boolean valid) {
        this.valid = valid;
    }

    public UUID getResultDefinitionId() {
        return resultDefinitionId;
    }

    public void setResultDefinitionId(final UUID resultDefinitionId) {
        this.resultDefinitionId = resultDefinitionId;
    }

    public UUID getResultLineId() {
        return resultLineId;
    }

    public void setResultLineId(final UUID resultLineId) {
        this.resultLineId = resultLineId;
    }

    public AmendmentReason getAmendmentReason() {
        return amendmentReason;
    }

    public void setAmendmentReason(final AmendmentReason amendmentReason) {
        this.amendmentReason = amendmentReason;
    }

    public AmendmentLog getAmendmentsLog() {
        return amendmentsLog;
    }

    public void setAmendmentsLog(final AmendmentLog amendmentsLog) {
        this.amendmentsLog = amendmentsLog;
    }

    public List<ResultPrompt> getResultPrompts() {
        return resultPrompts;
    }

    public void setResultPrompts(final List<ResultPrompt> resultPrompts) {
        this.resultPrompts = resultPrompts;
    }

    public static Builder resultLine() {
        return new ResultLine.Builder();
    }

    public static class Builder {
        private UUID caseId;
        private UUID defendantId;
        private UUID offenceId;
        private UUID applicationId;
        private ZonedDateTime amendmentDate;
        private ZonedDateTime sharedDate;
        private LocalDate orderedDate;
        private UUID resultLineId;
        private UUID resultDefinitionId;
        private String shortCode;
        private String resultLevel;
        private Boolean deleted;
        private Boolean valid;
        private AmendmentReason amendmentReason;
        private AmendmentLog amendmentsLog;
        private List<ResultPrompt> resultPrompts;

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withDefendantId(final UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public Builder withOffenceId(final UUID offenceId) {
            this.offenceId = offenceId;
            return this;
        }

        public Builder withApplicationId(final UUID applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public Builder withAmendmentDate(final ZonedDateTime amendmentDate) {
            this.amendmentDate = amendmentDate;
            return this;
        }

        public Builder withSharedDate(final ZonedDateTime sharedDate) {
            this.sharedDate = sharedDate;
            return this;
        }

        public Builder withOrderedDate(final LocalDate orderedDate) {
            this.orderedDate = orderedDate;
            return this;
        }

        public Builder withResultLineId(final UUID resultLineId) {
            this.resultLineId = resultLineId;
            return this;
        }

        public Builder withResultDefinitionId(final UUID resultDefinitionId) {
            this.resultDefinitionId = resultDefinitionId;
            return this;
        }

        public Builder withShortCode(final String shortCode) {
            this.shortCode = shortCode;
            return this;
        }

        public Builder withResultLevel(final String resultLevel) {
            this.resultLevel = resultLevel;
            return this;
        }

        public Builder withDeleted(final Boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder withValid(final Boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder withAmendmentReason(final AmendmentReason amendmentReason) {
            this.amendmentReason = amendmentReason;
            return this;
        }

        public Builder withAmendmentLog(final AmendmentLog amendmentsLog) {
            this.amendmentsLog = amendmentsLog;
            return this;
        }

        public Builder withResultPrompts(final List<ResultPrompt> resultPrompts) {
            this.resultPrompts = resultPrompts;
            return this;
        }

        public ResultLine build() {
            return new ResultLine(caseId, defendantId, offenceId, applicationId, amendmentDate, sharedDate, orderedDate, resultLineId,
                    resultDefinitionId, shortCode, resultLevel, deleted, valid, amendmentReason, amendmentsLog, resultPrompts);
        }
    }
}
