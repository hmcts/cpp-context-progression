package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cotr_details")
public class COTRDetailsEntity implements Serializable {

    private static final long serialVersionUID = 8134469472865L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "hearing_id", nullable = false)
    private UUID hearingId;

    @Column(name = "prosecution_case_id", nullable = false)
    private UUID prosecutionCaseId;

    @Column(name = "is_archived")
    private Boolean isArchived;

    @Column(name = "prosecution_form_data", nullable = false)
    private String prosecutionFormData;

    @Column(name = "case_progression_review_note", nullable = false)
    private String caseProgressionReviewNote;

    @Column(name = "listing_review_notes", nullable = false)
    private String listingReviewNotes;

    @Column(name = "judge_review_notes", nullable = false)
    private String judgeReviewNotes;

    public COTRDetailsEntity(final UUID id,
                             final UUID hearingId,
                             final UUID prosecutionCaseId,
                             final Boolean isArchived,
                             final String prosecutionFormData,
                             final String caseProgressionReviewNote,
                             final String listingReviewNotes,
                             final String judgeReviewNotes) {
        this.id = id;
        this.hearingId = hearingId;
        this.prosecutionCaseId = prosecutionCaseId;
        this.isArchived = isArchived;
        this.prosecutionFormData = prosecutionFormData;
        this.caseProgressionReviewNote = caseProgressionReviewNote;
        this.listingReviewNotes = listingReviewNotes;
        this.judgeReviewNotes = judgeReviewNotes;
    }

    public COTRDetailsEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(UUID hearingId) {
        this.hearingId = hearingId;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public void setProsecutionCaseId(UUID prosecutionCaseId) {
        this.prosecutionCaseId = prosecutionCaseId;
    }

    public Boolean getArchived() {
        return isArchived;
    }

    public void setArchived(Boolean archived) {
        isArchived = archived;
    }

    public String getProsecutionFormData() {
        return prosecutionFormData;
    }

    public void setProsecutionFormData(String prosecutionFormData) {
        this.prosecutionFormData = prosecutionFormData;
    }

    public String getCaseProgressionReviewNote() {
        return caseProgressionReviewNote;
    }

    public void setCaseProgressionReviewNote(String caseProgressionReviewNote) {
        this.caseProgressionReviewNote = caseProgressionReviewNote;
    }

    public String getListingReviewNotes() {
        return listingReviewNotes;
    }

    public void setListingReviewNotes(String listingReviewNotes) {
        this.listingReviewNotes = listingReviewNotes;
    }

    public String getJudgeReviewNotes() {
        return judgeReviewNotes;
    }

    public void setJudgeReviewNotes(String judgeReviewNotes) {
        this.judgeReviewNotes = judgeReviewNotes;
    }
}
