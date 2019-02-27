package uk.gov.moj.cpp.progression.query.view.response;

import java.util.UUID;

/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings({"squid:S1133", "squid:S1213"})
@Deprecated
public class IndicatedPleaView {

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

    public IndicatedPleaView(final UUID id, final String value, final String allocationDecision) {
        this.id = id;
        this.value = value;
        this.allocationDecision = allocationDecision;
    }
}
