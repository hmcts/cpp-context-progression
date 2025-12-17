package uk.gov.moj.cpp.prosecutioncase.persistence.entity;


import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;

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
@Table(name = "court_register_request")
public class CourtRegisterRequestEntity {

    @Id
    @Column(name = "court_register_request_id")
    private UUID courtRegisterRequestId;

    @Column(name = "court_centre_id")
    private UUID courtCentreId;

    @Column(name = "system_doc_generator_id")
    private UUID systemDocGeneratorId;

    @Column(name = "register_date")
    private LocalDate registerDate;

    @Column(name = "register_time")
    private ZonedDateTime registerTime;

    @Column(name = "payload")
    private String payload;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RegisterStatus status;

    @Column(name = "processed_on")
    private ZonedDateTime processedOn;

    @Column(name = "court_house")
    private String courtHouse;

    @Column(name = "generated_date")
    private LocalDate generatedDate;

    @Column(name = "generated_time")
    private ZonedDateTime generatedTime;

    @Column(name = "hearing_id")
    private UUID hearingId;

    public UUID getSystemDocGeneratorId() {
        return systemDocGeneratorId;
    }

    public void setSystemDocGeneratorId(UUID systemDocGeneratorId) {
        this.systemDocGeneratorId = systemDocGeneratorId;
    }

    public UUID getCourtRegisterRequestId() {
        return courtRegisterRequestId;
    }

    public void setCourtRegisterRequestId(UUID courtRegisterRequestId) {
        this.courtRegisterRequestId = courtRegisterRequestId;
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

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public void setCourtCentreId(UUID courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getCourtHouse() {
        return courtHouse;
    }

    public void setCourtHouse(String courtHouse) {
        this.courtHouse = courtHouse;
    }

    public LocalDate getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(LocalDate registerDate) {
        this.registerDate = registerDate;
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

    public ZonedDateTime getGeneratedTime() {
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
