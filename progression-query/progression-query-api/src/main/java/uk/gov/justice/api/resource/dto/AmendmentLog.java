package uk.gov.justice.api.resource.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AmendmentLog {

    private Boolean isAmended;
    private List<AmendmentRecord> amendmentsRecord;

    @JsonCreator
    public AmendmentLog(final Boolean isAmended, final List<AmendmentRecord> amendmentsRecord) {
        this.isAmended = isAmended;
        this.amendmentsRecord = amendmentsRecord;
    }

    public Boolean getAmended() {
        return isAmended;
    }

    public void setAmended(final Boolean amended) {
        isAmended = amended;
    }

    public List<AmendmentRecord> getAmendmentsRecord() {
        return amendmentsRecord;
    }

    public void setAmendmentsRecord(final List<AmendmentRecord> amendmentsRecord) {
        this.amendmentsRecord = amendmentsRecord;
    }

    public static Builder amendmentLog() {
        return new Builder();
    }

    public static class Builder {
        private Boolean isAmended;
        private List<AmendmentRecord> amendmentsRecord;

        public Builder withIsAmended(final Boolean isAmended) {
            this.isAmended = isAmended;
            return this;
        }

        public Builder withAmendmentsRecord(final List<AmendmentRecord> amendmentsRecord) {
            this.amendmentsRecord = amendmentsRecord;
            return this;
        }

        public AmendmentLog build() {
            return new AmendmentLog(isAmended, amendmentsRecord);
        }


    }
}
