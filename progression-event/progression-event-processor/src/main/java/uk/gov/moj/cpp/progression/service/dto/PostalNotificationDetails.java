package uk.gov.moj.cpp.progression.service.dto;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;

import java.time.LocalDate;
import java.util.UUID;

public class PostalNotificationDetails {

    private String hearingDate;
    private String hearingTime;
    private String applicationReference;
    private String applicationType;
    private String applicationTypeWelsh;
    private String legislation;
    private String legislationWelsh;
    private CourtCentre courtCentre;
    private CourtApplicationParty courtApplicationParty;
    private JurisdictionType jurisdictionType;
    private String applicationParticulars;
    private CourtApplication courtApplication;
    private Boolean isAmended;
    private UUID notificationId;
    private Boolean isWelTranslationRequired;
    private LocalDate issueDate;


    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(final UUID notificationId) {
        this.notificationId = notificationId;
    }

    public Boolean getWelTranslationRequired() {
        return isWelTranslationRequired;
    }

    public void setWelTranslationRequired(final Boolean welTranslationRequired) {
        isWelTranslationRequired = welTranslationRequired;
    }

    public String getHearingDate() {
        return hearingDate;
    }

    public void setHearingDate(final String hearingDate) {
        this.hearingDate = hearingDate;
    }

    public String getHearingTime() {
        return hearingTime;
    }

    public void setHearingTime(final String hearingTime) {
        this.hearingTime = hearingTime;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public void setApplicationReference(final String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(final String applicationType) {
        this.applicationType = applicationType;
    }

    public String getApplicationTypeWelsh() {
        return applicationTypeWelsh;
    }

    public void setApplicationTypeWelsh(final String applicationTypeWelsh) {
        this.applicationTypeWelsh = applicationTypeWelsh;
    }

    public String getLegislation() {
        return legislation;
    }

    public void setLegislation(final String legislation) {
        this.legislation = legislation;
    }

    public String getLegislationWelsh() {
        return legislationWelsh;
    }

    public void setLegislationWelsh(final String legislationWelsh) {
        this.legislationWelsh = legislationWelsh;
    }

    public CourtCentre getCourtCentre() {
        return courtCentre;
    }

    public void setCourtCentre(final CourtCentre courtCentre) {
        this.courtCentre = courtCentre;
    }

    public CourtApplicationParty getCourtApplicationParty() {
        return courtApplicationParty;
    }

    public void setCourtApplicationParty(final CourtApplicationParty courtApplicationParty) {
        this.courtApplicationParty = courtApplicationParty;
    }

    public JurisdictionType getJurisdictionType() {
        return jurisdictionType;
    }

    public void setJurisdictionType(final JurisdictionType jurisdictionType) {
        this.jurisdictionType = jurisdictionType;
    }

    public String getApplicationParticulars() {
        return applicationParticulars;
    }

    public void setApplicationParticulars(final String applicationParticulars) {
        this.applicationParticulars = applicationParticulars;
    }

    public CourtApplication getCourtApplication() {
        return courtApplication;
    }

    public void setCourtApplication(final CourtApplication courtApplication) {
        this.courtApplication = courtApplication;
    }

    public Boolean getAmended() {
        return isAmended;
    }

    public void setAmended(final Boolean amended) {
        isAmended = amended;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(final LocalDate issueDate) {
        this.issueDate = issueDate;
    }
}
