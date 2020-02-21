package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.services.common.configuration.Value;

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
}
