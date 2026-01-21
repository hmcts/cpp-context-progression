package uk.gov.moj.cpp.progression.service;

import lombok.Getter;
import uk.gov.justice.services.common.configuration.Value;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

@Getter
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
    @Value(key = "notifyHearingTemplateId", defaultValue = "e4648583-eb0f-438e-aab5-5eff29f3f7b4")
    private String notifyHearingTemplateId;


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

    @Inject
    @Value(key = "onlineGuiltyPleaCourtHearingEnglishTemplateId", defaultValue = "9a9d3078-9b52-4081-a183-22b3540e0edb")
    private String onlineGuiltyPleaCourtHearingEnglishTemplateId;

    @Inject
    @Value(key = "onlineGuiltyPleaCourtHearingWelshTemplateId", defaultValue = "dd91efcc-620f-453b-afc5-e8d97eebc4a3")
    private String onlineGuiltyPleaCourtHearingWelshTemplateId;

    @Inject
    @Value(key = "onlineGuiltyPleaNoCourtHearingEnglishTemplateId", defaultValue = "c6a5fe5e-fcf9-4a69-8305-58476dd38a89")
    private String onlineGuiltyPleaNoCourtHearingEnglishTemplateId;

    @Inject
    @Value(key = "onlineGuiltyPleaNoCourtHearingWelshTemplateId", defaultValue = "2d350aae-f3d7-40d9-a35b-102a246a4e4e")
    private String onlineGuiltyPleaNoCourtHearingWelshTemplateId;

    @Inject
    @Value(key = "onlineNotGuiltyPleaEnglishTemplateId", defaultValue = "40cc2acc-66a5-4779-ad6a-b529f8f8a76d")
    private String onlineNotGuiltyPleaEnglishTemplateId;

    @Inject
    @Value(key = "onlineNotGuiltyPleaWelshTemplateId", defaultValue = "cee11f72-fb64-46a2-9d7a-b9cc9a549993")
    private String onlineNotGuiltyPleaWelshTemplateId;

    @Inject
    @Value(key = "onlinePleaProsecutorTemplateId", defaultValue = "fae48a1e-612a-4e98-9a95-e650754e9f6c")
    public String onlinePleaProsecutorTemplateId;

    @Inject
    @Value(key = "laa.azure.apim.invocation.retryTimes", defaultValue = "3")
    private String retryTimes;

    @Inject
    @Value(key = "laa.azure.apim.invocation.retryInterval", defaultValue = "1000")
    public String retryInterval;

    /**
     * URL for Crime Hearing Case Event service PCR notification endpoint.
     * Local Development: Default value: http://localhost:8080/AMP/notifications/pcr
     * For Higher environments
     *  Kubernetes Deployment Options: Services are in different namespaces and in the same network
     *  Ingress URL or Kubernetes Discovery URL
     */
    @Inject
    @Value(key = "crimeHearingCaseEvent.pcrNotification.url", defaultValue ="http://localhost:8080/AMP/notifications/pcr")
    private String crimeHearingCaseEventPcrNotificationUrl;

    @Inject
    @Value(key = "crimeHearingCaseEvent.pcrNotification.retryTimes", defaultValue = "3")
    private String crimeHearingCaseEventRetryTimes;

    @Inject
    @Value(key = "crimeHearingCaseEvent.pcrNotification.retryInterval", defaultValue = "1000")
    public String crimeHearingCaseEventRetryInterval;

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
