package uk.gov.moj.cpp.progression.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.helper.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

import javax.json.Json;

public class AuthorisationServiceStub extends StubUtil {

    private static final String CAPABILITY_ENABLEMENT_QUERY_URL = "/authorisation-service-server/rest/capabilities/%s";
    private static final String CAPABILITY_ENABLEMENT_QUERY_MEDIA_TYPE = "application/vnd.authorisation.capability+json";
    private static final String AUTHORISATION_SERVICE_SERVER = "authorisation-service-server";

    public static void stubSetStatusForCapability(String capabilityName, boolean statusToReturn) {
        String url = format(CAPABILITY_ENABLEMENT_QUERY_URL, capabilityName);
        stubEnableCapabilities(url, statusToReturn);
    }

    public static void stubEnableAllCapabilities() {
        String url = format(CAPABILITY_ENABLEMENT_QUERY_URL, ".*");
        stubEnableCapabilities(url, true);
    }

    private static void stubEnableCapabilities(String stubUrl, boolean statusToReturn) {
        String responsePayload = Json.createObjectBuilder().add("enabled", statusToReturn).build().toString();
        InternalEndpointMockUtils.stubPingFor(AUTHORISATION_SERVICE_SERVER);

        stubFor(get(urlMatching(stubUrl))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", DEFAULT_JSON_CONTENT_TYPE)
                        .withBody(responsePayload)));

        waitForStubToBeReady(stubUrl, CAPABILITY_ENABLEMENT_QUERY_MEDIA_TYPE);
    }
}
