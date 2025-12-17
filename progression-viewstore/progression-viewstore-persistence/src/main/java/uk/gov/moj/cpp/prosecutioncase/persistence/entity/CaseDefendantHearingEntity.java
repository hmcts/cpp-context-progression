package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "case_defendant_hearing")
public class CaseDefendantHearingEntity implements Serializable {

    private static final long serialVersionUID = 9730445115611L;

    @Id
    private CaseDefendantHearingKey id;

    @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "hearing_id", insertable = false, updatable = false, referencedColumnName = "hearing_id")
    private HearingEntity hearing;

    public CaseDefendantHearingKey getId() {
        return id;
    }

    public void setId(CaseDefendantHearingKey id) {
        this.id = id;
    }

    public HearingEntity getHearing() {
        return hearing;
    }

    public void setHearing(HearingEntity hearing) {
        this.hearing = hearing;
    }
}
