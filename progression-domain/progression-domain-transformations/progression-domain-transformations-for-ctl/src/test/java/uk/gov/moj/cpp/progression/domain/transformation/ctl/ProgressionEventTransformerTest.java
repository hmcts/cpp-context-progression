package uk.gov.moj.cpp.progression.domain.transformation.ctl;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.moj.cpp.coredomain.transform.transforms.BailStatusEnum2ObjectTransformer;

import java.io.IOException;
import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(MockitoJUnitRunner.class)
public class ProgressionEventTransformerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionEventTransformerTest.class);

    private ProgressionEventTransformer target = new ProgressionEventTransformer();

    private Enveloper enveloper = createEnveloper();

    @Before
    public void setup() {
        target.setEnveloper(enveloper);
    }

    @Test
    public void shouldCreateInstanceOfEventTransformation() {
        assertThat(target, is(instanceOf(EventTransformation.class)));
    }


    @Test
    public void shouldSetActionToNoActionForTheEventsThatDoesNotMatch() {
        final JsonEnvelope event = buildEnvelope("hearing.events.other");
        assertThat(target.actionFor(event), is(NO_ACTION));
    }


    @Test
    public void shouldTransformProgressionEventHearingResulted() {

        final JsonObject inputPayload = mock(JsonObject.class);

        final JsonObject transformedPayload = mock(JsonObject.class);

        final BailStatusEnum2ObjectTransformer bailStatusTransformer = mock(BailStatusEnum2ObjectTransformer.class);

        target.setBailStatusTransformer(bailStatusTransformer);

        final JsonEnvelope event = buildEnvelope("progression.event.hearing-resulted", inputPayload);

        when(bailStatusTransformer.transform(Mockito.anyObject())).thenReturn(transformedPayload);

        final JsonEnvelope expected = target.apply(event).findFirst().get();

        verify(bailStatusTransformer).transform(Mockito.anyObject());

        assertNotNull(expected);

    }

    @Test
    public void shouldTransformBailStatusForProsecutionCaseCreatedEvent() {
        final JsonEnvelope event = buildEnvelope("progression.event.prosecution-case-created", "progression.event.prosecution-case-created.json");
        final JsonObject jsonObject = event.payloadAsJsonObject();
        final JsonObject expected = target.apply(event).findFirst().get().payloadAsJsonObject();
        LOGGER.info("progression.event.prosecution-case-created-{}", expected.toString());
        assertThat(expected.getJsonObject("prosecutionCase").getString("id"), is(jsonObject.getJsonObject("prosecutionCase").getString("id")));
        assertThat(expected.getJsonObject("prosecutionCase").getJsonArray("defendants").size(), is(jsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").size()));
        assertThat(expected.getJsonObject("prosecutionCase").
                getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant")
                .getJsonObject("bailStatus").getString("id"), is("02e69486-4d01-3403-a50a-7419ca040635"));
        assertThat(expected.getJsonObject("prosecutionCase").
                getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant")
                .getJsonObject("bailStatus").getString("code"), is("C"));
        assertThat(expected.getJsonObject("prosecutionCase").
                getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant")
                .getJsonObject("bailStatus").getString("description"), is("Custody or remanded into custody"));

    }


    @Test
    public void shouldTransformBailStatusForProsecutionDefendantUpdatedEvent() {
        final JsonEnvelope event = buildEnvelope("progression.events.defendant-updated", "progression.events.defendant-updated.json");
        final JsonObject jsonObject = event.payloadAsJsonObject();
        final JsonObject expected = target.apply(event).findFirst().get().payloadAsJsonObject();
        LOGGER.info("progression.events.defendant-updated-{}", expected.toString());
        assertThat(expected.getJsonObject("bailStatus").getString("id"), is("eaf18bf8-9569-3656-a4ab-64299f9bd513"));
        assertThat(expected.getJsonObject("bailStatus").getString("code"), is("U"));
        assertThat(expected.getJsonObject("bailStatus").getString("description"), is("Unconditional Bail"));

    }

    @Test
    public void shouldTransformBailStatusForHearingResultedEvent() {
        final JsonEnvelope event = buildEnvelope("progression.event.hearing-resulted", "progression.event.hearing-resulted.json");
        final JsonObject jsonObject = event.payloadAsJsonObject();
        final JsonObject expected = target.apply(event).findFirst().get().payloadAsJsonObject();
        LOGGER.info("progression.event.hearing-resulted-{}", expected.toString());
        assertThat(expected.getJsonObject("hearing").getString("id"), is(jsonObject.getJsonObject("hearing").getString("id")));
        assertThat(expected.getJsonObject("hearing").getJsonArray("prosecutionCases").size(),
                is(jsonObject.getJsonObject("hearing").getJsonArray("prosecutionCases").size()));

        assertThat(expected.getJsonObject("hearing").
                getJsonArray("prosecutionCases").getJsonObject(0).
                getJsonArray("defendants").getJsonObject(0).
                getJsonObject("personDefendant").getJsonObject("bailStatus").getString("id"), is("0d4073b6-22be-3875-9d63-5da286bb3ece"));

        assertThat(expected.getJsonObject("hearing").
                getJsonArray("prosecutionCases").getJsonObject(0).
                getJsonArray("defendants").getJsonObject(0).
                getJsonObject("personDefendant").getJsonObject("bailStatus").getString("code"), is("B"));

        assertThat(expected.getJsonObject("hearing").
                getJsonArray("prosecutionCases").getJsonObject(0).
                getJsonArray("defendants").getJsonObject(0).
                getJsonObject("personDefendant").getJsonObject("bailStatus").getString("description"), is("Conditional Bail"));
    }


    private JsonEnvelope buildEnvelope(final String eventName, final JsonObject jsonPayload) {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(eventName), jsonPayload);
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