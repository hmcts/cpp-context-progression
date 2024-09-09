package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

public class AzureSteCaseFilterServiceStub {

    private static final String STE_CASE_FILTER_ENDPOINT_URL = "/fa-ste-casefilter";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String SET_CASE_EJECTED = "/api/setCaseEjected?code=6o94LtYzbEBjHHWJWAcrHjFnUfG5ttkUOHqaJQUAfIiCx27D6G8AZQ==";

    private AzureSteCaseFilterServiceStub() {}

    static {
        configureFor(HOST, 8080);
    }

    public static void stubPostSetCaseEjected() {
        stubFor(post(urlPathMatching(format("%s", STE_CASE_FILTER_ENDPOINT_URL + SET_CASE_EJECTED)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

}
