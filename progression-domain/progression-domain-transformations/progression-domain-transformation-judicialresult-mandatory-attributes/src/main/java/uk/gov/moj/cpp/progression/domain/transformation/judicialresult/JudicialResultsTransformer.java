package uk.gov.moj.cpp.progression.domain.transformation.judicialresult;

import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.isEventToTransform;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.progression.domain.transformation.judicialresult.service.EventPayloadTransformer;

import java.util.stream.Stream;

import javax.json.JsonValue;

import org.slf4j.Logger;

@Transformation
public class JudicialResultsTransformer implements EventTransformation {

    private static final Logger LOGGER = getLogger(JudicialResultsTransformer.class);

    private static final String JUDICIAL_RESULTS_KEYWORD = "judicialResults";
    private final EventPayloadTransformer eventPayloadTransformer;

    public JudicialResultsTransformer() {
        eventPayloadTransformer = new EventPayloadTransformer();
    }

    @Override
    public Action actionFor(final JsonEnvelope event) {
        final String eventName = event.metadata().name();
        final String payload = event.payload().toString();
        if (isEventToTransform(eventName) && payload.contains(JUDICIAL_RESULTS_KEYWORD) && !payload.contains("00000000-0000-0000-0000-000000000000")) {
            LOGGER.debug("Found event '{}' with stream ID '{}'", eventName, event.metadata().streamId().orElse(null));
            return TRANSFORM;
        }

        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {
        final String eventName = event.metadata().name();
        LOGGER.info("Processing stream with ID '{}' and event with name '{}'", event.metadata().streamId().orElse(null), eventName);

        final JsonValue transformedPayload = eventPayloadTransformer.transform(event);
        final JsonEnvelope transformedEnvelope = envelopeFrom(event.metadata(), transformedPayload);
        return of(transformedEnvelope);
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        // not used
    }
}
