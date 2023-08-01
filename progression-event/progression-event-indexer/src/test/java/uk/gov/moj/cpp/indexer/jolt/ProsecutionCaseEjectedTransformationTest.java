package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonassert.JsonAssert.with;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.Before;
import org.junit.Test;

public class ProsecutionCaseEjectedTransformationTest {

    private final JoltTransformer joltTransformer = new JoltTransformer();

    @Before
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformProsecutionCaseEjectedJson() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.case-ejected-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.case-ejected.json");
        final DocumentContext inputProsecutionCase = JsonPath.parse(inputJson);

        final JsonObject outputCase = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        with(outputCase.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCaseId")).getString()))
                .assertThat("$.caseStatus", equalTo("EJECTED"))
                .assertThat("$._case_type", equalTo("PROSECUTION"));
    }
}