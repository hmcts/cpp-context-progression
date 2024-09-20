package uk.gov.moj.cpp.prosecution.casefile.transformation;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.moj.cpp.progression.domain.transformation.transformer.CdesEventsTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CdesEventsTransformerTest {

    public static final String PROGRESSION_COURT_DOCUMENT_ADDED = "progression.event.court-document-added";
    public static final String PROGRESSION_COURT_DOCUMENT_CREATED = "progression.event.court-document-created";
    public static final String PROGRESSION_COURT_PROCEDDINGS_INITIATED = "progression.event.court-proceedings-initiated";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private CdesEventsTransformer transformer;

    @Test
    public void shouldProcessProgressionCourtDocumentReceivedEvent() {

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_COURT_DOCUMENT_ADDED),
                readJson("progression.event.court-document-added.json", JsonValue.class));
        final Action action = whenTransformerActionIsCheckedFor(envelope);

        assertThat(action, is(TRANSFORM));
        verifyTransformation(envelope, "progression.event.court-document-added-transformed.json", PROGRESSION_COURT_DOCUMENT_ADDED);
    }

    @Test
    public void shouldProcessProgressionCourtDocumentCreatedEvent() {

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_COURT_DOCUMENT_CREATED),
                readJson("progression.event.court-document-created.json", JsonValue.class));
        final Action action = whenTransformerActionIsCheckedFor(envelope);

        assertThat(action, is(TRANSFORM));
        verifyTransformation(envelope, "progression.event.court-document-created-transformed.json", PROGRESSION_COURT_DOCUMENT_CREATED);
    }

    @Test
    public void shouldProcessProgressionCourtDocumentForNOWsCreatedEvent() {

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_COURT_DOCUMENT_CREATED),
                readJson("progression.event.court-document-created-for-nows.json", JsonValue.class));
        final Action action = whenTransformerActionIsCheckedFor(envelope);

        assertThat(action, is(TRANSFORM));
        verifyTransformation(envelope, "progression.event.court-document-created-for-nows-transformed.json", PROGRESSION_COURT_DOCUMENT_CREATED);
    }

    @Test
    public void shouldProcessProgressionCourtDocumentForPostalNotificationEvent() {

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_COURT_DOCUMENT_CREATED),
                readJson("progression.event.court-document-created-for-postal.json", JsonValue.class));
        final Action action = whenTransformerActionIsCheckedFor(envelope);

        assertThat(action, is(TRANSFORM));
        verifyTransformation(envelope, "progression.event.court-document-created-for-postal-transformed.json", PROGRESSION_COURT_DOCUMENT_CREATED);
    }

    @Test
    public void shouldProcessProgressionCourtReferralInitiatedEvent() {

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_COURT_PROCEDDINGS_INITIATED),
                readJson("progression.event.court-proceedings-initiated.json", JsonValue.class));
        final Action action = whenTransformerActionIsCheckedFor(envelope);

        assertThat(action, is(TRANSFORM));
        verifyTransformation(envelope, "progression.event.court-proceedings-initiated-transformed.json", PROGRESSION_COURT_PROCEDDINGS_INITIATED);
    }


    private void verifyTransformation(final JsonEnvelope envelope, final String expectedFileName, final String eventName, final Object... placeHolders) {
        final Stream<JsonEnvelope> jsonEnvelopeStream = transformer.apply(envelope);
        final List<JsonEnvelope> actual = jsonEnvelopeStream.collect(Collectors.toList());
        assertThat(actual, hasSize(1));

        final JsonObject expectedPayload = envelopeFrom(
                metadataWithRandomUUID(eventName),
                readJson(expectedFileName, JsonValue.class, placeHolders))
                .payloadAsJsonObject();

        assertThat(actual.get(0).payloadAsJsonObject(), is(expectedPayload));
    }

    private static <T> T readJson(final String jsonPath, final Class<T> clazz, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(jsonPath)) {
            return OBJECT_MAPPER.readValue(format(IOUtils.toString(systemResourceAsStream), placeholders), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Resource " + jsonPath + " inaccessible ", e);
        }
    }

    private Action whenTransformerActionIsCheckedFor(final JsonEnvelope envelope) {
        return transformer.actionFor(envelope);
    }
}
