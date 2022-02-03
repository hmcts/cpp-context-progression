package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.services.common.configuration.Value;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class ApplicationParameters {

    @Inject
    @Value(key = "applicationTemplateId")
    private String applicationTemplateId;

    @Inject
    @Value(key = "defenceInstructionTemplateId")
    private String defenceInstructionTemplateId;

    @Inject
    @Value(key = "defenceDisassociationTemplateId")
    private String defenceDisassociationTemplateId;

    @Inject
    @Value(key = "defenceAssociationTemplateId")
    private String defenceAssociationTemplateId;

    @Inject
    @Value(key = "ncesEmailTemplateId")
    private String ncesEmailTemplateId;

    @Inject
    @Value(key = "cpsCourtDocumentTemplateId")
    private String cpsCourtDocumentTemplateId;


    @Inject
    @Value(key = "cpsDefendantCourtDocumentTemplateId")
    private String cpsDefendantCourtDocumentTemplateId;

    @Inject
    @Value(key = "AZURE_FUNCTION_HOST_NAME")
    private String azureFunctionHostName;

    @Inject
    @Value(key = "azureScanManagerContainerName")
    private String azureScanManagerContainerName;

    @Inject
    @Value(key = "SET_CASE_EJECTED_FUNCTION_PATH")
    private String setCaseEjectedFunctionPath;

    @Inject
    @Value(key = "RELAY_CASE_ON_CPP_FUNCTION_PATH")
    private String relayCaseOnCppFunctionPath;

    @Inject
    @Value(key = "laa.defendantProceedingsConcluded.apim.url", defaultValue ="http://localhost:8080/LAA/v1/caseOutcome/conclude")
    private String defendantProceedingsConcludedApimUrl;

    @Inject
    @Value(key = "laa.defendantProceedingsConcluded.apim.subscription.key", defaultValue ="3674a16507104b749a76b29b6c837352")
    private String subscriptionKey;

    @Inject
    @Value(key = "notifyDefenceOfNewMaterialTemplateId", defaultValue = "f5d3b9e6-0583-4062-b00d-38d02b548409")
    private String notifyDefenceOfNewMaterialTemplateId;

    @Inject
    @Value(key = "endClientHost", defaultValue = "")
    private String endClientHost;

    @Inject
    @Value(key = "pcr_email_template_id")
    private String prisonCourtRegisterEmailTemplateId;

    @Inject
    @Value(key = "cr_email_template_id")
    private String courtRegisterEmailTemplateId;

    @Inject
    @Value(key = "ir_email_template_id")
    private String informantRegisterEmailTemplateId;

    @Inject
    @Value(key = "now_email_template_id")
    private String nowEmailTemplateId;

    @Inject
    @Value(key = "now_sla_email_template_id")
    private String nowSlaEmailTemplateId;

    @Inject
    @Value(key = "now_extradition_email_template_id")
    private String nowExtraditionEmailTemplateId;

    @Inject
    @Value(key = "now_extradition_sla_email_template_id")
    private String nowExtraditionSlaEmailTemplateId;

    @Inject
    @Value(key = "unscheduled_hearing_allocation_email_templateId", defaultValue = "326a74e0-5e1c-49fe-880e-834a7b58a864")
    private String unscheduledHearingAllocationEmailTemplateId;

    @Inject
    @Value(key = "caseAtaGlanceURI", defaultValue = "prosecution-casefile/case-at-a-glance/")
    private String caseAtaGlanceURI;

    @Inject
    @Value(key = "summons_approved_and_suppressed_template_id")
    private String summonsApprovedAndSuppressedTemplateId;

    @Inject
    @Value(key = "summons_approved_and_not_suppressed_template_id")
    private String summonsApprovedAndNotSuppressedTemplateId;

    @Inject
    @Value(key = "summons_rejected_template_id")
    private String summonsRejectedTemplateId;

    @Inject
    @Value(key = "statdec_send_appointment_letter_template_id")
    private String statDecSendAppointmentLetterTemplateId;

    public String getDefenceAssociationTemplateId() {
        return defenceAssociationTemplateId;
    }

    public String getNcesEmailTemplateId() {

        return ncesEmailTemplateId;
    }

    public String getApplicationTemplateId() {

        return applicationTemplateId;
    }

    public String getDefenceInstructionTemplateId() {

        return defenceInstructionTemplateId;
    }

    public String getDefenceDisassociationTemplateId() {

        return defenceDisassociationTemplateId;
    }

    public String getAzureFunctionHostName() {
        return azureFunctionHostName;
    }

    public String getAzureScanManagerContainerName() {
        return azureScanManagerContainerName;
    }

    public String getSetCaseEjectedFunctionPath() {
        return setCaseEjectedFunctionPath;
    }

    public String getRelayCaseOnCppFunctionPath() {
        return relayCaseOnCppFunctionPath;
    }

    public String getDefendantProceedingsConcludedApimUrl() {
        return defendantProceedingsConcludedApimUrl;
    }

    public String getSubscriptionKey() {
        return subscriptionKey;
    }

    public String getCpsCourtDocumentTemplateId() {
        return cpsCourtDocumentTemplateId;
    }

    public String getCpsDefendantCourtDocumentTemplateId() {
        return cpsDefendantCourtDocumentTemplateId;
    }

    public String getNotifyDefenceOfNewMaterialTemplateId() {
        return notifyDefenceOfNewMaterialTemplateId;
    }

    public String getSummonsApprovedAndSuppressedTemplateId() {
        return summonsApprovedAndSuppressedTemplateId;
    }

    public String getSummonsApprovedAndNotSuppressedTemplateId() {
        return summonsApprovedAndNotSuppressedTemplateId;
    }

    public String getSummonsRejectedTemplateId() {
        return summonsRejectedTemplateId;
    }

    public String getEndClientHost() {
        return endClientHost;
    }

    public String getUnscheduledHearingAllocationEmailTemplateId() {
        return unscheduledHearingAllocationEmailTemplateId;
    }

    public String getCaseAtaGlanceURI() {
        return caseAtaGlanceURI;
    }

    public String getStatDecSendAppointmentLetterTemplateId() {
        return statDecSendAppointmentLetterTemplateId;
    }

    public String getEmailTemplateId(final String templateName) {
        final Map<String, String> emailTemplatesMap = new HashMap<>();
        emailTemplatesMap.put("pcr_standard", this.prisonCourtRegisterEmailTemplateId);
        emailTemplatesMap.put("ir_standard", this.informantRegisterEmailTemplateId);
        emailTemplatesMap.put("cr_standard", this.courtRegisterEmailTemplateId);
        emailTemplatesMap.put("now_standard_template", this.nowEmailTemplateId);
        emailTemplatesMap.put("now_sla_template", this.nowSlaEmailTemplateId);
        emailTemplatesMap.put("now_extradition_standard_template", this.nowExtraditionEmailTemplateId);
        emailTemplatesMap.put("now_extradition_sla_template", this.nowExtraditionSlaEmailTemplateId);

        return emailTemplatesMap.getOrDefault(templateName, "");
    }
}
