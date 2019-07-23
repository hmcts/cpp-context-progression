package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;

public class ApplicationParameters {

    @Inject
    @Value(key = "applicationTemplateId")
    private String applicationTemplateId;

    public String getApplicationTemplateId() {
        return applicationTemplateId;
    }
}
