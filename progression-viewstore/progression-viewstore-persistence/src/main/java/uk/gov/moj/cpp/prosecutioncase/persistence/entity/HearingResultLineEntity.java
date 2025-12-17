package uk.gov.moj.cpp.prosecutioncase.persistence.entity;


import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@SuppressWarnings("squid:S00116")
@Entity
@Table(name = "hearing_result_line")
public class HearingResultLineEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID Id;

    @Column(name = "payload")
    private String payload;

    @ManyToOne
    @JoinColumn(name = "hearing_id")
    private HearingEntity hearing;

    public HearingResultLineEntity(final UUID id, final String payload, final HearingEntity hearing) {
        Id = id;
        this.payload = payload;
        this.hearing = hearing;
    }
    public HearingResultLineEntity() {
    }

    public UUID getId() {
        return Id;
    }

    public void setId(final UUID id) {
        Id = id;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    public HearingEntity getHearing() {
        return hearing;
    }

    public void setHearing(final HearingEntity hearing) {
        this.hearing = hearing;
    }
}
