package uk.gov.moj.cpp.progression.service;

import uk.gov.moj.cpp.platform.data.utils.rest.service.RestClientService;
import uk.gov.moj.cpp.progression.helper.HttpConnectionHelper;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureFunctionService {

    private static final String HTTPS = "https://";
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureFunctionService.class.getName());

    private HttpConnectionHelper httpConnectionHelper;

    @Inject
    private RestClientService restClientService;

    @Inject
    private ApplicationParameters applicationParameters;

    public AzureFunctionService() {
        this.httpConnectionHelper = new HttpConnectionHelper();
    }

    public AzureFunctionService(final HttpConnectionHelper httpConnectionHelper, final ApplicationParameters applicationParameters, final RestClientService restClientService) {
        this.httpConnectionHelper = httpConnectionHelper;
        this.applicationParameters = applicationParameters;
        this.restClientService = restClientService;
    }

    public Integer makeFunctionCall(final String payload) throws IOException {
        return httpConnectionHelper.getResponseCode(HTTPS + applicationParameters.getAzureFunctionHostName() + applicationParameters.getSetCaseEjectedFunctionPath(), payload);
    }

    public Integer relayCaseOnCPP(final String payload) throws IOException {
        return httpConnectionHelper.getResponseCode(HTTPS + applicationParameters.getAzureFunctionHostName() + applicationParameters.getRelayCaseOnCppFunctionPath(), payload);
    }

    public Integer concludeDefendantProceeding(final String payload) {
        final Response response = restClientService.post(applicationParameters.getDefendantProceedingsConcludedApimUrl(), getHeaders(applicationParameters.getSubscriptionKey()), payload);
        LOGGER.info("Azure function response  status{}", response.getStatusInfo().getReasonPhrase());
        return response.getStatus();
    }

    private Map<String, String> getHeaders(final String subscriptionKey) {
        return ImmutableMap.of(
                HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON,
                "Ocp-Apim-Subscription-Key", subscriptionKey,
                "Ocp-Apim-Trace", "true");
    }
}
