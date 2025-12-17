package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.CourtApplicationVerificationHelper.verifyAddApplicationWhenNoOrganisationApplicant;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.CourtApplicationVerificationHelper.verifyEmbeddedApplication;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.CourtApplicationVerificationHelper.verifyStandaloneApplication;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.CourtApplicationVerificationHelper.verifyUpdateApplication;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CourtApplicationProceedingsEditTransformationTest {

    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformStandaloneCourtUpdatedApplication() throws IOException {

        final JsonObject inputJson = readJson("/progression.event.court-application-updated-with-no-applicant-org.json");
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.court-application-proceedings-edited-spec.json");
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        final Configuration suppressPathNotFoundExceptionConfiguration = defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS);
        final DocumentContext inputCourtApplication = parse(inputJson, suppressPathNotFoundExceptionConfiguration);

        verifyStandaloneApplication(inputCourtApplication, transformedJson);
        verifyAddApplicationWhenNoOrganisationApplicant(inputCourtApplication, transformedJson, 2, 0);
    }


    @Test
    public void shouldTransformEmbeddedCourtUpdatedApplication() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.court-application-proceedings-edited-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.court-application-updated-embedded.json");
        final DocumentContext inputCourtApplication = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        verifyEmbeddedApplication(inputCourtApplication, transformedJson);
        verifyUpdateApplication(inputCourtApplication, transformedJson, 2, 0);
    }

    @Test
    public void shouldTransformStandaloneCourtUpdatedApplicationWithOrganisation() throws IOException {

        final JsonObject inputJson = readJson("/progression.event.court-application-updated.json");
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.court-application-proceedings-edited-spec.json");
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        final DocumentContext inputCourtApplication = parse(inputJson);
        verifyStandaloneApplication(inputCourtApplication, transformedJson);
        verifyUpdateApplication(inputCourtApplication, transformedJson, 2, 0);
    }

    @Test
    public void shouldTransformStandaloneCourtUpdatedApplicationWithoutRespondents() throws IOException {

        final JsonObject inputJson = readJson("/progression.event.court-application-updated-without-respondent.json");
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.court-application-proceedings-edited-spec.json");
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        final DocumentContext inputCourtApplication = parse(inputJson);
        verifyStandaloneApplication(inputCourtApplication, transformedJson);
        verifyUpdateApplication(inputCourtApplication, transformedJson, 1, 0);
    }
}
