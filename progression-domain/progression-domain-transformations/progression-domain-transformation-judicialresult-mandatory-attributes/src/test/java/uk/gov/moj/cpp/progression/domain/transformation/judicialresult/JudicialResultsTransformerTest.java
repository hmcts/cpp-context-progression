package uk.gov.moj.cpp.progression.domain.transformation.judicialresult;

import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.APPLICATION_REFERRED_TO_COURT;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.BOXWORK_APPLICATION_REFERRED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.COURT_APPLICATION_ADDED_TO_CASE;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.COURT_APPLICATION_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.COURT_APPLICATION_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_APPLICATION_LINK_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_CONFIRMED_CASE_STATUS_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_EXTENDED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_INITIATE_ENRICHED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_RESULTED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_RESULTED_CASE_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.LISTED_COURT_APPLICATION_CHANGED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_OFFENCES_UPDATED;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.moj.cpp.progression.domain.transformation.judicialresult.service.EventPayloadTransformer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(DataProviderRunner.class)
public class JudicialResultsTransformerTest {

    private static final String JUDICIAL_RESULTS_KEYWORD = "judicialResults";

    @Mock
    private EventPayloadTransformer eventPayloadTransformer;

    private JudicialResultsTransformer underTest;

    @DataProvider
    public static Object[][] validEventToTransform() {
        return new Object[][]{
                {BOXWORK_APPLICATION_REFERRED.getEventName()},
                {HEARING_EXTENDED.getEventName()},
                {HEARING_RESULTED.getEventName()},
                {COURT_APPLICATION_UPDATED.getEventName()},
                {PROSECUTION_CASE_OFFENCES_UPDATED.getEventName()},
                {PROSECUTION_CASE_DEFENDANT_UPDATED.getEventName()},
                {HEARING_RESULTED_CASE_UPDATED.getEventName()},
                {HEARING_INITIATE_ENRICHED.getEventName()},
                {COURT_APPLICATION_CREATED.getEventName()},
                {COURT_APPLICATION_ADDED_TO_CASE.getEventName()},
                {LISTED_COURT_APPLICATION_CHANGED.getEventName()},
                {APPLICATION_REFERRED_TO_COURT.getEventName()},
                {HEARING_APPLICATION_LINK_CREATED.getEventName()},
                {PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED.getEventName()},
                {HEARING_CONFIRMED_CASE_STATUS_UPDATED.getEventName()}
        };
    }

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException {
        MockitoAnnotations.initMocks(this);
        underTest = new JudicialResultsTransformer();

        final Field eventPayloadTransformerField = JudicialResultsTransformer.class.getDeclaredField("eventPayloadTransformer");
        eventPayloadTransformerField.setAccessible(true);
        eventPayloadTransformerField.set(underTest, eventPayloadTransformer);
    }

    @Test
    @UseDataProvider("validEventToTransform")
    public void shouldTransformValidEventThatHasJudicialResultsInThePayload(final String eventToTransform) {
        final JsonEnvelope event = prepareEventWithJudicialResultsToTransform(eventToTransform);

        final Action action = underTest.actionFor(event);

        assertThat(action, is(TRANSFORM));
    }

    @Test
    public void shouldNotTransformAnInvalidEventThatHasJudicialResultsInThePayload() {
        final JsonEnvelope event = prepareEventWithJudicialResultsToTransform(STRING.next());

        final Action action = underTest.actionFor(event);

        assertThat(action, is(NO_ACTION));
    }

    @Test
    @UseDataProvider("validEventToTransform")
    public void shouldNotTransformValidEventWhenJudicialResultsIsNotInThePayload(final String eventToTransform) {
        final JsonEnvelope event = prepareEventWithoutJudicialResultsToTransform(eventToTransform);

        final Action action = underTest.actionFor(event);

        assertThat(action, is(NO_ACTION));
    }

    @Test
    @UseDataProvider("validEventToTransform")
    public void shouldNotTransformValidEventWhenPayloadHasInvalidId(final String eventToTransform) {
        final JsonEnvelope event = prepareEventWithJudicialResultsAndInvalidIdToTransform(eventToTransform);

        final Action action = underTest.actionFor(event);

        assertThat(action, is(NO_ACTION));
    }

    @Test
    public void shouldTransformIncomingEventAndReturnTransformedEvent() {
        final JsonEnvelope event = prepareEventWithJudicialResultsToTransform(STRING.next());
        final JsonObject transformedPayload = createObjectBuilder().build();
        given(eventPayloadTransformer.transform(event)).willReturn(transformedPayload);

        final Stream<JsonEnvelope> stream = underTest.apply(event);

        final List<JsonEnvelope> expectedEvents = stream.collect(toList());
        assertThat(expectedEvents.size(), is(equalTo(1)));
        assertThat(expectedEvents.get(0).payload(), is(transformedPayload));
        assertThat(expectedEvents.get(0).metadata(), is(event.metadata()));
    }

    private JsonEnvelope prepareEventWithJudicialResultsToTransform(final String eventName) {
        return envelope()
                .with(metadataWithRandomUUID(eventName))
                .withPayloadOf(createArrayBuilder().build(), JUDICIAL_RESULTS_KEYWORD)
                .build();
    }

    private JsonEnvelope prepareEventWithoutJudicialResultsToTransform(final String eventName) {
        return envelope()
                .with(metadataWithRandomUUID(eventName))
                .withPayloadOf(createArrayBuilder().build(), STRING.next())
                .build();
    }

    private JsonEnvelope prepareEventWithJudicialResultsAndInvalidIdToTransform(final String eventName) {
        return envelope()
                .with(metadataWithRandomUUID(eventName))
                .withPayloadOf(createArrayBuilder()
                        .add(createObjectBuilder().add("id", "00000000-0000-0000-0000-000000000000"))
                        .build(), JUDICIAL_RESULTS_KEYWORD)
                .build();
    }
}