package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import javax.json.Json;
import javax.json.JsonObject;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

public class StructureCaseDetailsStub {

    public static void stubQueryCaseDetails(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("structure-service");
        final JsonObject caseDetails = Json.createReader(
                        StructureCaseDetailsStub.class.getResourceAsStream(resourceName))
                        .readObject();

        final String urlPath =
                        "/structure-service/query/api/rest/structure/cases/.*";
        stubFor(get(urlMatching(urlPath)).willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON).withBody(caseDetails.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.structure.query.case+json");
    }
}
