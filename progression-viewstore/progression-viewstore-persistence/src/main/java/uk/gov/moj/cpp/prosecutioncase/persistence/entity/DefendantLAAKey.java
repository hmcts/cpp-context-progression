package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class DefendantLAAKey  implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "defendant_id", nullable = false)
    private UUID defendantId;

    @Column(name = "laa_contract_number", nullable = false)
    private String laaContractNumber;

    public DefendantLAAKey() {

    }

    public DefendantLAAKey(final UUID defendantId, final String laaContractNumber) {
        this.defendantId = defendantId;
        this.laaContractNumber = laaContractNumber;
    }


    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public String getLaaContractNumber() {
        return laaContractNumber;
    }

    public void setLaaContractNumber(String laaContractNumber) {
        this.laaContractNumber = laaContractNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefendantLAAKey that = (DefendantLAAKey) o;
        return Objects.equals(defendantId, that.defendantId) &&
                Objects.equals(laaContractNumber, that.laaContractNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defendantId, laaContractNumber);
    }

    @Override
    public String toString() {
        return "DefendantLAAKey{" +
                "defendantId=" + defendantId +
                ", laaContractNumber='" + laaContractNumber + '\'' +
                '}';
    }
}
