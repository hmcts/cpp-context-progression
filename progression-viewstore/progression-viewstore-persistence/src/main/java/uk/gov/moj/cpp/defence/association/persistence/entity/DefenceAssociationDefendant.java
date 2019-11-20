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
public class DefenceAssociationDefendant implements Serializable {

    @Id
    @Column(name = "defendant_id", unique = true, nullable = false)
    private UUID defendantId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "defenceAssociationDefendant", orphanRemoval = true)
    private Set<DefenceAssociation> defenceAssociations = new HashSet<>();

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public Set<DefenceAssociation> getDefenceAssociations() {
        return defenceAssociations;
    }

    public void setDefenceAssociations(final Set<DefenceAssociation> defenceAssociations) {
        this.defenceAssociations = defenceAssociations;
    }
}
