package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseDefendantUpdated;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProsecutionCaseDefendantUpdatedTransformationTest {


    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformCaseDefendantUpdated() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.prosecution-case-defendant-updated-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.prosecution-case-defendant-updated.json");
        final JsonObject outputDefendantJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        final DocumentContext prosecutionCase = JsonPath.parse(inputJson);
        verifyCaseDefendantUpdated(prosecutionCase, outputDefendantJson);
        with(outputDefendantJson.toString()).assertNotDefined("$.caseStatus");
    }

}
