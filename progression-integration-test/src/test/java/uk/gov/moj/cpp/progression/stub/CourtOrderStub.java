package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;


public class CourtOrderStub {

    public static void setupCourtOrdersStub() {

        InternalEndpointMockUtils.stubPingFor("applicationscourtorders-service");

        final String body = getPayload("stub-data/applicationscourtorders.get-court-order-by-defendant-id.json");

        final String urlPath = "/applicationscourtorders-service/query/api/rest/courtorders/court-order/defendant/";

                stubFor(get(urlPathMatching(urlPath+".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(body)));

        waitForStubToBeReady(urlPath, "application/vnd.courtorders.query.court-order-by-defendant-id+json");
    }
}
