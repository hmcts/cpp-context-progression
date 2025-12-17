package uk.gov.justice.services;

import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.validation.JsonDocumentValidator;
import uk.gov.moj.cpp.indexer.jolt.verificationHelpers.HearingVerificationHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
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
public class DefendantsListingStatusChangedV3TransformerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private DefendantsListingStatusChangedV3Transformer defendantsListingStatusChangedTransformer;

    private HearingVerificationHelper hearingVerificationHelper = new HearingVerificationHelper();


    final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Inject
    private JsonDocumentValidator jsonValidator = new JsonDocumentValidator();

    @BeforeEach
    public void setUp() {
        hearingVerificationHelper.resetCounts();
    }

    @Test
    public void shouldTransformDefendantsListingStatusChangedTransformer() throws IOException {

        final JsonObject inputJson = readJson("/progression.event.prosecution-case-defendant-listing-status-changed.json");
        final DocumentContext inputDC = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final JsonObject output = objectToJsonObjectConverter.convert(defendantsListingStatusChangedTransformer.transform(input));

        hearingVerificationHelper.verifyProsecutionCase(inputDC, output, 0, "$.hearing.prosecutionCases[0]");
        hearingVerificationHelper.verifyProsecutionCase(inputDC, output, 3, "$.hearing.prosecutionCases[1]");

        hearingVerificationHelper.verifyApplication(inputDC, output, 1, "$.hearing.courtApplications[0]");
        hearingVerificationHelper.verifyApplication(inputDC, output, 2, "$.hearing.courtApplications[1]");

        hearingVerificationHelper.verifyHearings(inputDC, output, 0, 0);
        hearingVerificationHelper.verifyHearings(inputDC, output, 1, 0);
        hearingVerificationHelper.verifyHearings(inputDC, output, 2, 0);
        hearingVerificationHelper.verifyHearings(inputDC, output, 3, 0);

        hearingVerificationHelper.verifyCounts(4, 4, 0);
    }

    @Test
    public void shouldTransformDefendantsListingStatusChangedTransformerWithoutCourtCentreInHearingDays() {

        final JsonObject inputJson = readJson("/progression.event.prosecution-case-defendant-listing-status-changed-without-court-centre.json");
        final DocumentContext inputDC = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final JsonObject output = objectToJsonObjectConverter.convert(defendantsListingStatusChangedTransformer.transform(input));

        jsonValidator.validate(output, "/json/schema/crime-case-index-schema.json");

        hearingVerificationHelper.verifyProsecutionCase(inputDC, output, 0, "$.hearing.prosecutionCases[0]");
        hearingVerificationHelper.verifyProsecutionCase(inputDC, output, 3, "$.hearing.prosecutionCases[1]");
        hearingVerificationHelper.verifyApplication(inputDC, output, 1, "$.hearing.courtApplications[0]");
        hearingVerificationHelper.verifyApplication(inputDC, output, 2, "$.hearing.courtApplications[1]");

        hearingVerificationHelper.verifyHearingsWithoutCourtCentre(inputDC, output, 0, 0);
        hearingVerificationHelper.verifyHearingsWithoutCourtCentre(inputDC, output, 1, 0);
        hearingVerificationHelper.verifyHearingsWithoutCourtCentre(inputDC, output, 2, 0);
        hearingVerificationHelper.verifyHearingsWithoutCourtCentre(inputDC, output, 3, 0);
        hearingVerificationHelper.verifyCounts(4, 4, 0);
    }

    @Test
    public void shouldTransformDefendantsListingStatusChangedTransformerNPEBug() throws IOException {

        final JsonObject inputJson = readJson("/progression.event.prosecution-case-defendant-listing-status-changed-npe-bug.json");
        final DocumentContext inputDC = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final JsonObject output = objectToJsonObjectConverter.convert(defendantsListingStatusChangedTransformer.transform(input));

        hearingVerificationHelper.verifyApplication(inputDC, output, 0, "$.hearing.courtApplications[0]");
        hearingVerificationHelper.verifyHearingsWithoutCourtCentre(inputDC, output, 0, 0);
        hearingVerificationHelper.verifyCounts(1, 1, 0);
    }

    @Test
    public void shouldTransformDefendantsListingStatusChangedWithLinkedApplication() throws IOException {

        final JsonObject inputJson = readJson("/progression.event.prosecution-case-defendant-listing-status-changed-with-linked-applications.json");
        final DocumentContext inputDC = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final JsonObject output = objectToJsonObjectConverter.convert(defendantsListingStatusChangedTransformer.transform(input));

        hearingVerificationHelper.verifyProsecutionCase(inputDC, output, 0, "$.hearing.prosecutionCases[0]");
        hearingVerificationHelper.verifyApplication(inputDC, output, 0, "$.hearing.courtApplications[0]", 0);
        hearingVerificationHelper.verifyApplication(inputDC, output, 0, "$.hearing.courtApplications[1]", 1);
        hearingVerificationHelper.verifyHearings(inputDC, output, 0, 0);
        hearingVerificationHelper.verifyCounts(1, 1, 0);
    }
}