package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cotr_prosecution_further_info")
public class COTRProsecutionFurtherInfoEntity implements Serializable {

    private static final long serialVersionUID = 7743473151671L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "cotr_id", nullable = false)
    private UUID cotrId;

    @Column(name = "further_information", nullable = false)
    private String furtherInformation;

    @Column(name = "info_added_by", nullable = false)
    private UUID infoAddedBy;

    @Column(name = "added_on", nullable = false)
    private ZonedDateTime addedOn;

    @Column(name = "is_certification_ready", nullable = false)
    private Boolean isCertificationReady;

    @Column(name = "info_added_by_name", nullable = false)
    private String infoAddedByName;


    public COTRProsecutionFurtherInfoEntity(UUID id, UUID cotrId, String furtherInformation, UUID infoAddedBy, ZonedDateTime addedOn, final Boolean isCertificationReady, final String infoAddedByName) {
        this.id = id;
        this.cotrId = cotrId;
        this.furtherInformation = furtherInformation;
        this.infoAddedBy = infoAddedBy;
        this.addedOn = addedOn;
        this.isCertificationReady=isCertificationReady;
        this.infoAddedByName=infoAddedByName;
    }

    public COTRProsecutionFurtherInfoEntity() {
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

    public String getFurtherInformation() {
        return furtherInformation;
    }

    public void setFurtherInformation(String furtherInformation) {
        this.furtherInformation = furtherInformation;
    }

    public UUID getInfoAddedBy() {
        return infoAddedBy;
    }

    public void setInfoAddedBy(UUID infoAddedBy) {
        this.infoAddedBy = infoAddedBy;
    }

    public ZonedDateTime getAddedOn() {
        return addedOn;
    }

    public void setAddedOn(ZonedDateTime addedOn) {
        this.addedOn = addedOn;
    }

    public Boolean getIsCertificationReady() {
        return isCertificationReady;
    }

    public void setIsCertificationReady(final Boolean isCertificationReady) {
        this.isCertificationReady = isCertificationReady;
    }

    public String getInfoAddedByName() {
        return infoAddedByName;
    }

    public void setInfoAddedByName(final String infoAddedByName) {
        this.infoAddedByName = infoAddedByName;
    }
}
