package uk.gov.moj.cpp.progression.domain.transformation;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.*;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Transformation
public class ProgressionSeparateDefendantAdded implements EventTransformation {

    private static final List<String> EVENTS_TO_KEEP = newArrayList(
            "progression.events.defendant-added"
    );

    private Enveloper enveloper;

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {
        return Stream.of(event);
    }

    @Override
    public Action actionFor(final JsonEnvelope event) {
        if (EVENTS_TO_KEEP.stream()
                .anyMatch(eventToArchive -> event.metadata().name().equalsIgnoreCase(eventToArchive))) {
            return NO_ACTION;
        } else {
            return new Action(true, false, false);
        }
    }

    @Override
    public Optional<UUID> setStreamId(final JsonEnvelope event) {
        return Optional.of(UUID.randomUUID());
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}