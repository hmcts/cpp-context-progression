package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "informant_register")
public class InformantRegisterEntity implements Serializable {
    private static final long serialVersionUID = 2774561198743459041L;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "register_date")
    private LocalDate registerDate;

    @Column(name = "register_time")
    private ZonedDateTime registerTime;

    @Column(name = "prosecution_authority_id")
    private UUID prosecutionAuthorityId;

    @Column(name = "prosecution_authority_code")
    private String prosecutionAuthorityCode;

    @Column(name = "payload")
    private String payload;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RegisterStatus status;

    @Column(name = "processed_on")
    private ZonedDateTime processedOn;

    @Column(name = "generated_date")
    private LocalDate generatedDate;

    @Column(name = "generated_time")
    private ZonedDateTime generatedTime;

    @Column(name = "hearing_id")
    private UUID hearingId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(LocalDate registerDate) {
        this.registerDate = registerDate;
    }

    public UUID getProsecutionAuthorityId() {
        return prosecutionAuthorityId;
    }

    public void setProsecutionAuthorityId(UUID prosecutionAuthorityId) {
        this.prosecutionAuthorityId = prosecutionAuthorityId;
    }

    public String getProsecutionAuthorityCode() {
        return prosecutionAuthorityCode;
    }

    public void setProsecutionAuthorityCode(String prosecutionAuthorityCode) {
        this.prosecutionAuthorityCode = prosecutionAuthorityCode;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public RegisterStatus getStatus() {
        return status;
    }

    public void setStatus(RegisterStatus status) {
        this.status = status;
    }

    public ZonedDateTime getProcessedOn() {
        return processedOn;
    }

    public void setProcessedOn(ZonedDateTime processedOn) {
        this.processedOn = processedOn;
    }

    public ZonedDateTime getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(ZonedDateTime registerTime) {
        this.registerTime = registerTime;
    }

    public LocalDate getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(LocalDate generatedDate) {
        this.generatedDate = generatedDate;
    }

    public ZonedDateTime getGeneratedTime(ZonedDateTime generatedTime) {
        return generatedTime;
    }

    public void setGeneratedTime(ZonedDateTime generatedTime) {
        this.generatedTime = generatedTime;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(UUID hearingId) {
        this.hearingId = hearingId;
    }
}
