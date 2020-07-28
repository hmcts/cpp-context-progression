package uk.gov.justice.framework.tools.transformation;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.justice.framework.tools.transformation.EventLogBuilder.eventLogFrom;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.Event;
import uk.gov.justice.services.eventsourcing.repository.jdbc.exception.InvalidPositionException;
import uk.gov.justice.services.test.utils.events.EventStoreDataAccess;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import liquibase.exception.LiquibaseException;

public class DatabaseUtils {

    private final EventStoreDataAccess eventStoreDataAccess;
    private final TestEventStreamJdbcRepository eventStreamJdbcRepository;
    private final LiquibaseUtil liquibaseUtil = new LiquibaseUtil();
    private final DataSource dataSource;

    public DatabaseUtils() throws SQLException, LiquibaseException {
        dataSource = liquibaseUtil.initEventStoreDb();
        eventStoreDataAccess = new EventStoreDataAccess(dataSource);
        eventStreamJdbcRepository = new TestEventStreamJdbcRepository(dataSource);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void dropAndUpdateLiquibase() throws SQLException, LiquibaseException {
        liquibaseUtil.dropAndUpdate();
    }

    public void insertEventLogData(final String eventName, final UUID streamId, final long sequenceId, final ZonedDateTime createdAt) throws InvalidPositionException {
        insertEventLogData(eventName, streamId, sequenceId, createdAt, empty());

    }

    public void insertEventLogData(final String eventName, final UUID streamId, final long sequenceId, final ZonedDateTime createdAt, final long eventNumber) throws InvalidPositionException {
        insertEventLogData(eventName, streamId, sequenceId, createdAt, of(eventNumber));
    }

    public void insertEventLogData(final String eventName, final UUID streamId, final long sequenceId, final ZonedDateTime createdAt, final Optional<Long> eventNumber) throws InvalidPositionException {
        insertEventLogData(eventName, streamId, sequenceId, createdAt, eventNumber, true);
    }

    public void insertEventLogData(final String eventName, final UUID streamId, final long sequenceId, final ZonedDateTime createdAt, boolean streamStatus) throws InvalidPositionException {
        insertEventLogData(eventName, streamId, sequenceId, createdAt, empty(), streamStatus);
    }

    public void insertEventLogData(final String eventName, final UUID streamId, final long sequenceId, final ZonedDateTime createdAt, final Optional<Long> eventNumber, boolean streamStatus) throws InvalidPositionException {
        final Event event = eventLogFrom(eventName, sequenceId, streamId, createdAt, eventNumber);

        eventStoreDataAccess.insertIntoEventLog(event);
        eventStreamJdbcRepository.insert(streamId, streamStatus);
    }


    public void insertEventLogData(final String eventName, final UUID streamId, final long sequenceId, final ZonedDateTime createdAt, final Optional<Long> eventNumber, boolean streamStatus,
                                   final String payload) throws InvalidPositionException {

        final Event event = eventLogFrom(eventName, sequenceId, streamId, createdAt, eventNumber,payload);

        eventStoreDataAccess.insertIntoEventLog(event);
        eventStreamJdbcRepository.insert(streamId, streamStatus);
    }

    public EventStoreDataAccess getEventStoreDataAccess() {
        return eventStoreDataAccess;
    }

    public TestEventStreamJdbcRepository getEventStreamJdbcRepository() {
        return eventStreamJdbcRepository;
    }
}
