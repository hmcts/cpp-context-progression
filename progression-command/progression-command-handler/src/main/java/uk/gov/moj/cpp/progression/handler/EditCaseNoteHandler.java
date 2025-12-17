package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.EditCaseNote;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class EditCaseNoteHandler extends AbstractCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditCaseNoteHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.edit-case-note")
    public void handle(final Envelope<EditCaseNote> editCaseNoteEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.edit-case-note {}", editCaseNoteEnvelope);
        final EditCaseNote editCaseNote = editCaseNoteEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(editCaseNote.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.editNote(editCaseNote.getCaseId(), editCaseNote.getCaseNoteId(), editCaseNote.getIsPinned());
        appendEventsToStream(editCaseNoteEnvelope, eventStream, events);
    }

}

