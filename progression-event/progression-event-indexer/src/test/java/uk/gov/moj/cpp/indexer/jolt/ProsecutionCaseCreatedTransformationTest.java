package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonassert.JsonAssert.with;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.Before;
import org.junit.Test;

public class ProsecutionCaseCreatedTransformationTest {

    private final JoltTransformer joltTransformer = new JoltTransformer();

    @Before
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformProsecutionCaseCreatedJson() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.prosecution-case-created-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.prosecution-case-created.json");
        final DocumentContext inputProsecutionCase = JsonPath.parse(inputJson);

        final JsonObject outputCase = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        verifyCaseCreated(inputProsecutionCase, outputCase, 1, true);
    }

    @Test
    public void shouldTransformProsecutionCaseCreatedJsonWithProsecutor() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.prosecution-case-created-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.prosecution-case-created-with-prosecutor.json");
        final DocumentContext inputProsecutionCase = JsonPath.parse(inputJson);

        final JsonObject outputCase = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        with(outputCase.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.id")).getString()))
                .assertThat("$.caseReference", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityReference")).getString()))
                .assertThat("$.prosecutingAuthority", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.prosecutor.prosecutorCode")).getString()))
                .assertThat("$.caseStatus", equalTo("ACTIVE"))
                .assertThat("$._case_type", equalTo("PROSECUTION"))
                .assertThat("$._is_crown", equalTo(false));
    }


    @Test
    public void shouldTransformProsecutionCaseCreatedWithMultipleDefendantsJson() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.prosecution-case-created-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.prosecution-case-created-with-multi-defendants.json");
        final DocumentContext inputProsecutionCase = JsonPath.parse(inputJson);

        final JsonObject outputCase = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        verifyCaseCreated(inputProsecutionCase, outputCase, 3, false);
    }
}