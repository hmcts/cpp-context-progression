package uk.gov.moj.cpp.progression.domain.transformation.corechanges;

import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.progression.domain.transformation.corechanges.transform.ProgressionEventTransformer;
import uk.gov.moj.cpp.progression.domain.transformation.corechanges.transform.TransformFactory;

import java.util.List;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.slf4j.Logger;

@Transformation
public class ProgressionEventStreamTransform implements EventTransformation {

    private static final Logger LOGGER = getLogger(ProgressionEventStreamTransform.class);

    private TransformFactory transformFactory;

    public ProgressionEventStreamTransform() {
        transformFactory = new TransformFactory();
    }

    @Override
    public Action actionFor(final JsonEnvelope event) {
        final List<ProgressionEventTransformer> eventTransformer = transformFactory.getEventTransformer(event.metadata().name());
        if (eventTransformer != null && !eventTransformer.isEmpty()) {
            return TRANSFORM;
        }
        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {

        JsonObject payload = event.payloadAsJsonObject();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("----------------------event name------------ {}", event.metadata().name());
        }

        final String eventName = event.metadata().name();
        for (final ProgressionEventTransformer progressionEventTransformer : transformFactory.getEventTransformer(eventName)) {
            payload = progressionEventTransformer.transform(event.metadata(), payload);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("-------------------transformedPayload---------------{}", payload);
        }

        return of(envelopeFrom(metadataFrom(event.metadata()), payload));

    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        // no need
    }

    void setTransformFactory(final TransformFactory transformFactory) {
        this.transformFactory = transformFactory;
    }
}
