package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "defendant_request")
public class DefendantRequestEntity implements Serializable {

    private static final long serialVersionUID = 8137449412665L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID defendantId;

    @Column(name = "prosecution_case_id")
    private UUID prosecutionCaseId;

    @Column(name = "payload")
    private String payload;

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public String getPayload() {
        return payload;
    }

    public void setProsecutionCaseId(final UUID prosecutionCaseId) {
        this.prosecutionCaseId = prosecutionCaseId;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }


}
