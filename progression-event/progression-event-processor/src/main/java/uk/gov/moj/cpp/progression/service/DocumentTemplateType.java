package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.core.courts.FormType;

import java.util.Arrays;

public enum DocumentTemplateType {

    BETTER_CASE_MANAGEMENT(FormType.BCM, false, "BetterCaseManagement"),
    BETTER_CASE_MANAGEMENT_WELSH(FormType.BCM, true, "BetterCaseManagementWelsh"),
    PLEA_AND_TRIAL_PREPARATION_HEARING(FormType.PTPH, false, "PleaAndTrialPreparationHearing"),
    PLEA_AND_TRIAL_PREPARATION_HEARING_WELSH(FormType.PTPH, true, "PleaAndTrialPreparationHearingWelsh"),
    PREPARATION_FOR_EFFECTIVE_TRIAL(FormType.PET, false, "PreparationForEffectiveTrial"),
    PREPARATION_FOR_EFFECTIVE_TRIAL_WELSH(FormType.PET, true, "PreparationForEffectiveTrialWelsh");

    private FormType formType;
    private boolean isWelsh;
    private String templateName;

    DocumentTemplateType(final FormType formType, final boolean isWelsh, final String templateName) {
        this.formType = formType;
        this.isWelsh = isWelsh;
        this.templateName = templateName;
    }


    public static String getDocumentTemplateNameByType(final FormType formType, final boolean isWelsh) {
        return Arrays.stream(DocumentTemplateType.values())
                .filter(dc -> dc.formType.equals(formType) && dc.isWelsh == isWelsh)
                .map(DocumentTemplateType::getTemplateName)
                .findFirst()
                .orElse(null);
    }

    public FormType getCode() {
        return formType;
    }

    public boolean getWelsh() {
        return isWelsh;
    }

    public String getTemplateName() { return templateName; }
}
