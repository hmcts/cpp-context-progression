package uk.gov.moj.cpp.progression.handler;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.AddCaseNote;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserDetails;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AddCaseNoteHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddCaseNoteHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private UsersGroupService usersGroupService;

    @Handles("progression.command.add-case-note")
    public void handle(final Envelope<AddCaseNote> addCaseNoteEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.add-case-note {}", addCaseNoteEnvelope);

        final String userId = addCaseNoteEnvelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException("No UserId Supplied"));

        final UserDetails userDetails = usersGroupService.getUserDetails(addCaseNoteEnvelope)
                .orElseThrow(() -> new IllegalStateException(format("Cannot find the user details with userId %s.", userId)));

        final AddCaseNote addCaseNote = addCaseNoteEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(addCaseNote.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        boolean isPinned = false;
        if (addCaseNote.getIsPinned() != null) {
            isPinned = addCaseNote.getIsPinned();
        }
        final Stream<Object> events = caseAggregate.addNote(randomUUID(), addCaseNote.getCaseId(), addCaseNote.getNote(), isPinned, userDetails.getFirstName(), userDetails.getLastName());

        appendEventsToStream(addCaseNoteEnvelope, eventStream, events);
    }


    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        final Stream<JsonEnvelope> envelopeStream = events.map(toEnvelopeWithMetadataFrom(jsonEnvelope));
        eventStream.append(envelopeStream);
    }

}

