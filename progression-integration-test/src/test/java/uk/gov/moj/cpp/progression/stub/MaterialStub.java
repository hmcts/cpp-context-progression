package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class MaterialStub {

    public static final String MATERIAL_METADATA_QUERY = "/material-service/query/api/rest/material/material/.*/metadata";
    public static final String MATERIAL_METADATA_QUERY_TYPE = "material.query.material-metadata";

    public static final String UPLOAD_MATERIAL_COMMAND = "/material-service/command/api/rest/material/material";
    public static final String MATERIAL_UPLOAD_COMMAND_TYPE = "material.command.upload-file";

    public static final String STRUCTURED_FORM_QUERY = "/material-service/query/api/rest/material/structured-form/.*";
    public static final String STRUCTURED_FORM_QUERY_TYPE = "material.query.structured-form";

    public static void stubMaterialUploadFile() {
        InternalEndpointMockUtils.stubPingFor("material-service");

        stubFor(post(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody("")
                ));

        stubFor(get(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(UPLOAD_MATERIAL_COMMAND, MATERIAL_UPLOAD_COMMAND_TYPE);
    }

    public static void stubMaterialMetadata() {
        InternalEndpointMockUtils.stubPingFor("material-service");

        stubFor(get(urlPathMatching(MATERIAL_METADATA_QUERY))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(createObjectBuilder()
                                .add("materialAddedDate", ZonedDateTime.now().toString())
                                .add("mimeType", "pdf")
                                .add("fileName", "filename.pdf")
                                .build().toString())
                ));

        waitForStubToBeReady(format(MATERIAL_METADATA_QUERY, UUID.randomUUID()), MATERIAL_METADATA_QUERY_TYPE);
    }

    public static void stubMaterialStructuredFormQuery(final String formData) {
        InternalEndpointMockUtils.stubPingFor("material-service");

        stubFor(get(urlPathMatching(STRUCTURED_FORM_QUERY))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(createObjectBuilder()
                                .add("courtFormId", randomUUID().toString())
                                .add("data", formData)
                                .add("lastUpdated", ZonedDateTime.now().toString())
                                .build().toString())
                ));

        waitForStubToBeReady(format(STRUCTURED_FORM_QUERY, randomUUID()), STRUCTURED_FORM_QUERY_TYPE);
    }


    public static void verifyMaterialCreated() {
        verifyMaterialCreated("materialId");
    }

    public static void verifyMaterialCreated(String... expectedValues) {
        await().atMost(30, SECONDS).pollInterval(1, SECONDS).until(() -> {
            RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(UPLOAD_MATERIAL_COMMAND));
            Arrays.stream(expectedValues).forEach(
                    expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
            );
            try {
                verify(requestPatternBuilder);
            } catch (VerificationException e) {
                return false;
            }
            return true;
        });
    }
}
