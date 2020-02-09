package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonpath.JsonPath.parse;
import static junit.framework.TestCase.assertNotNull;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.OffencesForDefendantChangedVerificationHelper.verifyOffence;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Before;
import org.junit.Test;

public class OffencesForDefendantChangedTransformationTest {


    private final JoltTransformer joltTransformer = new JoltTransformer();

    @Before
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformOffencesForDefendantChangedEvent() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.offences-for-defendant-changed-spec.json");
        final JsonObject inputJson = readJson("/progression.event.offences-for-defendant-changed.json");

        assertNotNull(specJson);
        assertNotNull(inputJson);

        final DocumentContext inputOffence = parse(inputJson);
        final JsonObject outputOffence = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        verifyOffence(inputOffence, outputOffence);
    }
}
