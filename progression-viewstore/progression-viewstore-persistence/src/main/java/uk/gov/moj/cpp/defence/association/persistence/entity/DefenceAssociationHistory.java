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
@Table(name = "defence_association_history")
public class DefenceAssociationHistory implements Serializable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "defendant_id", nullable=false)
    private DefenceAssociation defenceAssociation;

    @Column(name = "grantee_user_id")
    private UUID granteeUserId;

    @Column(name = "grantee_org_id")
    private UUID granteeOrgId;

    @Column(name = "grantor_user_id")
    private UUID grantorUserId;

    @Column(name = "grantor_org_id")
    private UUID grantorOrgId;

    @Column(name = "description")
    private String description;

    @Column(name = "start_date")
    private ZonedDateTime startDate;

    @Column(name = "end_date")
    private ZonedDateTime endDate;

    @Column(name = "agent_flag")
    private Boolean agentFlag;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DefenceAssociation getDefenceAssociation() {
        return defenceAssociation;
    }

    public void setDefenceAssociation(final DefenceAssociation defenceAssociation) {
        this.defenceAssociation = defenceAssociation;
    }

    public UUID getGranteeUserId() {
        return granteeUserId;
    }

    public void setGranteeUserId(final UUID granteeUserId) {
        this.granteeUserId = granteeUserId;
    }

    public UUID getGrantorUserId() {
        return grantorUserId;
    }

    public void setGrantorUserId(final UUID grantorUserId) {
        this.grantorUserId = grantorUserId;
    }

    public UUID getGrantorOrgId() {
        return grantorOrgId;
    }

    public void setGrantorOrgId(final UUID grantorOrgId) {
        this.grantorOrgId = grantorOrgId;
    }

    public String getActionDescription() {
        return this.description;
    }

    public void setActionDescription(final String description) {this.description = description;}

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

    public Boolean getAgentFlag() {
        return this.agentFlag;
    }

    public void setAgentFlag(final Boolean agentFlag) {

        if (null == agentFlag)  {
            this.agentFlag = false;
        } else {
            this.agentFlag = agentFlag;
        }
    }

}
