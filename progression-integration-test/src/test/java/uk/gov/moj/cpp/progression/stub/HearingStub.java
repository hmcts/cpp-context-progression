package uk.gov.moj.cpp.progression.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.helper.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

public class HearingStub {

    public static final String HEARING_COMMAND = "/hearing-service/command/api/rest/hearing/hearings";
    public static final String HEARING_COMMAND_TYPE = "hearing.initiate";

    public static void stubInitiateHearing() {
        InternalEndpointMockUtils.stubPingFor("hearing-service");

        stubFor(post(urlPathEqualTo(HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)));

        stubFor(get(urlPathEqualTo(HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(HEARING_COMMAND, HEARING_COMMAND_TYPE);
    }
}
