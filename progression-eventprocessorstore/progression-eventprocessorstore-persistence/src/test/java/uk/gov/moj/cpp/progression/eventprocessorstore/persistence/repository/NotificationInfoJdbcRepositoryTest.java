package uk.gov.moj.cpp.progression.eventprocessorstore.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.jdbc.persistence.DefaultJdbcDataSourceProvider;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapper;
import uk.gov.moj.cpp.progression.eventprocessorstore.persistence.entity.NotificationInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class NotificationInfoJdbcRepositoryTest {

    @Mock
    private DefaultJdbcDataSourceProvider defaultJdbcDataSourceProvider;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatementWrapper preparedStatementWrapper;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private NotificationInfoJdbcRepository repository;

    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        when(defaultJdbcDataSourceProvider.getDataSource(anyString())).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    @Test
    public void shouldFindById() throws SQLException {
        UUID notificationId = UUID.randomUUID();
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(preparedStatementWrapper.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("notification_id")).thenReturn(notificationId.toString());
        when(resultSet.getString("notification_type")).thenReturn("type");
        when(resultSet.getString("payload")).thenReturn("payload");
        when(resultSet.getString("process_name")).thenReturn("process");

        Optional<NotificationInfoResult> result = repository.findById(notificationId);

        assertTrue(result.isPresent());
        assertEquals(notificationId, result.get().getNotificationId());
        assertEquals("type", result.get().getNotificationType());
        assertEquals("payload", result.get().getPayload());
        assertEquals("process", result.get().getProcessName());
    }

    @Test
    public void shouldSave() throws SQLException {
        NotificationInfo notificationInfo = new NotificationInfo();
        notificationInfo.setNotificationId(UUID.randomUUID());
        notificationInfo.setNotificationType("type");
        notificationInfo.setPayload("payload");
        notificationInfo.setProcessName("process");
        notificationInfo.setProcessedTimestamp(ZonedDateTime.now());
        notificationInfo.setStatus("status");

        when(connection.prepareStatement(anyString())).thenReturn(mock(PreparedStatement.class));

        repository.save(notificationInfo);

        verify(connection, Mockito.times(1)).prepareStatement(anyString());
    }
}