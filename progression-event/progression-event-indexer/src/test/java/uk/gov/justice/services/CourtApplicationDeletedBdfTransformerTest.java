package uk.gov.justice.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.CourtApplicationDeletedBdfTransformer.DELETED_APPLICATION_STATUS;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.validation.JsonDocumentValidator;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class CourtApplicationDeletedBdfTransformerTest {

    private final CourtApplicationDeletedBdfTransformer transformer = new CourtApplicationDeletedBdfTransformer();

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    private final JsonDocumentValidator jsonValidator = new JsonDocumentValidator();

    @Test
    public void shouldCreateDeletedApplicationEntryForEachLinkedCase() {

        final String applicationId = UUID.randomUUID().toString();
        final String caseId1 = UUID.randomUUID().toString();
        final String caseId2 = UUID.randomUUID().toString();

        final String inputJson = "{\"applicationId\":\"" + applicationId + "\",\"caseIds\":[\"" + caseId1 + "\",\"" + caseId2 + "\"]}";
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.getBytes()));

        final JsonObject output = objectToJsonObjectConverter.convert(transformer.transform(input));

        jsonValidator.validate(output, "/json/schema/crime-case-index-schema.json");

        final JsonArray caseDocuments = output.getJsonArray("caseDocuments");
        assertThat(caseDocuments.size(), is(2));

        final JsonObject firstCase = caseDocuments.getJsonObject(0);
        assertThat(firstCase.getString("caseId"), is(caseId1));
        assertThat(firstCase.getString("_case_type"), is("PROSECUTION"));

        final JsonArray applications = firstCase.getJsonArray("applications");
        assertThat(applications.size(), is(1));
        assertThat(applications.getJsonObject(0).getString("applicationId"), is(applicationId));
        assertThat(applications.getJsonObject(0).getString("applicationStatus"), is(DELETED_APPLICATION_STATUS));

        final JsonObject secondCase = caseDocuments.getJsonObject(1);
        assertThat(secondCase.getString("caseId"), is(caseId2));
        assertThat(secondCase.getJsonArray("applications").getJsonObject(0).getString("applicationId"), is(applicationId));
    }

    @Test
    public void shouldCreateNoCaseDocumentsWhenNoLinkedCases() {

        final String inputJson = "{\"applicationId\":\"" + UUID.randomUUID() + "\"}";
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.getBytes()));

        final JsonObject output = objectToJsonObjectConverter.convert(transformer.transform(input));

        jsonValidator.validate(output, "/json/schema/crime-case-index-schema.json");

        assertThat(output.getJsonArray("caseDocuments").isEmpty(), is(true));
    }
}
