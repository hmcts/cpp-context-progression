package uk.gov.moj.cpp.progression.eventprocessorstore.persistence.repository;

import static java.lang.String.format;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.toSqlTimestamp;

import uk.gov.justice.services.jdbc.persistence.DefaultJdbcDataSourceProvider;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapper;
import uk.gov.moj.cpp.progression.eventprocessorstore.persistence.entity.NotificationInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;

@ApplicationScoped
public class NotificationInfoJdbcRepository {

    private final String EVENT_PROCESSOR_STORE_JNDI_NAME = "java:/app/progression-event-processor/DS.progression.eventprocessorstore";
    private final String NOTIFICATION_INFO_INSERT_QUERY = "INSERT INTO notification_info (notification_id, notification_type, payload, process_name, processed_timestamp, status) VALUES (?, ?, ?, ?, ?, ?)";
    private final String NOTIFICATION_INFO_QUERY = "SELECT * FROM notification_info WHERE notification_id = ?";
    private final String NOTIFICATION_INFO_DELETE_QUERY = "DELETE FROM notification_info notification_id = ?";

    @Inject
    private DefaultJdbcDataSourceProvider defaultJdbcDataSourceProvider;

    private DataSource dataSource;

    @PostConstruct
    protected void initialiseDataSource() {
        dataSource = defaultJdbcDataSourceProvider.getDataSource(EVENT_PROCESSOR_STORE_JNDI_NAME);
    }

    public Optional<NotificationInfoResult> findById(UUID notificationId) {
        final List<NotificationInfoResult> notificationInfos = new ArrayList();

        try (final Connection eventProcessorStoreConnection = dataSource.getConnection();
             final PreparedStatementWrapper ps = PreparedStatementWrapper.valueOf(eventProcessorStoreConnection, NOTIFICATION_INFO_QUERY)) {
            ps.setObject(1, notificationId);
            try (final ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    notificationInfos.add(entityFrom(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new NotificationInfoJdbcException(format("Exception while executing query: %s", NOTIFICATION_INFO_QUERY), e);
        }

        return notificationInfos.isEmpty() ? Optional.empty() : Optional.of(notificationInfos.get(0));
    }


    protected NotificationInfoResult entityFrom(ResultSet resultSet) throws SQLException {
        final UUID notificationId = UUID.fromString(resultSet.getString("notification_id"));
        final String notificationType = resultSet.getString("notification_type");
        final String payload = resultSet.getString("payload");
        final String processName = resultSet.getString("process_name");

        return NotificationInfoResult.Builder.builder()
                .withNotificationId(notificationId)
                .withNotificationType(notificationType)
                .withPayload(payload)
                .withProcessName(processName)
                .build();
    }

    public void save(NotificationInfo notificationInfo) {
        try (final Connection connection = dataSource.getConnection()) {
            save(notificationInfo, connection);
        } catch (final SQLException e) {
            throw new NotificationInfoJdbcException(format("Exception while inserting: %s", NOTIFICATION_INFO_INSERT_QUERY), e);
        }
    }

    public void save(final NotificationInfo notificationInfo, final Connection connection) {

        try (final PreparedStatement ps = connection.prepareStatement(NOTIFICATION_INFO_INSERT_QUERY)) {
            ps.setObject(1, notificationInfo.getNotificationId());
            ps.setObject(2, notificationInfo.getNotificationType());
            ps.setString(3, notificationInfo.getPayload());
            ps.setString(4, notificationInfo.getProcessName());
            ps.setObject(5, toSqlTimestamp(notificationInfo.getProcessedTimestamp()));
            ps.setObject(6, notificationInfo.getStatus());
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new NotificationInfoJdbcException(format("Exception while inserting: %s", NOTIFICATION_INFO_INSERT_QUERY), e);
        }
    }
}