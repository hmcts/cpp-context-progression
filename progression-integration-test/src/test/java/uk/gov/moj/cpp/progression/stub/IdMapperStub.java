package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;

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
}
