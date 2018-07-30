package uk.gov.moj.cpp.progression.domain.transformation;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;

import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;


public class ProgressionEventStreamArchiverTest {

    private ProgressionEventStreamArchiver underTest = new ProgressionEventStreamArchiver();

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
    public void shouldSetCustomActionForEventsThatMatch() {
        final JsonEnvelope event = buildEnvelope("progression.events.case-added-to-crown-court.archived.1.9.release");

        assertThat(underTest.actionFor(event), is(new Action(true, true, false)));
    }

    @Test
    public void shouldSetDeactivateActionForEventsThatMatch() {
        final JsonEnvelope event = buildEnvelope("progression.events.case-added-to-crown-court");

        assertThat(underTest.actionFor(event), is(DEACTIVATE));
    }

    @Test
    public void shouldSetNoActionForEventsThatDoNotMatch() {
        final JsonEnvelope event = buildEnvelope("dummy.sample.v2.events.name");

        assertThat(underTest.actionFor(event), is(NO_ACTION));
    }

    @Test
    public void shouldCreateTransformation() {
        final JsonEnvelope event = buildEnvelope("progression.events.case-added-to-crown-court.archived.1.9.release");

        final Stream<JsonEnvelope> transformedStream = underTest.apply(event);

        final List<JsonEnvelope> transformedEvents = transformedStream.collect(toList());
        assertThat(transformedEvents, hasSize(1));
        assertThat(transformedEvents.get(0).metadata().name(), is("progression.events.case-added-to-crown-court"));
        assertThat(transformedEvents.get(0).payloadAsJsonObject().getString("field"),
                is(event.payloadAsJsonObject().getString("field")));
    }

    private JsonEnvelope buildEnvelope(final String eventName) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName),
                createObjectBuilder().add("field", "value").build());
    }

}
