package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cps_send_notification")

public class CpsSendNotificationEntity implements Serializable {

    private static final long serialVersionUID = 8137449412665L;

    @Id
    @Column(name = "court_document_id", unique = true, nullable = false)
    private UUID courtDocumentId;

    @Column(name = "send_to_cps", nullable = false)
    private Boolean sendToCps;

    public UUID getCourtDocumentId() {
        return courtDocumentId;
    }

    public void setCourtDocumentId(final UUID courtDocumentId) {
        this.courtDocumentId = courtDocumentId;
    }

    public Boolean getSendToCps() {
        return sendToCps;
    }

    public void setSendToCps(final Boolean sendToCps) {
        this.sendToCps = sendToCps;
    }
}
