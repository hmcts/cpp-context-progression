package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonpath.JsonPath.parse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.CourtApplicationVerificationHelper.verifyCourtApplication;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;
import uk.gov.justice.services.unifiedsearch.client.validation.JsonDocumentValidator;

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationReferredToCourtHearingTransformerTest {

    private final JoltTransformer joltTransformer = new JoltTransformer();

    private JsonDocumentValidator jsonValidator = new JsonDocumentValidator();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformApplicationReferredToCourtHearing() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.application-referred-to-court-hearing-spec.json");
        assertThat(specJson, is(notNullValue()));

        final JsonObject inputJson = readJson("/progression.event.application-referred-to-court-hearing.json");
        final DocumentContext inputDocumentContext = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        verifyCourtApplication(inputDocumentContext, transformedJson);
    }

    @Test
    public void shouldTransformApplicationReferredToCourtHearingForCourtOrder() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.application-referred-to-court-hearing-spec.json");
        assertThat(specJson, is(notNullValue()));

        final JsonObject inputJson = readJson("/progression.event.application-referred-to-court-hearing-for-court-order.json");
        final DocumentContext inputDocumentContext = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        jsonValidator.validate(transformedJson, "/json/schema/crime-case-index-schema.json");
        verifyCourtApplication(inputDocumentContext, transformedJson);
    }
}
