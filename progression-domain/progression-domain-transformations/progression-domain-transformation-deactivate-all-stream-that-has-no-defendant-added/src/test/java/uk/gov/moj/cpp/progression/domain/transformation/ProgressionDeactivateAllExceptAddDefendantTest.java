package uk.gov.moj.cpp.progression.domain.transformation;

import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

public class ProgressionDeactivateAllExceptAddDefendantTest {

    private ProgressionDeactivateAllExceptAddDefendant underTest = new ProgressionDeactivateAllExceptAddDefendant();

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

        assertThat(underTest.actionFor(event), is(DEACTIVATE));
    }

    @Test
    public void shouldSetDeactivateActionForEventsThatMatch() {
        final JsonEnvelope event = buildEnvelope("progression.events.case-added-to-crown-court");

        assertThat(underTest.actionFor(event), is(DEACTIVATE));
    }

    @Test
    public void shouldSetNoActionForEventsThatDoNotMatch() {
        final JsonEnvelope event = buildEnvelope("progression.events.defendant-added");

        assertThat(underTest.actionFor(event), is(NO_ACTION));
    }

    private JsonEnvelope buildEnvelope(final String eventName) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName),
                createObjectBuilder().add("field", "value").build());
    }

}
