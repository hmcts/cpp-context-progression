package uk.gov.moj.cpp.progression.query.view.response;

import java.util.UUID;

/**
 * Created by jchondig on 04/12/2017.
 */
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

    public IndicatedPleaView(UUID id, String value, String allocationDecision) {
        this.id = id;
        this.value = value;
        this.allocationDecision = allocationDecision;
    }
}
