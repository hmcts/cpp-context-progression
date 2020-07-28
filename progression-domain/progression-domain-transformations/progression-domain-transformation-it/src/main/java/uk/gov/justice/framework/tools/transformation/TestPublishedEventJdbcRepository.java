package uk.gov.justice.framework.tools.transformation;

import static java.lang.String.format;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromSqlTimestamp;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.PublishedEvent;
import uk.gov.justice.services.jdbc.persistence.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

public class TestPublishedEventJdbcRepository {

    private static final String SQL_FIND_BY_STREAM_ID = "SELECT * FROM published_event WHERE stream_id=? ORDER BY position_in_stream ASC";
    private static final String SQL_FIND_ALL = "SELECT * FROM published_event ORDER BY event_number ASC";
    private static final String SQL_COUNT_PRE_PUBLISH_QUEUE = "SELECT count(*) FROM pre_publish_queue";

    private final DataSource dbsource;

    public TestPublishedEventJdbcRepository(final DataSource dataSource) {
        this.dbsource = dataSource;
    }

    public List<PublishedEvent> findByStreamIdOrderByPositionAsc(final UUID streamId) {

        final List<PublishedEvent> events = new ArrayList<>();

        try (final Connection connection = dbsource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SQL_FIND_BY_STREAM_ID)) {

            preparedStatement.setObject(1, streamId);

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    events.add(createPublishedEventFrom(resultSet));
                }
            } catch (final SQLException e) {
                throw new DataAccessException(format("Failed to execute query '%s'", SQL_FIND_BY_STREAM_ID), e);
            }

            return events;
        } catch (final SQLException e) {
            throw new DataAccessException(format("Failed to execute query '%s'", SQL_FIND_BY_STREAM_ID), e);
        }
    }

    public List<PublishedEvent> findAllOrderByPositionAsc() {

        final List<PublishedEvent> events = new ArrayList<>();

        try (final Connection connection = dbsource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SQL_FIND_ALL);
             final ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                events.add(createPublishedEventFrom(resultSet));
            }

        } catch (final SQLException e) {
            throw new DataAccessException(format("Failed to execute query '%s'", SQL_FIND_ALL), e);
        }

        return events;
    }

    public long publishedEventsCount(final UUID streamId) {
        return findByStreamIdOrderByPositionAsc(streamId).size();
    }

    public long prePublishQueueCount() {

        try (final Connection connection = dbsource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SQL_COUNT_PRE_PUBLISH_QUEUE);
             final ResultSet resultSet = preparedStatement.executeQuery()) {

            resultSet.next();
            return resultSet.getLong(1);

        } catch (final SQLException e) {
            throw new DataAccessException(format("Failed to execute query '%s'", SQL_COUNT_PRE_PUBLISH_QUEUE), e);
        }
    }

    private PublishedEvent createPublishedEventFrom(final ResultSet resultSet) throws SQLException {
        return new PublishedEvent(
                (UUID) resultSet.getObject("id"),
                (UUID) resultSet.getObject("stream_id"),
                resultSet.getLong("position_in_stream"),
                resultSet.getString("name"),
                resultSet.getString("metadata"),
                resultSet.getString("payload"),
                fromSqlTimestamp(resultSet.getTimestamp("date_created")),
                resultSet.getLong("event_number"),
                resultSet.getLong("previous_event_number")
        );
    }
}
