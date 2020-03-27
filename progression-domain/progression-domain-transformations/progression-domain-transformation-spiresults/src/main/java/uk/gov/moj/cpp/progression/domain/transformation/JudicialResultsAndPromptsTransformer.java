package uk.gov.moj.cpp.progression.domain.transformation;

import static java.util.stream.Stream.of;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.progression.domain.transformation.EventMapper.getEventNames;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.progression.domain.transformation.service.EventPayloadTransformer;

import java.util.stream.Stream;

import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transformation
public class JudicialResultsAndPromptsTransformer implements EventTransformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(JudicialResultsAndPromptsTransformer.class);

    private static final String JUDICIAL_RESULT_KEYWORD = "judicialResult";
    private Enveloper enveloper;
    private EventPayloadTransformer eventPayloadTransformer;

    public JudicialResultsAndPromptsTransformer() {
        eventPayloadTransformer = new EventPayloadTransformer();
    }

    @SuppressWarnings("squid:S2250")
    @Override
    public Action actionFor(final JsonEnvelope event) {
        final String eventName = event.metadata().name();
        final String payload = event.payload().toString();
        if (getEventNames().contains(eventName) && payload.contains(JUDICIAL_RESULT_KEYWORD) && ! payload.contains("00000000-0000-0000-0000-000000000000")) {
            LOGGER.debug("Found event '{}' with stream ID '{}'", eventName, event.metadata().streamId().orElse(null));
            return TRANSFORM;
        }

        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope eventEnvelope) {
        final String eventName = eventEnvelope.metadata().name();
        LOGGER.info("Processing stream with ID '{}' and event with name {}", eventEnvelope.metadata().streamId().orElse(null), eventName);

        final JsonValue transformedPayload = eventPayloadTransformer.transform(eventEnvelope);
        final JsonEnvelope transformedEnvelope = envelopeFrom(eventEnvelope.metadata(), transformedPayload);
        return of(transformedEnvelope);
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}