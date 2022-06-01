package uk.gov.moj.cpp.progression.domain.aggregate.utils;

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
    private List<PetDefendants> formDefendants;

    public Form() {
    }

    public Form(final List<PetDefendants> formDefendants, final UUID courtFormId, final FormType formType, final UUID submissionId) {
        this.courtFormId = courtFormId;
        this.formType = formType;
        this.submissionId = submissionId;
        this.formDefendants = Collections.unmodifiableList(formDefendants);
    }

    public List<PetDefendants> getFormDefendants() {
        return new ArrayList<>(formDefendants);
    }

    public void setFormDefendants(final List<PetDefendants> formDefendants) {
        this.formDefendants = Collections.unmodifiableList(formDefendants);
    }

    public UUID getCourtFormId() {
        return courtFormId;
    }

    public void setCourtFormId(final UUID petId) {
        this.courtFormId = petId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(final UUID submissionId) {
        this.submissionId = submissionId;
    }

    public FormType getFormType() {
        return formType;
    }

    public void setFormType(final FormType formType) {
        this.formType = formType;
    }
}
