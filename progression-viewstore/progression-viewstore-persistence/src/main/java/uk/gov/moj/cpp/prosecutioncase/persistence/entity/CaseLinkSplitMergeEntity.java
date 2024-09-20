package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import uk.gov.moj.cpp.progression.domain.event.link.LinkType;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "case_link_split_merge")
public class CaseLinkSplitMergeEntity implements Serializable {
    private static final long serialVersionUID = 1924425651577045815L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "linked_case_id")
    private UUID linkedCaseId;

    @Column(name = "reference")
    private String reference;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private LinkType type;

    @Column(name = "link_group_id")
    private UUID linkGroupId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "linked_case_id", insertable = false, updatable = false, referencedColumnName = "id")
    private ProsecutionCaseEntity linkedCase;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getLinkedCaseId() {
        return linkedCaseId;
    }

    public void setLinkedCaseId(final UUID linkedCaseId) {
        this.linkedCaseId = linkedCaseId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(final String reference) {
        this.reference = reference;
    }

    public LinkType getType() {
        return type;
    }

    public void setType(final LinkType type) {
        this.type = type;
    }

    public UUID getLinkGroupId() {
        return linkGroupId;
    }

    public void setLinkGroupId(final UUID linkGroupId) {
        this.linkGroupId = linkGroupId;
    }

    public ProsecutionCaseEntity getLinkedCase() {
        return linkedCase;
    }

    public void setLinkedCase(final ProsecutionCaseEntity linkedCase) {
        this.linkedCase = linkedCase;
    }
}
