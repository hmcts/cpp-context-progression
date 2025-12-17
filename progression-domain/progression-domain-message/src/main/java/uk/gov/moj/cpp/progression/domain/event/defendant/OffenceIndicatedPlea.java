package uk.gov.moj.cpp.progression.domain.event.defendant;


import java.io.Serializable;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class OffenceIndicatedPlea implements Serializable {

    private static final long serialVersionUID = 182447333590503939L;

    private final UUID id;

    private final String value;

    private final String allocationDecision;

    public UUID getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public String getAllocationDecision() {
        return allocationDecision;
    }

    public OffenceIndicatedPlea(final UUID id, final String value, final String allocationDecision) {
        this.id = id;
        this.value = value;
        this.allocationDecision = allocationDecision;
    }
}
