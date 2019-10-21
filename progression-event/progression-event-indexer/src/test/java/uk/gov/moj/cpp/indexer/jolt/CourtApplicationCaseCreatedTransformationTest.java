package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonpath.JsonPath.parse;
import static junit.framework.TestCase.assertNotNull;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.CourtApplicationVerificationHelper.verifyAddApplication;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.CourtApplicationVerificationHelper.verifyAddApplicationWhenNoOrganisationApplicant;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.CourtApplicationVerificationHelper.verifyEmbeddedApplication;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.CourtApplicationVerificationHelper.verifyStandaloneApplication;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Before;
import org.junit.Test;

public class CourtApplicationCaseCreatedTransformationTest {

    private final JoltTransformer joltTransformer = new JoltTransformer();

    @Before
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformStandaloneCourtCreatedApplication() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.court-application-created-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.court-application-created.json");
        final DocumentContext inputCourtApplication = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        verifyStandaloneApplication(inputCourtApplication, transformedJson);
        verifyAddApplication(inputCourtApplication, transformedJson);
    }

    @Test
    public void shouldTransformEmbeddedCourtCreatedApplication() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.court-application-created-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.court-application-created-embedded.json");
        final DocumentContext inputCourtApplication = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        verifyEmbeddedApplication(inputCourtApplication, transformedJson);
        verifyAddApplication(inputCourtApplication, transformedJson);
    }

    @Test
    public void shouldTransformStandaloneCourtCreatedApplicationWithNoApplicantOrganisation() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.court-application-created-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.court-application-created-with-no-applicant-org.json");
        final DocumentContext inputCourtApplication = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        verifyStandaloneApplication(inputCourtApplication, transformedJson);
        verifyAddApplicationWhenNoOrganisationApplicant(inputCourtApplication, transformedJson);
    }
}
