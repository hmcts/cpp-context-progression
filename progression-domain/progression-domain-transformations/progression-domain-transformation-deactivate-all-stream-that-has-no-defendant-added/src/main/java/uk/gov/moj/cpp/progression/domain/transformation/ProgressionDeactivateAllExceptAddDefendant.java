package uk.gov.moj.cpp.progression.domain.transformation;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;


@Transformation
public class ProgressionDeactivateAllExceptAddDefendant implements EventTransformation {

    private static final List<String> EVENTS_TO_KEEP = newArrayList(
            "progression.events.defendant-added",
            "progression.event.cases-referred-to-court",
            "progression.event.hearing-defendant-request-created",
            "progression.event.prosecution-case-created",
            "progression.event.prosecutionCase-defendant-listing-status-changed"
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
        }
        return DEACTIVATE;
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}