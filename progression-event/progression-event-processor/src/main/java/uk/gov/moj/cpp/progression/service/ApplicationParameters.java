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
}
