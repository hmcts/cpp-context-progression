package uk.gov.moj.cpp.progression.domain.transformation;


import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.List;
import java.util.stream.Stream;

@Transformation
public class ProgressionEventStreamArchiver implements EventTransformation {

    private static final List<String> EVENTS_TO_ARCHIVE = newArrayList(
            "progression.events.new-case-document-received",
            "progression.events.case-already-exists-in-crown-court",
            "progression.events.sending-sheet-invalidated",
            "progression.events.sending-sheet-previously-completed",
            "progression.events.defendant-addition-failed",
            "progression.events.defendant-not-found",
            "progression.events.defendant-offences-does-not-have-required-modeoftrial",
            "progression.events.plea-update-failed",
            "progression.events.pre-sentence-report-for-defendants-requested",
            "progression.events.case-added-to-crown-court",
            "progression.events.case-pending-for-sentence-hearing",
            "progression.events.case-ready-for-sentence-hearing",
            "progression.events.case-to-be-assigned-updated",
            "progression.events.sending-committal-hearing-information-added",
            "progression.events.sentence-hearing-date-added",
            "progression.events.sending-sheet-completed",
            "progression.events.bail-status-updated-for-defendant",
            "progression.events.defence-solicitor-firm-for-defendant-updated",
            "progression.events.defendant-added",
            "progression.events.defendant-additional-information-added",
            "progression.events.interpreter-for-defendant-updated",
            "progression.events.no-more-information-required",
            "progression.events.offences-for-defendant-updated"
    );

    private static final String EARLIER_ARCHIVED_EVENT_NAME_ENDS_WITH = ".archived.1.9.release";

    private Enveloper enveloper;

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {
        final String restoredEventName = event.metadata().name().replace(EARLIER_ARCHIVED_EVENT_NAME_ENDS_WITH, "");

        final JsonEnvelope transformedEnvelope = enveloper
                .withMetadataFrom(event, restoredEventName)
                .apply(event.payload());
        return Stream.of(transformedEnvelope);
    }

    @Override
    public Action actionFor(final JsonEnvelope event) {
        if (EVENTS_TO_ARCHIVE.stream()
                .anyMatch(eventToArchive -> event.metadata().name().equalsIgnoreCase(eventToArchive))) {
            return DEACTIVATE;
        } else if (event.metadata().name().toLowerCase().endsWith(EARLIER_ARCHIVED_EVENT_NAME_ENDS_WITH)) {
            return new Action(true, true, false);
        }

        return NO_ACTION;
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}