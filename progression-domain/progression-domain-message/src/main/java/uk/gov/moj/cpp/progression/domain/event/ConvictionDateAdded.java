package uk.gov.moj.cpp.progression.domain.event;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.justice.domain.annotation.Event;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@Event("progression.events.offence-conviction-date-changed")
public class ConvictionDateAdded implements Serializable {

    private static final long serialVersionUID = -7186949966428456562L;

    private final UUID caseId;
    private final UUID offenceId;
    private final LocalDate convictionDate;

    @JsonCreator
    public ConvictionDateAdded(@JsonProperty("caseId") final UUID caseId, 
            @JsonProperty("offenceId") final UUID offenceId,
            @JsonProperty("convictionDate") final LocalDate convictionDate) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.convictionDate = convictionDate;
    }

    @JsonIgnore
    private ConvictionDateAdded(final Builder builder) {
        this.caseId = builder.caseId;
        this.offenceId = builder.offenceId;
        this.convictionDate = builder.convictionDate;
    }

    public UUID getCaseId() {
        return caseId;
    }
    
    public UUID getOffenceId() {
        return offenceId;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
        private UUID caseId;
        private UUID offenceId;
        private LocalDate convictionDate;

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }
        
        public Builder withOffenceId(final UUID offenceId) {
            this.offenceId = offenceId;
            return this;
        }

        public Builder withConvictionDate(final LocalDate convictionDate) {
            this.convictionDate = convictionDate;
            return this;
        }

        public ConvictionDateAdded build() {
            return new ConvictionDateAdded(this);
        }
    }
}
