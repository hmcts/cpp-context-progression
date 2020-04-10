package uk.gov.moj.cpp.progression.domain.transformation.corechanges;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_APPLICATION_REFERRED_TO_COURT;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.moj.cpp.progression.domain.transformation.corechanges.transform.ProgressionEventTransformer;
import uk.gov.moj.cpp.progression.domain.transformation.corechanges.transform.TransformFactory;

import java.util.Arrays;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionEventStreamTransformTest {

    private final ProgressionEventStreamTransform underTest = new ProgressionEventStreamTransform();

    @Mock
    private TransformFactory transformFactory;

    @Before
    public void setup() {
        underTest.setTransformFactory(transformFactory);
        when(transformFactory.getEventTransformer(PROGRESSION_APPLICATION_REFERRED_TO_COURT)).thenReturn(Arrays.asList(mock(ProgressionEventTransformer.class)));
    }

    @Test
    public void shouldCreateInstanceOfEventTransformation() {
        assertThat(underTest, is(instanceOf(EventTransformation.class)));
    }

    @Test
    public void shouldSetActionToTransformForTheEventsThatMatch() {
        final JsonEnvelope event = buildEnvelope(PROGRESSION_APPLICATION_REFERRED_TO_COURT);
        assertThat(underTest.actionFor(event), is(TRANSFORM));
    }

    @Test
    public void shouldSetActionToNoActionForTheEventsThatDoesNotMatch() {
        final JsonEnvelope event = buildEnvelope("progression.events.other");
        assertThat(underTest.actionFor(event), is(NO_ACTION));
    }

    @Test
    public void shouldTransformProgressionInitiatedEvent() {
        final JsonObject jsonObject = mock(JsonObject.class);
        final JsonEnvelope event = buildEnvelopeWithPayload(PROGRESSION_APPLICATION_REFERRED_TO_COURT, jsonObject);
        final ProgressionEventTransformer progressionEventTransformer = mock(ProgressionEventTransformer.class);

        when(transformFactory.getEventTransformer(PROGRESSION_APPLICATION_REFERRED_TO_COURT)).thenReturn(Arrays.asList(progressionEventTransformer));

        underTest.apply(event);

        verify(progressionEventTransformer).transform(event.metadata(), jsonObject);

    }

    private JsonEnvelope buildEnvelope(final String eventName) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName),
                createObjectBuilder().add("field", "value").build());
    }


    private JsonEnvelope buildEnvelopeWithPayload(final String eventName, final JsonObject jsonObject) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName),
                jsonObject);
    }
}
