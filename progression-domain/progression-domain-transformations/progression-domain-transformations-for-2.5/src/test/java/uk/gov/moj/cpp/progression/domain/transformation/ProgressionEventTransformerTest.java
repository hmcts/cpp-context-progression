package uk.gov.moj.cpp.progression.domain.transformation;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.moj.cpp.progression.domain.transformation.util.TransformationEventHelper;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.IOException;
import java.io.InputStream;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

public class ProgressionEventTransformerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionEventTransformerTest.class);

    private static final String DEFENDANT_CASE_OFFENCES = "defendantCaseOffences";

    private final ProgressionEventTransformer underTest = new ProgressionEventTransformer();

    private Enveloper enveloper = createEnveloper();

    @Before
    public void setup() {
        underTest.setEnveloper(enveloper);
    }

    @Test
    public void shouldCreateInstanceOfEventTransformation() {
        assertThat(underTest, is(instanceOf(EventTransformation.class)));
    }

    @Test
    public void shouldSetActionToTransformForTheEventsThatMatch() {
        final JsonEnvelope event = buildEnvelope("progression.event.prosecution-case-offences-updated");
        assertThat(underTest.actionFor(event), is(TRANSFORM));
    }

    @Test
    public void shouldSetActionToNoActionForTheEventsThatDoesNotMatch() {
        final JsonEnvelope event = buildEnvelope("hearing.events.other");
        assertThat(underTest.actionFor(event), is(NO_ACTION));
    }

    @Test
    public void shouldTransformProsecutionCaseOffenceUpdatedEventRequiredFields() {
        final JsonEnvelope event = buildEnvelope("progression.event.prosecution-case-offences-updated", "progression.event.prosecution-case-offences-updated.json");
        final JsonObject defendantCaseOffence = event.payloadAsJsonObject().getJsonObject(DEFENDANT_CASE_OFFENCES);
        final JsonObject expected = underTest.apply(event).findFirst().get().payloadAsJsonObject().getJsonObject(DEFENDANT_CASE_OFFENCES);
        LOGGER.info("progression.event.prosecution-case-offences-updated -- {}", expected.toString());
        assertThat(expected.getString("defendantId"), is(defendantCaseOffence.getString("defendantId")));
        assertThat(expected.getString("prosecutionCaseId"), is(defendantCaseOffence.getString("prosecutionCaseId")));
        assertThat(expected.getJsonArray("offences").size(), is(defendantCaseOffence.getJsonArray("offences").size()));
    }

    @Test
    public void shouldTransformProsecutionCaseOffenceUpdatedEventAllFields() {
        final JsonEnvelope event = buildEnvelope("progression.event.prosecution-case-offences-updated", "progression.event.prosecution-case-offences-updated_All.json");
        final JsonObject defendantCaseOffence = event.payloadAsJsonObject().getJsonObject(DEFENDANT_CASE_OFFENCES);
        final JsonObject expected = underTest.apply(event).findFirst().get().payloadAsJsonObject().getJsonObject(DEFENDANT_CASE_OFFENCES);
        LOGGER.info("progression.event.prosecution-case-offences-updated-{}", expected.toString());
        assertThat(expected.getString("defendantId"), is(defendantCaseOffence.getString("defendantId")));
        assertThat(expected.getString("prosecutionCaseId"), is(defendantCaseOffence.getString("prosecutionCaseId")));
        assertThat(expected.getJsonArray("offences").size(), is(defendantCaseOffence.getJsonArray("offences").size()));
    }

    @Test
    public void shouldTransformListingStatusChangedEventRequiredFields() {
        final JsonEnvelope event = buildEnvelope("prosecutionCase-defendant-listing-status-changed", "progression.event.prosecutionCase-defendant-listing-status-changed.json");
        final JsonObject jsonObject = event.payloadAsJsonObject();
        final JsonObject expected = underTest.apply(event).findFirst().get().payloadAsJsonObject();
        LOGGER.info("prosecutionCase-defendant-listing-status-changed-{}", expected.toString());
        assertThat(expected.getString("hearingListingStatus"), is(jsonObject.getString("hearingListingStatus")));
        assertThat(expected.getJsonObject("hearing").getString("id"), is(jsonObject.getJsonObject("hearing").getString("id")));
        assertThat(expected.getJsonObject("hearing").getJsonArray("prosecutionCases").size(), is(jsonObject.getJsonObject("hearing").getJsonArray("prosecutionCases").size()));
    }

    @Test
    public void shouldTransformProsecutionCaseCreatedEventRequiredFields() {
        final JsonEnvelope event = buildEnvelope("progression.event.prosecution-case-created", "progression.event.prosecution-case-created.json");
        final JsonObject jsonObject = event.payloadAsJsonObject();
        final JsonObject expected = underTest.apply(event).findFirst().get().payloadAsJsonObject();
        LOGGER.info("progression.event.prosecution-case-created-{}", expected.toString());
        assertThat(expected.getJsonObject("prosecutionCase").getString("id"), is(jsonObject.getJsonObject("prosecutionCase").getString("id")));
        assertThat(expected.getJsonObject("prosecutionCase").getJsonArray("defendants").size(), is(jsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").size()));
    }

    @Test
    public void shouldTransformHearingResultedEventRequiredFields() {
        final JsonEnvelope event = buildEnvelope("progression.event.hearing-resulted", "progression.event.hearing-resulted.json");
        final JsonObject jsonObject = event.payloadAsJsonObject();
        final JsonObject expected = underTest.apply(event).findFirst().get().payloadAsJsonObject();
        LOGGER.info("progression.event.hearing-resulted-{}", expected.toString());
        assertThat(expected.getJsonObject("hearing").getString("id"), is(jsonObject.getJsonObject("hearing").getString("id")));
        assertThat(expected.getJsonObject("hearing").getJsonArray("prosecutionCases").size(), is(jsonObject.getJsonObject("hearing").getJsonArray("prosecutionCases").size()));
    }

    @Test
    public void shouldNotThrowUnsupportedExceptionWhileTransformHearingResulted() {
        final JsonEnvelope event = buildEnvelope("progression.event.hearing-resulted", "progression.event.hearing-resulted-unsupported-exception.json");
        final JsonObject jsonObject = event.payloadAsJsonObject();
        final JsonObject expected = underTest.apply(event).findFirst().get().payloadAsJsonObject();
        assertThat(expected.getJsonObject("hearing").getString("id"), is(jsonObject.getJsonObject("hearing").getString("id")));
        final JsonObject judicialResult = expected.getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("judicialResults").getJsonObject(0);
        assertThat(judicialResult.getString("label"), is(jsonObject.getJsonObject("hearing").getJsonArray("sharedResultLines").getJsonObject(3).getString("label")));
    }

    @Test
    public void shouldNotThrowNullPointerExceptionWhileTransformHearingResulted() {
        final JsonEnvelope event = buildEnvelope("progression.event.hearing-resulted", "progression.event.hearing-resulted-nullpointer-exception.json");
        JsonEnvelope transformedEvent = new TransformationEventHelper().buildHearingResultedTransformedPayload(event, "progression.event.hearing-resulted");
    }

    private JsonEnvelope buildEnvelope(final String eventName, final String payloadFileName) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream stream = loader.getResourceAsStream(payloadFileName); final JsonReader jsonReader = Json.createReader(stream)) {
            final JsonObject payload = jsonReader.readObject();
            return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(eventName), payload);
        } catch (final IOException e) {
            LOGGER.warn("Error in reading payload {}", payloadFileName, e);
        }
        return null;
    }

    private JsonEnvelope buildEnvelope(final String eventName) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName),
                createObjectBuilder().add("field", "value").build());
    }
}