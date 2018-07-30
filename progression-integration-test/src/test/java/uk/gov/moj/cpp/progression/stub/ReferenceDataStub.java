package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import javax.json.Json;
import javax.json.JsonObject;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

public class ReferenceDataStub {

    public static void stubQueryOffences(String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject offences = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/offences";
        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("cjsoffencecode", matching(".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(offences.toString())));

        waitForStubToBeReady(urlPath + "?cjsoffencecode", "application/vnd.referencedata.query.offences+json");
    }

    public static void stubQueryJudge(String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject judge = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/court/judges/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                .withHeader("CPPID", randomUUID().toString())
                .withHeader("Content-Type", APPLICATION_JSON)
                .withBody(judge.toString())));

        waitForStubToBeReady(urlPath  , "application/vnd.referencedata.get.judge+json");
    }

    public static void stubQueryCourtCentre(String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject courtCentre = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/court/centres/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                .withHeader("CPPID", randomUUID().toString())
                .withHeader("Content-Type", APPLICATION_JSON)
                .withBody(courtCentre.toString())));

        waitForStubToBeReady(urlPath  , "application/vnd.referencedata.get.court-centre+json");
    }

}
