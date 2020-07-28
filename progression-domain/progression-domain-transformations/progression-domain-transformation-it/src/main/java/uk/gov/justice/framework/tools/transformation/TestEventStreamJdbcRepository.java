package uk.gov.justice.framework.tools.transformation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromSqlTimestamp;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.toSqlTimestamp;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventstream.EventStream;
import uk.gov.justice.services.jdbc.persistence.DataAccessException;
import uk.gov.justice.services.jdbc.persistence.JdbcRepositoryException;
import uk.gov.justice.services.jdbc.persistence.JdbcResultSetStreamer;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.sql.DataSource;

public class TestEventStreamJdbcRepository {

    private static final String SQL_INSERT_EVENT_STREAM = "INSERT INTO event_stream (stream_id, date_created, active) values (?, ?, ?) ON CONFLICT DO NOTHING";
    private static final String SQL_FIND_ALL_EVENT_STREAM = "SELECT * FROM event_stream ORDER BY position_in_stream ASC";
    private static final String SQL_FIND_ALL_ACTIVE_EVENT_STREAM = "SELECT * FROM event_stream s WHERE s.active=true ORDER BY position_in_stream ASC";

    private final DataSource dbsource;

    public TestEventStreamJdbcRepository(final DataSource dataSource) {
        this.dbsource = dataSource;
    }

    public DataSource getDatasource() {
        return dbsource;
    }

    public void insert(final UUID streamId, final boolean active) {

        try (final Connection connection = dbsource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SQL_INSERT_EVENT_STREAM)) {

            preparedStatement.setObject(1, streamId);
            preparedStatement.setTimestamp(2, toSqlTimestamp(new UtcClock().now()));
            preparedStatement.setBoolean(3, active);

            preparedStatement.executeUpdate();

        } catch (final SQLException e) {
            throw new DataAccessException(format("Failed to execute query '%s'", SQL_INSERT_EVENT_STREAM), e);
        }
    }

    public List<EventStream> findAll() {

        try {
            return new JdbcResultSetStreamer().streamOf(new PreparedStatementWrapperFactory().preparedStatementWrapperOf(getDatasource(), SQL_FIND_ALL_EVENT_STREAM),
                    entityFromFunction())
                    .collect(toList());
        } catch (SQLException e) {
            throw new JdbcRepositoryException(format("Failed to execute query '%s'", SQL_FIND_ALL_EVENT_STREAM), e);
        }
    }

    public List<EventStream> findActive() {

        try {
            return new JdbcResultSetStreamer().streamOf(new PreparedStatementWrapperFactory().preparedStatementWrapperOf(getDatasource(), SQL_FIND_ALL_ACTIVE_EVENT_STREAM),
                    entityFromFunction())
                    .collect(toList());
        } catch (SQLException e) {
            throw new JdbcRepositoryException(format("Failed to execute query '%s'", SQL_FIND_ALL_EVENT_STREAM), e);
        }
    }

    protected Function<ResultSet, EventStream> entityFromFunction() {
        return resultSet -> {
            try {
                return new EventStream((UUID) resultSet.getObject("stream_id"),
                        resultSet.getLong("position_in_stream"),
                        resultSet.getBoolean("active"),
                        fromSqlTimestamp(resultSet.getTimestamp("date_created")));
            } catch (final SQLException e) {
                throw new JdbcRepositoryException(e);
            }
        };
    }
}
