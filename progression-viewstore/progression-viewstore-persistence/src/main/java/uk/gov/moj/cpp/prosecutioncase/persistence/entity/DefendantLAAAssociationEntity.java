package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "defendant_laa_association")
public class DefendantLAAAssociationEntity implements Serializable {

    private static final long serialVersionUID = 9730443111611L;


    @Column(name = "is_associated_by_laa",  nullable = false)
    private boolean isAssociatedByLAA;

    @Id
    private DefendantLAAKey id;

    public boolean isAssociatedByLAA() {
        return isAssociatedByLAA;
    }

    public void setAssociatedByLAA(boolean associatedByLAA) {
        isAssociatedByLAA = associatedByLAA;
    }

    public DefendantLAAKey getDefendantLAAKey() {
        return id;
    }

    public void setDefendantLAAKey(DefendantLAAKey id) {
        this.id = id;
    }



}
