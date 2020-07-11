package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;

public class MaterialStub {

    public static final String UPLOAD_MATERIAL_COMMAND = "/material-service/command/api/rest/material/material";
    public static final String MATERIAL_UPLOAD_COMMAND_TYPE = "material.command.upload-file";

    public static void stubMaterialUploadFile() {
        InternalEndpointMockUtils.stubPingFor("material-service");

        stubFor(post(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody("")
                ));

        stubFor(get(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(UPLOAD_MATERIAL_COMMAND, MATERIAL_UPLOAD_COMMAND_TYPE);
    }

    public static void verifyMaterialCreated() {
        await().atMost(30, SECONDS).pollInterval(5, SECONDS).until(() -> {
            RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlPathMatching(UPLOAD_MATERIAL_COMMAND));
            verify(requestPatternBuilder);
        });
    }
}
