package uk.gov.moj.cpp.defence.association.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "defence_association")
public class DefenceAssociation implements Serializable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "defendant_id", nullable = false)
    private DefenceAssociationDefendant defenceAssociationDefendant;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "org_id")
    private UUID orgId;

    @Column(name = "description")
    private String description;

    @Column(name = "start_date")
    private ZonedDateTime startDate;

    @Column(name = "end_date")
    private ZonedDateTime endDate;

    @Column(name = "representation_type")
    private String representationType;

    @Column(name = "laa_contract_number")
    private String laaContractNumber;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }


    public DefenceAssociationDefendant getDefenceAssociationDefendant() {
        return defenceAssociationDefendant;
    }

    public void setDefenceAssociationDefendant(final DefenceAssociationDefendant defenceAssociationDefendant) {
        this.defenceAssociationDefendant = defenceAssociationDefendant;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(final UUID orgId) {
        this.orgId = orgId;
    }

    public String getActionDescription() {
        return this.description;
    }

    public void setActionDescription(final String description) {
        this.description = description;
    }

    public ZonedDateTime getStartDate() {
        return this.startDate;
    }

    public void setStartDate(final ZonedDateTime startDate) {
        this.startDate = startDate;
    }

    public ZonedDateTime getEndDate() {
        return this.endDate;
    }

    public void setEndDate(final ZonedDateTime endDate) {
        this.endDate = endDate;
    }

    public String getRepresentationType() {
        return representationType;
    }

    public void setRepresentationType(final String representationType) {
        this.representationType = representationType;
    }

    public String getLaaContractNumber() {
        return laaContractNumber;
    }

    public void setLaaContractNumber(final String laaContractNumber) {
        this.laaContractNumber = laaContractNumber;
    }
}
