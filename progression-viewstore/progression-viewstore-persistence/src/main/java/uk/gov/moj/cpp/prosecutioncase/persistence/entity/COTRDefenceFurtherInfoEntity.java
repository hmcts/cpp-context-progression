package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cotr_defence_further_info")
public class COTRDefenceFurtherInfoEntity implements Serializable {

    private static final long serialVersionUID = -5824529789636685931L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "cotr_defendant_id", nullable = false)
    private UUID cotrDefendantId;

    @Column(name = "further_information", nullable = false)
    private String furtherInformation;

    @Column(name = "is_certification_ready", nullable = false)
    private Boolean isCertificationReady;

    @Column(name = "info_added_by", nullable = false)
    private UUID infoAddedBy;

    @Column(name = "info_added_by_name", nullable = false)
    private String infoAddedByName;

    @Column(name = "added_on", nullable = false)
    private ZonedDateTime addedOn;

    public COTRDefenceFurtherInfoEntity(
            final UUID id,
            final UUID cotrDefendantId,
            final String furtherInformation,
            final Boolean isCertificationReady,
            final UUID infoAddedBy,
            final String infoAddedByName,
            final ZonedDateTime addedOn) {
        this.id = id;
        this.cotrDefendantId = cotrDefendantId;
        this.furtherInformation = furtherInformation;
        this.isCertificationReady = isCertificationReady;
        this.infoAddedBy = infoAddedBy;
        this.infoAddedByName = infoAddedByName;
        this.addedOn = addedOn;
    }

    public COTRDefenceFurtherInfoEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCotrDefendantId() {
        return cotrDefendantId;
    }

    public void setCotrDefendant(UUID cotrDefendantId) {
        this.cotrDefendantId = cotrDefendantId;
    }

    public String getFurtherInformation() {
        return furtherInformation;
    }

    public void setFurtherInformation(String furtherInformation) {
        this.furtherInformation = furtherInformation;
    }

    public Boolean getIsCertificationReady() {
        return isCertificationReady;
    }

    public void setIsCertificationReady(Boolean isCertificationReady) {
        this.isCertificationReady = isCertificationReady;
    }


    public UUID getInfoAddedBy() {
        return infoAddedBy;
    }

    public void setInfoAddedBy(UUID infoAddedBy) {
        this.infoAddedBy = infoAddedBy;
    }

    public String getInfoAddedByName() {
        return infoAddedByName;
    }

    public void setInfoAddedByName(String infoAddedByName) {
        this.infoAddedByName = infoAddedByName;
    }

    public ZonedDateTime getAddedOn() {
        return addedOn;
    }

    public void setAddedOn(ZonedDateTime addedOn) {
        this.addedOn = addedOn;
    }
}
