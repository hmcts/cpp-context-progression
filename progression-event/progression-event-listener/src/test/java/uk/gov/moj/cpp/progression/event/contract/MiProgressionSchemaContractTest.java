package uk.gov.moj.cpp.progression.event.contract;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonSchemaValidationMatcher;

import java.io.IOException;
import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Validates progression event payloads against the JSON schemas published by MI via the
 * mireportdata-progression-event-listener:schema-resources jar. Schemas are loaded from
 * /schema.validation/ on the classpath. Runs under progression's own framework version,
 * avoiding binary coupling between the two repos.
 */
public class MiProgressionSchemaContractTest {

    private static final String ASSERT_ERROR_MESSAGE = "MI schema contract validation has failed for event %s. "
            + "Please inform MI team about the event schema change and consumer test failure";

    @ParameterizedTest
    @ValueSource(strings = {
            "progression.event.financial-means-deleted",
            "progression.event.prosecution-case-created",
            "progression.event.defendants-added-to-court-proceedings",
            "progression.event.prosecution-case-defendant-updated",
            "progression.event.case-ejected",
            "progression.event.hearing-resulted",
            "progression.event.court-application-created",
            "progression.event.court-application-updated",
            "progression.event.court-application-status-changed",
            "progression.event.court-application-rejected",
            "progression.event.application-ejected",
            "progression.event.court-application-proceedings-edited",
            "progression.event.case-markers-updated",
            "progression.events.offences-for-defendant-changed",
            "progression.event.master-defendant-id-updated",
            "progression.event.defendant-unmatched",
            "progression.event.link-cases",
            "progression.event.merge-cases",
            "progression.event.split-cases",
            "progression.event.cases-unlinked",
            "progression.event.case-note-added-v2",
            "progression.event.case-note-edited-v2",
            "progression.event.court-document-added",
            "progression.event.court-document-removed",
            "progression.event.court-document-updated",
            "progression.event.civil-fees-updated",
            "progression.event.civil-fees-added",
            "progression.event.case-group-info-updated",
            "progression.event.case-removed-from-group-cases"
    })
    public void shouldValidateMiSchemaContract(final String eventName) throws IOException {
        JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(eventName).createdAt(now()),
                givenPayload("/schema.validation/" + eventName + ".json"));
        assertThat(format(ASSERT_ERROR_MESSAGE, eventName), envelope,
                JsonSchemaValidationMatcher.isValidJsonEnvelopeForSchema());
    }

    private static JsonObject givenPayload(final String filePath) throws IOException {
        try (InputStream inputStream = MiProgressionSchemaContractTest.class.getResourceAsStream(filePath)) {
            JsonReader jsonReader = Json.createReader(inputStream);
            return jsonReader.readObject();
        }
    }
}
