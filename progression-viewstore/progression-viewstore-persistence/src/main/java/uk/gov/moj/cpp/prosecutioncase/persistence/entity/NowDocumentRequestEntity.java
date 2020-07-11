package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "now_document_request")
public class NowDocumentRequestEntity implements Serializable {
    private static final long serialVersionUID = 2774561198743459041L;

    @Id
    @Column(name = "material_id", unique = true, nullable = false)
    private UUID materialId;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "hearing_id")
    private UUID hearingId;

    @Column(name = "payload")
    private String payload;

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(UUID materialId) {
        this.materialId = materialId;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(UUID hearingId) {
        this.hearingId = hearingId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
