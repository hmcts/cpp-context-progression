package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "defendant_partial_match")
public class DefendantPartialMatchEntity implements Serializable {

    private static final long serialVersionUID = 3587270136588612472L;

    @Id
    @Column(name = "defendant_id", unique = true, nullable = false)
    private UUID defendantId;

    @Column(name = "prosecution_case_id", nullable = false)
    private UUID prosecutionCaseId;

    @Column(name = "defendant_name", nullable = false)
    private String defendantName;

    @Column(name = "case_reference", nullable = false)
    private String caseReference;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "case_received_datetime", nullable = false)
    private ZonedDateTime caseReceivedDatetime;

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public void setProsecutionCaseId(UUID prosecutionCaseId) {
        this.prosecutionCaseId = prosecutionCaseId;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public void setDefendantName(String defendantName) {
        this.defendantName = defendantName;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public ZonedDateTime getCaseReceivedDatetime() {
        return caseReceivedDatetime;
    }

    public void setCaseReceivedDatetime(ZonedDateTime caseReceivedDatetime) {
        this.caseReceivedDatetime = caseReceivedDatetime;
    }
}
