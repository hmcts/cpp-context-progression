package uk.gov.moj.cpp.indexer.jolt;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.CourtApplicationVerificationHelper.verifyCourtApplication;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationReferredToBoxworkTransformerTest {
    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformApplicationReferredToBoxworkJson() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.application-referred-to-boxwork-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.application-referred-to-boxwork.json");
        final DocumentContext inputDocumentContext = JsonPath.parse(inputJson);

        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        verifyCourtApplication(inputDocumentContext, transformedJson);
    }
}
