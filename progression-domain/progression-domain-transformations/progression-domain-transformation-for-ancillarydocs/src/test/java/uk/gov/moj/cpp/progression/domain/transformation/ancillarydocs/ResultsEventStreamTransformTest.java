package uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.SchemaVariableConstants.PROGRESSION_EVENT_HEARING_RESULTED;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultsEventStreamTransformTest {

    private ProgressionEventStreamTransform underTest = new ProgressionEventStreamTransform();

    @Mock
    private AttendanceDayEventTransformer attendanceDayEventTransformer;

    @Before
    public void setup() {
        underTest.setAttendanceDayEventTransformer(attendanceDayEventTransformer);
    }

    @Test
    public void shouldCreateInstanceOfEventTransformation() {
        assertThat(underTest, is(instanceOf(EventTransformation.class)));
    }

    @Test
    public void shouldSetActionToTransformForTheEventsThatMatch() {
        final JsonEnvelope event = buildEnvelope(PROGRESSION_EVENT_HEARING_RESULTED);
        assertThat(underTest.actionFor(event), is(TRANSFORM));
    }

    @Test
    public void shouldSetActionToNoActionForTheEventsThatDoesNotMatch() {
        final JsonEnvelope event = buildEnvelope("hearing.events.other");
        assertThat(underTest.actionFor(event), is(NO_ACTION));
    }

    @Test
    public void shouldTransformHearingInitiatedEvent() {
        JsonObject jsonObject = mock(JsonObject.class);
        final JsonEnvelope event = buildEnvelopeWithPayload(PROGRESSION_EVENT_HEARING_RESULTED, jsonObject);

        underTest.apply(event);

        verify(attendanceDayEventTransformer).transform(PROGRESSION_EVENT_HEARING_RESULTED, jsonObject);

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