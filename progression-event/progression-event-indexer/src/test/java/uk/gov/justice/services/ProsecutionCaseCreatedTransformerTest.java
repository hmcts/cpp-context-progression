package uk.gov.justice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;

import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProsecutionCaseCreatedTransformerTest {

    @InjectMocks
    private ProsecutionCaseCreatedTransformer prosecutionCaseCreatedTransformer;


    @ParameterizedTest
    @CsvSource({"progression.event.prosecution-case-created.json", "progression.event.prosecution-case-created-with-migration-source-system.json"})
    public void shouldHandleExistingProsecutionCase(String fileName) {
        final JsonObject inputJson = readJson("/" + fileName);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final CaseDetails caseDetails  = (CaseDetails) prosecutionCaseCreatedTransformer.transform(input);
        assertEquals( "ACTIVE", caseDetails.getCaseStatus());
    }

    @Test
    public void shouldHandleExistingProsecutionWithMigrationCaseStatus() {
        final JsonObject inputJson = readJson("/progression.event.prosecution-case-created-with-migration-case-status.json");
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final CaseDetails caseDetails  = (CaseDetails) prosecutionCaseCreatedTransformer.transform(input);
        assertEquals( "INACTIVE", caseDetails.getCaseStatus());
    }
}