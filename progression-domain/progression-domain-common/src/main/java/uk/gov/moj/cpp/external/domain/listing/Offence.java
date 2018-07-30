package uk.gov.moj.cpp.external.domain.listing;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(NON_NULL)
public final class Offence implements Serializable {

    private static final long serialVersionUID = -7665846452603586679L;
    private UUID id;
    private String offenceCode;
    private String startDate;
    private String endDate;
    private StatementOfOffence statementOfOffence;

    @JsonCreator
    public Offence(@JsonProperty("id") final UUID id,
                   @JsonProperty("offenceCode") final String offenceCode,
                   @JsonProperty("startDate") final String startDate,
                   @JsonProperty("endDate") final String endDate,
                   @JsonProperty("statementOfOffence") final StatementOfOffence statementOfOffence) {

        this.id = id;
        this.offenceCode = offenceCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.statementOfOffence = statementOfOffence;
    }

    public UUID getId() {
        return id;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public StatementOfOffence getStatementOfOffence() {
        return statementOfOffence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Offence)) {
            return false;
        }

        final Offence offence = (Offence) o;

        if (id != null ? !id.equals(offence.id) : offence.id != null) {
            return false;
        }
        if (offenceCode != null ? !offenceCode.equals(offence.offenceCode) : offence.offenceCode != null) {
            return false;
        }
        if (startDate != null ? !startDate.equals(offence.startDate) : offence.startDate != null) {
            return false;
        }
        if (endDate != null ? !endDate.equals(offence.endDate) : offence.endDate != null) {
            return false;
        }
        return statementOfOffence != null ? statementOfOffence.equals(offence.statementOfOffence) : offence.statementOfOffence == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (offenceCode != null ? offenceCode.hashCode() : 0);
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
        result = 31 * result + (statementOfOffence != null ? statementOfOffence.hashCode() : 0);
        return result;
    }
}
