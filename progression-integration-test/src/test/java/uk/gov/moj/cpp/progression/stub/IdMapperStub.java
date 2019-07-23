package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static com.jayway.awaitility.Duration.TEN_SECONDS;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.moj.cpp.progression.WireMockStubUtils.BASE_URI;

import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.util.UUID;

import javax.json.Json;
import javax.ws.rs.core.Response;

import org.apache.http.HttpHeaders;

/**
 * Created by satishkumar on 10/12/2018.
 */
public class IdMapperStub {
    private static final String SYSTEM_ID_MAPPER_ENDPOINT = "/system-id-mapper-api/rest/systemid/mappings/*";

    public static void setUp() {
        stubPingFor("system-id-mapper-api");

        stubFor(get(urlPathMatching(SYSTEM_ID_MAPPER_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(404)));

        stubFor(post(urlPathMatching(SYSTEM_ID_MAPPER_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\n" +
                                "\t\"_metadata\": {\n" +
                                "\t\t\"id\": \"f2426280-f4d7-45cf-9f94-c618a210f7c2\",\n" +
                                "\t\t\"name\": \"systemid.map\"\n" +
                                "\t},\n" +
                                "\t\"id\": \"" + randomUUID() + "\"\n" +
                                "}")));
    }

    public static void stubForIdMapperSuccess(final Response.Status status, final UUID id) {
        final String path = "/system-id-mapper-api/rest/systemid/";
        final String mime = "application/vnd.systemid.map+json";

        stubFor(post(urlPathMatching(path))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(mime))
                .willReturn(aResponse()
                        .withStatus(status.getStatusCode())
                        .withBody(Json.createObjectBuilder().add("id", id.toString()).build().toString())
                )
        );

        waitForPostStubToBeReady(path, mime, status);
    }

    public static void stubForIdMapperSuccess(final Response.Status status) {
        stubForIdMapperSuccess(status, UUID.randomUUID());
    }

    private static void waitForPostStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RestClient restClient = new RestClient();
        waitAtMost(TEN_SECONDS).until(() -> restClient.postCommand(BASE_URI + resource, mediaType, "").getStatus() == expectedStatus.getStatusCode());
    }
}
