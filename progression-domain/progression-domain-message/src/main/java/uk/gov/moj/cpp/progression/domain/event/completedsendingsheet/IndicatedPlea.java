package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import java.io.Serializable;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class IndicatedPlea implements Serializable {

    private static final long serialVersionUID = -6924534111241575998L;
    private final UUID id;
    private final String value;
    private final String allocationDecision;

    public IndicatedPlea(final UUID id, final String value, final String allocationDecision) {
        this.id = id;
        this.value = value;
        this.allocationDecision = allocationDecision;
    }

    public UUID getId() {
        return this.id;
    }

    public String getValue() {
        return this.value;
    }

    public String getAllocationDecision() {
        return this.allocationDecision;
    }

}
