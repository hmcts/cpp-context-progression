package uk.gov.moj.cpp.progression.handler;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.command.handler.AddApplicationNote;
import uk.gov.moj.cpp.progression.command.handler.EditApplicationNote;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserDetails;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ApplicationNoteHandler extends AbstractCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationNoteHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private UsersGroupService usersGroupService;

    @Handles("progression.command.handler.add-application-note")
    public void handleAddApplicationNote(final Envelope<AddApplicationNote> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.handler.add-application-note {}", envelope);

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException("No UserId Supplied"));

        final UserDetails userDetails = usersGroupService.getUserDetails(envelope)
                .orElseThrow(() -> new IllegalStateException(format("Cannot find the user details with userId %s.", userId)));

        final AddApplicationNote note = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(note.getApplicationId());
        final ApplicationAggregate aggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = aggregate.addNote(randomUUID(), note.getApplicationId(),
                note.getNote(), nonNull(note.getIsPinned()) ? note.getIsPinned() : Boolean.FALSE,
                userDetails.getFirstName(), userDetails.getLastName());

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.handler.edit-application-note")
    public void handleEditApplicationNote(final Envelope<EditApplicationNote> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.handler.edit-application-note {}", envelope);

        final EditApplicationNote note = envelope.payload();

        final EventStream eventStream = eventSource.getStreamById(note.getApplicationId());
        final ApplicationAggregate aggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = aggregate.editNote(note.getApplicationNoteId(), note.getApplicationId(), note.getIsPinned());

        appendEventsToStream(envelope, eventStream, events);
    }
}

