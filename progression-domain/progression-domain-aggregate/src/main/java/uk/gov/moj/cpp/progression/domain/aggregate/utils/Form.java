package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import uk.gov.justice.core.courts.FormDefendants;
import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.core.courts.PetDefendants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Form implements Serializable {

    private static final long serialVersionUID = -2338626292552177485L;
    private UUID courtFormId; // in PET it is a petId
    private UUID submissionId;
    private FormType formType;
    private transient List<PetDefendants> petDefendants;
    private transient List<FormDefendants> formDefendants;
    private FormLockStatus formLockStatus;

    public Form() {
    }

    public Form(final List<PetDefendants> petDefendants, final UUID courtFormId, final FormType formType, final FormLockStatus formLockStatus, UUID submissionId) {
        this.courtFormId = courtFormId;
        this.formType = formType;
        this.formLockStatus = formLockStatus;
        this.petDefendants = Collections.unmodifiableList(petDefendants);
        this.submissionId = submissionId;
    }

    public Form(final List<FormDefendants> formDefendants, final UUID courtFormId, final FormType formType, final FormLockStatus formLockStatus) {
        this.formDefendants = Collections.unmodifiableList(formDefendants);
        this.courtFormId = courtFormId;
        this.formType = formType;
        this.formLockStatus = formLockStatus;
    }

    public List<FormDefendants> getFormDefendants() {
        return new ArrayList<>(formDefendants);
    }

    public List<PetDefendants> getPetFormDefendants() {
        return new ArrayList<>(petDefendants);
    }

    public void setPetDefendants(final List<PetDefendants> petFormDefendants) {
        this.petDefendants = Collections.unmodifiableList(petFormDefendants);
    }

    public void setFormDefendants(final List<FormDefendants> formDefendants) {
        this.formDefendants = Collections.unmodifiableList(formDefendants);
    }

    public UUID getCourtFormId() {
        return courtFormId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(final UUID submissionId) {
        this.submissionId = submissionId;
    }

    public void setCourtFormId(final UUID courtFormId) {
        this.courtFormId = courtFormId;
    }

    public FormType getFormType() {
        return formType;
    }

    public void setFormType(final FormType formType) {
        this.formType = formType;
    }

    public FormLockStatus getFormLockStatus() {
        return formLockStatus;
    }

    public void setFormLockStatus(final FormLockStatus formLockStatus) {
        this.formLockStatus = formLockStatus;
    }
}
