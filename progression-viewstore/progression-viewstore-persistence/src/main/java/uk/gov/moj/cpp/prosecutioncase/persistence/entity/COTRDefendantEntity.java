package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cotr_defendant")
public class COTRDefendantEntity implements Serializable {

    private static final long serialVersionUID = 2421761778232204986L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "cotr_id", nullable = false)
    private UUID cotrId;

    @Column(name = "defendant_id", nullable = false)
    private UUID defendantId;

    @Column(name = "d_number", nullable = false)
    private Integer dNumber;

    @Column(name = "defendant_form", nullable = false)
    private String defendantForm;

    @Column(name = "served_by", nullable = false)
    private UUID servedBy;

    @Column(name = "served_by_name", nullable = false)
    private String servedByName;

    @Column(name = "served_on", nullable = false)
    private ZonedDateTime servedOn;

    public COTRDefendantEntity(UUID id, UUID cotrId, UUID defendantId, Integer dNumber, String defendantForm, UUID servedBy, ZonedDateTime servedOn, String servedByName) {
        this.id = id;
        this.cotrId = cotrId;
        this.defendantId = defendantId;
        this.dNumber = dNumber;
        this.defendantForm = defendantForm;
        this.servedBy = servedBy;
        this.servedOn = servedOn;
        this.servedByName=servedByName;
    }

    public COTRDefendantEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCotrId() {
        return cotrId;
    }

    public void setCotrId(UUID cotrId) {
        this.cotrId = cotrId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public Integer getdNumber() {
        return dNumber;
    }

    public void setdNumber(Integer dNumber) {
        this.dNumber = dNumber;
    }

    public String getDefendantForm() {
        return defendantForm;
    }

    public void setDefendantForm(String defendantForm) {
        this.defendantForm = defendantForm;
    }

    public UUID getServedBy() {
        return servedBy;
    }

    public void setServedBy(UUID servedBy) {
        this.servedBy = servedBy;
    }

    public ZonedDateTime getServedOn() {
        return servedOn;
    }

    public void setServedOn(ZonedDateTime servedOn) {
        this.servedOn = servedOn;
    }

    public String getServedByName() {
        return servedByName;
    }

    public void setServedByName(final String servedByName) {
        this.servedByName = servedByName;
    }
}
