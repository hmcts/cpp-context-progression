package uk.gov.moj.cpp.progression.domain.event;

import java.io.Serializable;
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
@Event("progression.events.offence-conviction-date-removed")
public class ConvictionDateRemoved implements Serializable {

    private static final long serialVersionUID = -8315031669137332594L;
    
    private final UUID caseId;
    private final UUID offenceId;

    @JsonCreator
    public ConvictionDateRemoved(@JsonProperty("caseId") final UUID caseId, 
            @JsonProperty("offenceId") final UUID offenceId) {
        this.caseId = caseId;
        this.offenceId = offenceId;
    }

    @JsonIgnore
    private ConvictionDateRemoved(final Builder builder) {
        this.caseId = builder.caseId;
        this.offenceId = builder.offenceId;
    }

    public UUID getCaseId() {
        return caseId;
    }
    
    public UUID getOffenceId() {
        return offenceId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private UUID caseId;
        private UUID offenceId;

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }
        
        public Builder withOffenceId(final UUID offenceId) {
            this.offenceId = offenceId;
            return this;
        }

        public ConvictionDateRemoved build() {
            return new ConvictionDateRemoved(this);
        }
    }
}
