package uk.gov.moj.cpp.progression.command.service;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.progression.eventprocessorstore.persistence.repository.NotificationInfoJdbcRepository;

import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.transaction.Transactional;

@Transactional
public class DeleteNotificationInfoService {

    @Inject
    @Value(key = "notificationInfoRetentionDays", defaultValue = "30")
    private String notificationInfoRetentionDays;

    @Inject
    private NotificationInfoJdbcRepository repository;

    public void deleteNotifications(final String status, final ZonedDateTime dateTime) {
        final int retentionDays = Integer.parseInt(notificationInfoRetentionDays);
        final ZonedDateTime cutoffDate = dateTime.minusDays(retentionDays);
        repository.deleteNotifications(status, cutoffDate);
    }

}
