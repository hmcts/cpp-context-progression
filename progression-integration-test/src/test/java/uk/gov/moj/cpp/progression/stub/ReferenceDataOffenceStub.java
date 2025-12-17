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

import javax.json.Json;
import javax.json.JsonObject;

public class ReferenceDataOffenceStub {

    public static void stubReferenceDataOffencesGetOffenceById(final String resourceName) {
        final JsonObject offenceResponsePayLoad = Json.createReader(ReferenceDataOffenceStub.class
                .getResourceAsStream(resourceName)).readObject();

        final String urlPath = "/referencedataoffences-service/query/api/rest/referencedataoffences/offences/.*";

        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                                       .withHeader("CPPID", randomUUID().toString())
                                       .withHeader("Content-Type", APPLICATION_JSON)
                                       .withBody(offenceResponsePayLoad.toString())));
    }

    public static void stubReferenceDataOffencesGetOffenceByOffenceCode(final String resourceName) {
        final JsonObject offenceResponsePayLoad = Json.createReader(ReferenceDataOffenceStub.class
                .getResourceAsStream(resourceName)).readObject();

        final String urlPath = "/referencedataoffences-service/query/api/rest/referencedataoffences/offences";

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("cjsoffencecode", matching(".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(offenceResponsePayLoad.toString())));
    }
}

