package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonpath.JsonPath.parse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.OffencesForDefendantChangedVerificationHelper.verifyAddedOffences;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.OffencesForDefendantChangedVerificationHelper.verifyDeletedOffences;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.OffencesForDefendantChangedVerificationHelper.verifyUpdatedOffences;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OffencesForDefendantChangedTransformationTest {


    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformOffencesForDefendantChangedEventAddedOffences() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.offences-for-defendant-changed-spec.json");
        final JsonObject inputJson = readJson("/progression.event.offences-for-defendant-changed-added.json");

        assertNotNull(specJson);
        assertNotNull(inputJson);

        final DocumentContext inputOffence = parse(inputJson);
        final JsonObject outputOffence = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        verifyAddedOffences(inputOffence, outputOffence);
    }

    @Test
    public void shouldTransformOffencesForDefendantChangedEventDeletedOffences() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.offences-for-defendant-changed-spec.json");
        final JsonObject inputJson = readJson("/progression.event.offences-for-defendant-deleted.json");

        assertNotNull(specJson);
        assertNotNull(inputJson);

        final DocumentContext inputOffence = parse(inputJson);
        final JsonObject outputOffence = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        verifyDeletedOffences(inputOffence, outputOffence);
    }

    @Test
    public void shouldTransformOffencesForDefendantChangedEventUpdatedOffences() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.offences-for-defendant-changed-spec.json");
        final JsonObject inputJson = readJson("/progression.event.offences-for-defendant-updated.json");

        assertNotNull(specJson);
        assertNotNull(inputJson);

        final DocumentContext inputOffence = parse(inputJson);
        final JsonObject outputOffence = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        verifyUpdatedOffences(inputOffence, outputOffence);
    }
}
