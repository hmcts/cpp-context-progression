package uk.gov.moj.cpp.progression.command.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.moj.cpp.progression.eventprocessorstore.persistence.repository.NotificationInfoJdbcRepository;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteNotificationInfoServiceTest {

    @Mock
    private NotificationInfoJdbcRepository repository;

    @InjectMocks
    private DeleteNotificationInfoService deleteNotificationInfoService;

    @Test
    public void shouldDeleteNotificationsOlderThanDefaultRetentionDays() {
        final String status = "PROCESSED";
        final ZonedDateTime dateTime = ZonedDateTime.now();
        setField(deleteNotificationInfoService, "notificationInfoRetentionDays", "30");

        deleteNotificationInfoService.deleteNotifications(status, dateTime);

        verify(repository, times(1)).deleteNotifications(any(String.class), any(ZonedDateTime.class));
    }

    @Test
    public void shouldDeleteNotificationsOlderThanCurrentDate() {
        final String status = "PROCESSED";
        final ZonedDateTime dateTime = ZonedDateTime.now();
        setField(deleteNotificationInfoService, "notificationInfoRetentionDays", "0");

        deleteNotificationInfoService.deleteNotifications(status, dateTime);

        verify(repository, times(1)).deleteNotifications("PROCESSED", dateTime);
    }
}
