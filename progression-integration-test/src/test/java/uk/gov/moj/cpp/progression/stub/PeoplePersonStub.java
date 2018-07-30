package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import javax.json.Json;
import javax.json.JsonObject;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

public class PeoplePersonStub {

    public static void stubQueryPersons(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("people-service");
        final JsonObject persons = Json.createReader(
                        PeoplePersonStub.class.getResourceAsStream(resourceName))
                        .readObject();

        final String urlPath =
                        "/people-service/query/api/rest/people/people/people";
        stubFor(get(urlPathEqualTo(urlPath)).withQueryParam("personIds", matching(".*"))
                        .willReturn(aResponse().withStatus(SC_OK)
                                        .withHeader("CPPID", randomUUID().toString())
                                        .withHeader("Content-Type", APPLICATION_JSON)
                                        .withBody(persons.toString())));

        waitForStubToBeReady(urlPath + "?personIds", "application/vnd.people.query.persons+json");
    }

}
