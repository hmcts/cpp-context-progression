package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.JsonPath.parse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HearingResultedApplicationUpdatedTransformerTest {

    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformHearingUpdatedForAllocationFieldsJson() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.hearing-resulted-application-updated-spec.json");
        assertThat(specJson, is(notNullValue()));

        final JsonObject inputJson = readJson("/progression.event.hearing-updated-for-allocation-fields.json");
        final DocumentContext inputCourtApplication = parse(inputJson);
        final JsonObject outputCase = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        with(outputCase.toString())
                .assertThat("$.caseDocuments.[0].caseId", equalTo(((JsonString) inputCourtApplication.read("$.courtApplication.courtApplicationCases[0].prosecutionCaseId")).getString()))
                .assertThat("$.caseDocuments.[0].applications.[0].applicationReference", equalTo(((JsonString) inputCourtApplication.read("$.courtApplication.applicationReference")).getString()))
                .assertThat("$.caseDocuments.[0].applications.[0].receivedDate", equalTo(((JsonString) inputCourtApplication.read("$.courtApplication.applicationReceivedDate")).getString()))
                .assertThat("$.caseDocuments.[0].applications.[0].applicationStatus", equalTo(((JsonString) inputCourtApplication.read("$.courtApplication.applicationStatus")).getString()))
                .assertThat("$.caseDocuments.[0].applications.[0].applicationExternalCreatorType", equalTo(((JsonString) inputCourtApplication.read("$.courtApplication.applicationExternalCreatorType")).getString()));
    }
}
