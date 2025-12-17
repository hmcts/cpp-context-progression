package uk.gov.moj.cpp.progression.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.FormType.BCM;
import static uk.gov.justice.core.courts.FormType.PET;
import static uk.gov.justice.core.courts.FormType.PTPH;
import static uk.gov.moj.cpp.progression.service.DocumentTemplateType.getDocumentTemplateNameByType;

import org.junit.jupiter.api.Test;

public class DocumentTemplateTypeTest {

    @Test
    public void shouldReturnTemplateNameByFormTypeAndIsWelshFlag() {
        assertThat(getDocumentTemplateNameByType(BCM, false), is("BetterCaseManagement"));
        assertThat(getDocumentTemplateNameByType(BCM, true), is("BetterCaseManagementWelsh"));
        assertThat(getDocumentTemplateNameByType(PTPH, false), is("PleaAndTrialPreparationHearing"));
        assertThat(getDocumentTemplateNameByType(PTPH, true), is("PleaAndTrialPreparationHearingWelsh"));
        assertThat(getDocumentTemplateNameByType(PET, false), is("PreparationForEffectiveTrial"));
        assertThat(getDocumentTemplateNameByType(PET, true), is("PreparationForEffectiveTrialWelsh"));
    }
}
