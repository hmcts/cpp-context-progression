package uk.gov.moj.cpp.progression.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Entity
@Table(name = "offence_indicated_plea")
public class OffenceIndicatedPlea implements Serializable {

    @Column(name = "id")
    @Id
    private UUID id;

    @Column(name = "value")
    private String value;

    @Column(name = "allocation_decision")
    private String allocationDecision;

    public OffenceIndicatedPlea() {

    }

    public OffenceIndicatedPlea(final UUID id, final String value, final String allocationDecision) {
        this.id = id;
        this.value = value;
        this.allocationDecision = allocationDecision;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getAllocationDecision() {
        return allocationDecision;
    }

    public void setPleaDate(final String allocationDecision) {
        this.allocationDecision = allocationDecision;
    }

}
