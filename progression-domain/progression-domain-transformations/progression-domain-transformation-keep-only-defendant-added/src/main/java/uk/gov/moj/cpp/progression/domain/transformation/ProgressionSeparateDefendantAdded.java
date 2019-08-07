package uk.gov.moj.cpp.progression.domain.transformation;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Transformation
public class ProgressionSeparateDefendantAdded implements EventTransformation {

    private static final List<String> EVENTS_TO_KEEP = newArrayList(
            "progression.events.defendant-added"
    );

    private static final List<String> NEW_EVENTS_AFTER_19_1 = newArrayList(
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

        final ZonedDateTime lastDays = ZonedDateTime.of(LocalDate.of(2019, Month.MARCH, 11), LocalTime.of(0, 0), ZoneId.of("UTC"));

        final Optional<ZonedDateTime> createdAt = event.metadata().createdAt();

        if ((EVENTS_TO_KEEP.stream().anyMatch(eventToArchive -> event.metadata().name().equalsIgnoreCase(eventToArchive))) ||
                ((NEW_EVENTS_AFTER_19_1.stream().anyMatch(eventToArchive -> event.metadata().name().equalsIgnoreCase(eventToArchive)))
                         && createdAt.isPresent() && (ZonedDateTime.parse(createdAt.get().toString()).isAfter(lastDays)))) {
            return NO_ACTION;
        }

        return new Action(true, false, false);

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