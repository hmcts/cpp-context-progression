package uk.gov.justice.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.validation.JsonDocumentValidator;
import uk.gov.moj.cpp.indexer.jolt.verificationHelpers.HearingVerificationHelper;

import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingInitiateEnrichedTransformerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private HearingInitiateEnrichedTransformer hearingInitiateEnrichedTransformer;

    private HearingVerificationHelper hearingVerificationHelper = new HearingVerificationHelper();


    final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    private JsonDocumentValidator jsonValidator = new JsonDocumentValidator();

    @BeforeEach
    public void setUp() {
        hearingVerificationHelper.resetCounts();
    }

    @Test
    public void shouldNotTransformHearingInitiateEnrichedWhenNoApplicationOnHearing() {

        final JsonObject inputJson = readJson("/progression.hearing-initiate-enriched-for-case.json");
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final JsonObject output = objectToJsonObjectConverter.convert(hearingInitiateEnrichedTransformer.transform(input));

        assertThat(output.getJsonArray("caseDocuments").size(), is(0));
    }

    @Test
    public void shouldNotTransformHearingInitiateEnrichedWhenApplicationHasNoCourtOrderOnHearing() {

        final JsonObject inputJson = readJson("/progression.hearing-initiate-enriched-for-application-without-courtorder.json");
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final JsonObject output = objectToJsonObjectConverter.convert(hearingInitiateEnrichedTransformer.transform(input));

        assertThat(output.getJsonArray("caseDocuments").size(), is(0));
    }

    @Test
    public void shouldTransformHearingInitiateEnrichedWhenApplicationHasCourtOrderOnHearing() {

        final JsonObject inputJson = readJson("/progression.hearing-initiate-enriched.json");
        final DocumentContext inputDC = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final JsonObject output = objectToJsonObjectConverter.convert(hearingInitiateEnrichedTransformer.transform(input));

        jsonValidator.validate(output, "/json/schema/crime-case-index-schema.json");

        hearingVerificationHelper.verifyApplication(inputDC, output, 0, "$.hearing.courtApplications[0]");

        hearingVerificationHelper.verifyHearings(inputDC, output, 0, 0);

        hearingVerificationHelper.verifyCounts(1, 1, 0);

    }

}