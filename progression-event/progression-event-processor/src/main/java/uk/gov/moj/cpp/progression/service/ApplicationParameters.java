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

    public String getCpsCourtDocumentTemplateId() {
        return cpsCourtDocumentTemplateId;
    }

    public String getCpsDefendantCourtDocumentTemplateId() {
        return cpsDefendantCourtDocumentTemplateId;
    }

    public String getNotifyDefenceOfNewMaterialTemplateId() {
        return notifyDefenceOfNewMaterialTemplateId;
    }

    public String getEndClientHost() {
        return endClientHost;
    }

    public String getEmailTemplateId(final String templateName) {
        final Map<String, String> emailTemplatesMap = new HashMap<>();
        emailTemplatesMap.put("pcr_standard", this.prisonCourtRegisterEmailTemplateId);
        emailTemplatesMap.put("ir_standard", this.informantRegisterEmailTemplateId);
        emailTemplatesMap.put("cr_standard", this.courtRegisterEmailTemplateId);
        emailTemplatesMap.put("now_standard_template", this.nowEmailTemplateId);
        emailTemplatesMap.put("now_sla_template", this.nowSlaEmailTemplateId);

        return emailTemplatesMap.getOrDefault(templateName, "");
    }
}
