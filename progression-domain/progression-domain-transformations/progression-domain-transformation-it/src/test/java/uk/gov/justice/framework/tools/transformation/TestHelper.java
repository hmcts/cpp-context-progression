package uk.gov.justice.framework.tools.transformation;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.Event;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventstream.EventStream;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import liquibase.exception.LiquibaseException;

public class TestHelper {

    private UUID STREAM_ID = UUID.randomUUID();
    private DatabaseUtils databaseUtils;


    public TestHelper() {
        try {
            databaseUtils = new DatabaseUtils();
            databaseUtils.dropAndUpdateLiquibase();
        } catch (final SQLException e) {
            e.printStackTrace();
        } catch (final LiquibaseException e) {
            e.printStackTrace();
        }
    }

    public boolean clonedStreamAvailableAndActive() {
        final Optional<Event> matchingClonedEvent = databaseUtils.getEventStoreDataAccess().findAllEvents().stream()
                .filter(event -> event.getName().equals("system.events.cloned"))
                .findFirst();
        return matchingClonedEvent.isPresent()
                && streamAvailableAndActive(matchingClonedEvent.get().getStreamId());
    }

    public boolean originalEventStreamIsActive() {
        return streamAvailableAndActive(STREAM_ID);
    }


    public boolean originalEventStreamIsActive(final UUID streamId) {
        return streamAvailableAndActive(streamId);
    }

    public boolean streamAvailableAndActive(final UUID streamId) {
        final Optional<EventStream> matchingEvent = databaseUtils.getEventStreamJdbcRepository().findAll().stream()
                .filter(eventStream -> eventStream.getStreamId().equals(streamId))
                .findFirst();
        return matchingEvent.isPresent() && matchingEvent.get().isActive();
    }

    public boolean eventStoreTransformedEventPresent(final String transformedEventName) {
        final Stream<Event> eventLogs = databaseUtils.getEventStoreDataAccess().findAllEvents().stream();
        final Optional<Event> event = eventLogs.filter(item -> item.getStreamId().equals(STREAM_ID)).findFirst();

        return event.isPresent() && event.get().getName().equals(transformedEventName);
    }

    public boolean eventStoreTransformedEventPresentAndSequenceCorrect(final String transformedEventName, final UUID streamId, final long sequence) {
        final Stream<Event> eventLogs = databaseUtils.getEventStoreDataAccess().findAllEvents().stream();
        final Optional<Event> event = eventLogs
                .filter(item -> item.getStreamId().equals(streamId))
                .filter(item -> item.getName().equals(transformedEventName))
                .findFirst();

        return event.isPresent() && event.get().getPositionInStream().equals(sequence);
    }

    public boolean eventStoreTransformedEventPresent(final String transformedEventName, final UUID streamId) {
        final Stream<Event> eventLogs = databaseUtils.getEventStoreDataAccess().findAllEvents().stream();
        final Optional<Event> event = eventLogs
                .filter(item -> item.getStreamId().equals(streamId))
                .filter(item -> item.getName().equals(transformedEventName))
                .findFirst();

        return event.isPresent();
    }

    public List<Event> retrieveEventsFor(final String eventName) {
        final Stream<Event> eventLogs = databaseUtils.getEventStoreDataAccess().findAllEvents().stream();
        return eventLogs.filter(item -> item.getName().equals(eventName)).collect(toList());
    }

    public boolean eventStoreEventIsPresent(final String originalEventName) {
        final Stream<Event> eventLogs = databaseUtils.getEventStoreDataAccess().findAllEvents().stream();
        final Optional<Event> event = eventLogs
                .filter(item -> item.getName().equals(originalEventName))
                .filter(item -> item.getStreamId().equals(STREAM_ID))
                .findFirst();

        return event.isPresent();
    }

    public long totalStreamCount() {
        return databaseUtils.getEventStreamJdbcRepository().findAll().size();
    }

    public long totalEventCount(final String eventName) {
        final Stream<Event> eventLogs = databaseUtils.getEventStoreDataAccess().findAllEvents().stream();
        return eventLogs.filter(item -> item.getName().equals(eventName)).collect(toList()).size();
    }

    public long totalEventCount() {
        return databaseUtils.getEventStoreDataAccess().findAllEvents().size();
    }

    public List<Event> getTransformedEvents(final UUID streamId) {
        final Stream<Event> eventLogs = databaseUtils.getEventStoreDataAccess().findAllEvents().stream();
        return eventLogs.filter(item -> item.getStreamId().equals(streamId)).collect(toList());
    }

    public DatabaseUtils getDatabaseUtils() {
        return databaseUtils;
    }
}
