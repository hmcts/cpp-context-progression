package uk.gov.moj.cpp.indexer.jolt;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.ProsecutionCaseDefendantListingStatusChangedVerificationHelper.validateApplicationOrCaseForListingStatusChanged;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.ProsecutionCaseDefendantListingStatusChangedVerificationHelper.verifyCasesAndApplicationsCount;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.Before;
import org.junit.Test;

public class ProsecutionCaseDefendantListingStatusChangedTransformationTest {


    private final JoltTransformer joltTransformer = new JoltTransformer();

    @Before
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformCaseDefendantListingStatusChanged() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.prosecution-case-defendant-listing-status-changed-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.prosecution-case-defendant-listing-status-changed.json");

        final DocumentContext hearingInput = JsonPath.parse(inputJson);
        final JsonArray courtApplications = hearingInput.read("$.hearing.courtApplications");
        final JsonArray prosecutionCases = hearingInput.read("$.hearing.prosecutionCases");

        final JsonObject outputCaseDocumentsJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);
        verifyCasesAndApplicationsCount(outputCaseDocumentsJson, courtApplications, prosecutionCases);

        final JsonArray outputHearings = outputCaseDocumentsJson.getJsonArray("caseDocuments");
        assertThat(outputHearings.size(), is(4));

        validateApplicationOrCaseForListingStatusChanged(hearingInput, courtApplications, outputHearings, "APPLICATION");
        validateApplicationOrCaseForListingStatusChanged(hearingInput, prosecutionCases, outputHearings, "PROSECUTION");
    }

}
