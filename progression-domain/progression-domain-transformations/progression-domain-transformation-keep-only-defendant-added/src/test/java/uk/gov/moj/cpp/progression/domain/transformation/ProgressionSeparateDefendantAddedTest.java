package uk.gov.moj.cpp.progression.domain.transformation;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;


public class ProgressionSeparateDefendantAddedTest {

    private ProgressionSeparateDefendantAdded underTest = new ProgressionSeparateDefendantAdded();

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
    public void shouldSetDeactivateActionForEventsThatMatch() {
        final JsonEnvelope event = buildEnvelope("progression.events.case-added-to-crown-court");

        assertThat(underTest.actionFor(event), is(new Action(true, false, false)));
    }

    @Test
    public void shouldSetNoActionForEventsThatDoNotMatch() {
        final JsonEnvelope event = buildEnvelope("progression.events.defendant-added");

        assertThat(underTest.actionFor(event), is(NO_ACTION));
    }

    @Test
    public void shouldSetNoActionForEventGenerateAfterGivenDate() {
        final JsonEnvelope event = buildEnvelope("progression.event.cases-referred-to-court");

        assertThat(underTest.actionFor(event), is(NO_ACTION));
    }

    @Test
    public void shouldSetNoActionForEventGenerateBeforeGivenDate() {

        final ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDate.of(2019, Month.JANUARY, 1), LocalTime.of(0, 0), ZoneId.of("UTC"));

        final JsonEnvelope event = buildEnvelope("progression.event.cases-referred-to-court", zonedDateTime);


        assertThat(underTest.actionFor(event), is(new Action(true, false, false)));
    }

    private JsonEnvelope buildEnvelope(final String eventName) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName).createdAt(ZonedDateTime.now()),
                createObjectBuilder().add("field", "value").build());
    }

    private JsonEnvelope buildEnvelope(final String eventName, final ZonedDateTime zonedDateTime) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName).createdAt(zonedDateTime),
                createObjectBuilder().add("field", "value").build());
    }

}