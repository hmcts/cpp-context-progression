package uk.gov.moj.cpp.progression.service;

import uk.gov.moj.cpp.progression.helper.HttpConnectionHelper;

import javax.inject.Inject;
import java.io.IOException;

public class AzureFunctionService {

    private static final String HTTPS = "https://";

    private HttpConnectionHelper httpConnectionHelper;

    @Inject
    private ApplicationParameters applicationParameters;

    public AzureFunctionService() {
        this.httpConnectionHelper = new HttpConnectionHelper();
    }

    public AzureFunctionService(final HttpConnectionHelper httpConnectionHelper, final ApplicationParameters applicationParameters) {
        this.httpConnectionHelper = httpConnectionHelper;
        this.applicationParameters = applicationParameters;
    }

    public Integer makeFunctionCall(final String payload) throws IOException {
        return httpConnectionHelper.getResponseCode(HTTPS + applicationParameters.getAzureFunctionHostName() + applicationParameters.getSetCaseEjectedFunctionPath(), payload);
    }

    public Integer relayCaseOnCPP(final String payload) throws IOException {
        return httpConnectionHelper.getResponseCode(HTTPS + applicationParameters.getAzureFunctionHostName() + applicationParameters.getRelayCaseOnCppFunctionPath(),payload);
    }
}
