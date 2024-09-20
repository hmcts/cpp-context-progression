package uk.gov.moj.cpp.indexer.jolt;

import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;

import uk.gov.justice.services.DefendantsAddedToCourtProceedingTransformer;
import uk.gov.justice.services.DomainToIndexMapper;
import uk.gov.justice.services.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.indexer.jolt.verificationHelpers.BaseVerificationHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantsAddedToCourtProceedingTransformationTest {

    @Spy
    private DomainToIndexMapper domainToIndexMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private DefendantsAddedToCourtProceedingTransformer defendantsAddedToCourtProceedingTransformer;

    private BaseVerificationHelper verificationHelper = new BaseVerificationHelper();

    final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @BeforeEach
    public void setUp() {
        verificationHelper.resetCounts();
    }

    @Test
    public void shouldTransformDefendantsAddedToCourtProceedingWithMultiplePersonDefendantsJson() throws IOException {

        final JsonObject inputJson = readJson("/progression.event.defendant-added-to-court-proceedings-person-defendant-multiple-cases.json");
        final DocumentContext inputDC = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final JsonObject output = objectToJsonObjectConverter.convert(defendantsAddedToCourtProceedingTransformer.transform(input));

        verificationHelper.verifyProsecutionCase(inputDC, output, 0, 0);
        verificationHelper.verifyProsecutionCase(inputDC, output, 1, 1);

        verificationHelper.verifyCaseAndPartyCount(output, 0, 1);
        verificationHelper.verifyCaseAndPartyCount(output, 1, 1);

        verificationHelper.verifyDefendant(inputDC, output, 0, 0, 0);
        verificationHelper.verifyDefendant(inputDC, output, 1, 0, 1);

        verificationHelper.validateAliases(inputDC, output, 0, 0, 0, 0);
        verificationHelper.validateAliases(inputDC, output, 1, 0, 0, 0);

        verificationHelper.verifyCounts(2, 2, 2, 0, 2, 3, 2);
    }

    @Test
    public void shouldTransformDefendantsAddedToCourtProceedingWithMultipleLegalEntityDefendantsJson() throws IOException {

        final JsonObject inputJson = readJson("/progression.event.defendant-added-to-court-proceedings-legal-entity-defendant-multiple-cases.json");
        final DocumentContext inputDC = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final JsonObject output = objectToJsonObjectConverter.convert(defendantsAddedToCourtProceedingTransformer.transform(input));

        verificationHelper.verifyProsecutionCase(inputDC, output, 0, 0);
        verificationHelper.verifyProsecutionCase(inputDC, output, 1, 1);

        verificationHelper.verifyCaseAndPartyCount(output, 0, 1);
        verificationHelper.verifyCaseAndPartyCount(output, 1, 1);

        verificationHelper.verifyDefendant(inputDC, output, 0, 0, 0);
        verificationHelper.verifyDefendant(inputDC, output, 1, 0, 1);

        verificationHelper.validateAliases(inputDC, output, 0, 0, 0, 0);

        verificationHelper.verifyCounts(2, 2, 0, 2, 1, 3, 2);
    }

    @Test
    public void shouldTransformDefendantsAddedToCourtProceedingWithMultipleCasesWithMixedDefendantTypesLegalEntityDefendantsJson() throws IOException {
        final JsonObject inputJson = readJson("/progression.event.defendant-added-to-court-proceedings-person-defendant-and-legal-entity-defendant-multiple-cases.json");

        final DocumentContext inputDC = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final Object object = defendantsAddedToCourtProceedingTransformer.transform(input);
        final JsonObject output = objectToJsonObjectConverter.convert(object);

        verificationHelper.verifyProsecutionCase(inputDC, output, 0, 0);
        verificationHelper.verifyProsecutionCase(inputDC, output, 1, 1);

        verificationHelper.verifyCaseAndPartyCount(output, 0, 1);
        verificationHelper.verifyCaseAndPartyCount(output, 1, 1);

        verificationHelper.verifyDefendant(inputDC, output, 0, 0, 0);
        verificationHelper.verifyDefendant(inputDC, output, 1, 0, 1);

        verificationHelper.validateAliases(inputDC, output, 0, 0, 0, 0);

        verificationHelper.verifyCounts(2, 2, 1, 1, 1, 3, 2);
    }
}