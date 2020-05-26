package uk.gov.moj.cpp.progression.domain.transformation.pilotdata;

import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

@Transformation
public class EventStreamTransform implements EventTransformation {

    private static final Logger LOGGER = getLogger(EventStreamTransform.class);

    private final Set<UUID> uuidList = new HashSet<>();

    public EventStreamTransform() {
        uuidList.addAll(Stream.of("caseId.csv", "hearingId.csv")
                .flatMap(csvFilename -> CsvFileHelper.getCsvRecords(csvFilename).stream())
                .collect(Collectors.toList()));
    }

    public EventStreamTransform(final String... csvFilenames) {
        uuidList.addAll(Stream.of(csvFilenames)
                .flatMap(csvFilename -> CsvFileHelper.getCsvRecords(csvFilename).stream())
                .collect(Collectors.toList()));
    }

    @Override
    public Action actionFor(final JsonEnvelope event) {
        final Optional<UUID> streamId = event.metadata().streamId();
        if (streamId.isPresent() && uuidList.contains(streamId.get())) {
            LOGGER.info("{} deactivated... ", streamId);
            return DEACTIVATE;
        }
        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {

        return of(event);

    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        // no need
    }

}
