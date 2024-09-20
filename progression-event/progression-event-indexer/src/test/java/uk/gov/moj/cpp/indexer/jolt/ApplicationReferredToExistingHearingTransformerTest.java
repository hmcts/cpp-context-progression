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

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationReferredToExistingHearingTransformerTest {

    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformApplicationReferredToExistingHearing() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.application-referral-to-existing-hearing-spec.json");
        assertThat(specJson, is(notNullValue()));

        final JsonObject inputJson = readJson("/progression.event.application-referral-to-existing-hearing.json");
        final DocumentContext inputDocumentContext = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        verifyCourtApplication(inputDocumentContext, transformedJson);
    }

}
