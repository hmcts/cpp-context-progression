package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import uk.gov.moj.cpp.progression.domain.pojo.Status;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import static javax.persistence.EnumType.STRING;

@Entity
@Table(name = "direction")
@SuppressWarnings({"squid:S1186"})
public class DirectionEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "direction_ref_data_id", nullable = false)
    private UUID directionRefDataId;

    @Column(name = "direction_ref_data_type")
    private UUID directionRefDataType;

    @Column(name = "direction_ref_data_name")
    private UUID directionRefDataName;

    @Column(name = "hearing_id")
    private UUID hearingId;

    @Column(name = "form_id")
    private UUID formId;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "trial_date")
    private LocalDate trialDate;

    @Column(name = "payload")
    private String payload;

    @Column(name = "assignee_reference")
    private UUID assigneeReference;

    @Column(name = "assignee_organisation_text")
    private String assigneeOrganisationText;

    @Column(name = "text")
    private String text;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status")
    @Enumerated(STRING)
    private Status status;

    @Column(name = "hearing_type")
    private String hearingType;

    @Column(name = "source")
    private String source;

    @Column(name = "variation_type")
    private String variationType;

    @Column(name = "send_reminder")
    private Boolean sendReminder;

    @Column(name = "case_urn")
    private String caseURN;

    @Column(name = "introductory_note")
    private String introductoryNote;

    @Column(name = "hearing_date_time")
    private ZonedDateTime hearingDateTime;

    @Column(name = "notification_status")
    private String notificationStatus;

    @Column(name = "is_notification_sent")
    private Boolean isNotificationSent;

    public DirectionEntity() {
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(final LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(final LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public String getHearingType() {
        return hearingType;
    }

    public void setHearingType(final String hearingType) {
        this.hearingType = hearingType;
    }

    public UUID getFormId() {
        return formId;
    }

    public DirectionEntity setFormId(final UUID formId) {
        this.formId = formId;
        return this;
    }

    public LocalDate getTrialDate() {
        return trialDate;
    }

    public DirectionEntity setTrialDate(final LocalDate trialDate) {
        this.trialDate = trialDate;
        return this;
    }

    public ZonedDateTime getHearingDateTime() {
        return hearingDateTime;
    }

    public DirectionEntity setHearingDateTime(final ZonedDateTime hearingDateTime) {
        this.hearingDateTime = hearingDateTime;
        return this;
    }

    public UUID getDirectionRefDataId() {
        return directionRefDataId;
    }

    public DirectionEntity setDirectionRefDataId(final UUID directionRefDataId) {
        this.directionRefDataId = directionRefDataId;
        return this;
    }

    public UUID getDirectionRefDataType() {
        return directionRefDataType;
    }

    public DirectionEntity setDirectionRefDataType(final UUID directionRefDataType) {
        this.directionRefDataType = directionRefDataType;
        return this;
    }

    public UUID getDirectionRefDataName() {
        return directionRefDataName;
    }

    public DirectionEntity setDirectionRefDataName(final UUID directionRefDataName) {
        this.directionRefDataName = directionRefDataName;
        return this;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public DirectionEntity setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
        return this;
    }

    public UUID getAssigneeReference() {
        return assigneeReference;
    }

    public DirectionEntity setAssigneeReference(final UUID assigneeReference) {
        this.assigneeReference = assigneeReference;
        return this;
    }

    public String getAssigneeOrganisationText() {
        return assigneeOrganisationText;
    }

    public DirectionEntity setAssigneeOrganisationText(final String assigneeOrganisationText) {
        this.assigneeOrganisationText = assigneeOrganisationText;
        return this;
    }

    public String getText() {
        return text;
    }

    public DirectionEntity setText(final String text) {
        this.text = text;
        return this;
    }

    public String getSource() {
        return source;
    }

    public DirectionEntity setSource(final String source) {
        this.source = source;
        return this;
    }

    public String getVariationType() {
        return variationType;
    }

    public DirectionEntity setVariationType(final String variationType) {
        this.variationType = variationType;
        return this;
    }

    public Boolean isSendReminder() {
        return sendReminder;
    }

    public DirectionEntity setSendReminder(final Boolean sendReminder) {
        this.sendReminder = sendReminder;
        return this;
    }

    public String getCaseURN() {
        return caseURN;
    }

    public void setCaseURN(final String caseURN) {
        this.caseURN = caseURN;
    }

    public String getIntroductoryNote() {
        return introductoryNote;
    }

    public void setIntroductoryNote(final String introductoryNote) {
        this.introductoryNote = introductoryNote;
    }

    public String getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(final String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public Boolean getIsNotificationSent() {
        return isNotificationSent;
    }

    public void setIsNotificationSent(final Boolean isNotificationSent) {
        this.isNotificationSent = isNotificationSent;
    }
}
