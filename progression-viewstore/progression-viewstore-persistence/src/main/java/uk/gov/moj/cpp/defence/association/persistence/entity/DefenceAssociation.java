package uk.gov.moj.cpp.defence.association.persistence.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "defence_association_defendant")
@SuppressWarnings("squid:S2384")
public class DefenceAssociation implements Serializable {

    @Id
    @Column(name = "defendant_id", unique = true, nullable = false)
    private UUID defendantId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "defenceAssociation", orphanRemoval = true)
    private Set<DefenceAssociationHistory> defenceAssociationHistories = new HashSet<>();

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public Set<DefenceAssociationHistory> getDefenceAssociationHistories() {
        return defenceAssociationHistories;
    }

    public void setDefenceAssociationHistories(Set<DefenceAssociationHistory> defenceAssociationHistories) {
        this.defenceAssociationHistories = defenceAssociationHistories;
    }
}
