package uk.gov.moj.cpp.progression.domain.transformation.anonymise;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.tools.eventsourcing.anonymization.util.FileUtil;

import java.io.StringReader;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

public class ProgressionEventTransformationTest {

    private AnonymiseUtil anonymiseUtil;
    private JsonPath inputJsonPath;
    private JsonObject anonymisedJsonObject;

    private static JsonObject jsonFromString(String jsonObjectStr) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    public void initialize(final String eventName) {
        anonymiseUtil = new AnonymiseUtil().apply(eventName);
        inputJsonPath = anonymiseUtil.getInputJsonPath();
        anonymisedJsonObject = anonymiseUtil.getAnonymisedJsonObject();
    }

    @Test
    public void progression_event_court_document_added() {
        initialize("progression.event.court-document-added");

        verifyCourtDocument();
    }


    @Test
    public void progression_event_court_document_created() {
        initialize("progression.event.court-document-created");

        verifyCourtDocument();
    }

    private void verifyCourtDocument() {
        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.courtDocument.courtDocumentId", is(inputJsonPath.getString("courtDocument.courtDocumentId"))),
                        withJsonPath("$.courtDocument.documentTypeId", is(inputJsonPath.getString("courtDocument.documentTypeId"))),
                        withJsonPath("$.courtDocument.documentTypeDescription", is(inputJsonPath.getString("courtDocument.documentTypeDescription")))
                        )
                ));
    }


    private static class AnonymiseUtil {
        private JsonPath inputJsonPath;
        private JsonObject inputJsonObject;
        private JsonObject anonymisedJsonObject;

        public JsonPath getInputJsonPath() {
            return inputJsonPath;
        }

        public JsonObject getAnonymisedJsonObject() {
            return anonymisedJsonObject;
        }

        public AnonymiseUtil apply(String eventName) {

            String fileContentsAsString = FileUtil.getFileContentsAsString(eventName+".json");
            inputJsonPath = JsonPath.from(fileContentsAsString);

            inputJsonObject = jsonFromString(fileContentsAsString);
            final JsonEnvelope jsonEnvelope = EnvelopeFactory.createEnvelope(eventName, inputJsonObject);
            ProgressionEventTransformation st = new ProgressionEventTransformation();
            Stream<JsonEnvelope> apply = st.apply(jsonEnvelope);
            JsonEnvelope envelope = apply.findFirst().get();
            anonymisedJsonObject = envelope.payloadAsJsonObject();
            return this;
        }
    }
}
