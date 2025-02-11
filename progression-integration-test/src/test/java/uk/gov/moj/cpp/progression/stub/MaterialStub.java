package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class MaterialStub {

    public static final String MATERIAL_METADATA_QUERY = "/material-service/query/api/rest/material/material/.*/metadata";

    public static final String UPLOAD_MATERIAL_COMMAND = "/material-service/command/api/rest/material/material";

    public static final String STRUCTURED_FORM_QUERY = "/material-service/query/api/rest/material/structured-form/.*";

    public static void stubMaterialUploadFile() {
        stubFor(post(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody("")
                ));

        stubFor(get(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));
    }

    public static void stubMaterialMetadata() {
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
    }

    public static void stubMaterialStructuredFormQuery(final String formData) {
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
    }


    public static void verifyMaterialCreated() {
        verifyMaterialCreated("materialId");
    }

    public static void verifyMaterialCreated(String... expectedValues) {
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
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
