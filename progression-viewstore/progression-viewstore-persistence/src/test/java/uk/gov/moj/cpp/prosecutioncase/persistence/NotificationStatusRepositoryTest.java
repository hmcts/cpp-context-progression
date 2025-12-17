package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST_ACCEPTED;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST_FAILED;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST_SUCCEEDED;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationType.PRINT;
import static uk.gov.moj.cpp.prosecutioncase.persistence.builder.NotificationStatusBuilder.notificationStatusBuilder;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentIndexRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NotificationStatusRepository;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;

@RunWith(CdiTestRunner.class)
public class NotificationStatusRepositoryTest {

    @Inject
    private NotificationStatusRepository notificationStatusRepository;

    @Inject
    private CourtDocumentIndexRepository courtDocumentIndexRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Test
    public void shouldSaveReadAndUpdateResultOrderPrintFailedStatus() {

        final NotificationStatusEntity notificationRequestOrderStatus = notificationStatusBuilder()
                .withCaseId(randomUUID())
                .withNotificationId(randomUUID())
                .withMaterialId(randomUUID())
                .withNotificationStatus(NOTIFICATION_REQUEST)
                .withNotificationType(PRINT)
                .withUpdated(now())
                .build();

        final NotificationStatusEntity notificationRequestFailedOrderStatus = notificationStatusBuilder()
                .withCaseId(randomUUID())
                .withNotificationId(randomUUID())
                .withMaterialId(randomUUID())
                .withErrorMessage("The end of the world is nigh")
                .withStatusCode(SC_BAD_REQUEST)
                .withNotificationStatus(NOTIFICATION_REQUEST_FAILED)
                .withNotificationType(PRINT)
                .withUpdated(now())
                .build();

        final NotificationStatusEntity printRequestSucceededOrderStatus = notificationStatusBuilder()
                .withCaseId(randomUUID())
                .withNotificationId(randomUUID())
                .withNotificationStatus(NOTIFICATION_REQUEST_SUCCEEDED)
                .withNotificationType(PRINT)
                .withUpdated(now())
                .build();

        final NotificationStatusEntity printRequestAcceptedOrderStatus = notificationStatusBuilder()
                .withCaseId(randomUUID())
                .withNotificationId(randomUUID())
                .withNotificationStatus(NOTIFICATION_REQUEST_ACCEPTED)
                .withNotificationType(PRINT)
                .withUpdated(now())
                .build();

        assertThat(notificationStatusRepository.findByNotificationStatus(NOTIFICATION_REQUEST), hasSize(0));
        assertThat(notificationStatusRepository.findByNotificationStatus(NOTIFICATION_REQUEST_FAILED), hasSize(0));
        assertThat(notificationStatusRepository.findByNotificationStatus(NOTIFICATION_REQUEST_SUCCEEDED), hasSize(0));
        assertThat(notificationStatusRepository.findByNotificationStatus(NOTIFICATION_REQUEST_ACCEPTED), hasSize(0));

        notificationStatusRepository.save(notificationRequestOrderStatus);
        notificationStatusRepository.save(notificationRequestFailedOrderStatus);
        notificationStatusRepository.save(printRequestSucceededOrderStatus);
        notificationStatusRepository.save(printRequestAcceptedOrderStatus);

        assertThat(notificationStatusRepository.findByNotificationStatus(NOTIFICATION_REQUEST), containsInAnyOrder(notificationRequestOrderStatus));
        assertThat(notificationStatusRepository.findByNotificationStatus(NOTIFICATION_REQUEST_ACCEPTED), containsInAnyOrder(printRequestAcceptedOrderStatus));
        assertThat(notificationStatusRepository.findByNotificationStatus(NOTIFICATION_REQUEST_FAILED), containsInAnyOrder(notificationRequestFailedOrderStatus));
        assertThat(notificationStatusRepository.findByNotificationStatus(NOTIFICATION_REQUEST_SUCCEEDED), containsInAnyOrder(printRequestSucceededOrderStatus));

        notificationStatusRepository.remove(notificationRequestOrderStatus);
        notificationStatusRepository.remove(notificationRequestFailedOrderStatus);
        notificationStatusRepository.remove(printRequestSucceededOrderStatus);
        notificationStatusRepository.remove(printRequestAcceptedOrderStatus);
    }

    @Test
    public void shouldSaveReadAndRetrieveNowsNotificationStatusByNotificationId() {
        final UUID notificationId1 = randomUUID();
        final UUID notificationId2 = randomUUID();
        final UUID notificationId3 = randomUUID();
        final UUID notificationId4 = randomUUID();


        final NotificationStatusEntity notificationRequestOrderStatus = notificationStatusBuilder()
                .withCaseId(randomUUID())
                .withNotificationId(notificationId1)
                .withMaterialId(randomUUID())
                .withNotificationStatus(NOTIFICATION_REQUEST)
                .withNotificationType(PRINT)
                .withUpdated(now())
                .build();

        final NotificationStatusEntity resultOrderNotificationStatusForFailed = notificationStatusBuilder()
                .withCaseId(randomUUID())
                .withNotificationId(notificationId2)
                .withMaterialId(randomUUID())
                .withErrorMessage("The end of the world is nigh")
                .withStatusCode(SC_BAD_REQUEST)
                .withNotificationStatus(NOTIFICATION_REQUEST_FAILED)
                .withNotificationType(PRINT)
                .withUpdated(now())
                .build();

        final NotificationStatusEntity resultOrderStatusForSucceeded = notificationStatusBuilder()
                .withCaseId(randomUUID())
                .withNotificationId(notificationId3)
                .withNotificationStatus(NOTIFICATION_REQUEST_SUCCEEDED)
                .withNotificationType(PRINT)
                .withUpdated(now())
                .build();

        final NotificationStatusEntity resultOrderNotificationStatusForAccepted = notificationStatusBuilder()
                .withCaseId(randomUUID())
                .withNotificationId(notificationId4)
                .withNotificationStatus(NOTIFICATION_REQUEST_ACCEPTED)
                .withNotificationType(PRINT)
                .withUpdated(now())
                .build();

        assertThat(notificationStatusRepository.findByNotificationId(notificationId1), hasSize(0));
        assertThat(notificationStatusRepository.findByNotificationId(notificationId2), hasSize(0));
        assertThat(notificationStatusRepository.findByNotificationId(notificationId3), hasSize(0));
        assertThat(notificationStatusRepository.findByNotificationId(notificationId4), hasSize(0));

        notificationStatusRepository.save(notificationRequestOrderStatus);
        notificationStatusRepository.save(resultOrderNotificationStatusForAccepted);
        notificationStatusRepository.save(resultOrderNotificationStatusForFailed);
        notificationStatusRepository.save(resultOrderStatusForSucceeded);

        assertThat(notificationStatusRepository.findByNotificationId(notificationId1), containsInAnyOrder(notificationRequestOrderStatus));
        assertThat(notificationStatusRepository.findByNotificationId(notificationId4), contains(resultOrderNotificationStatusForAccepted));
        assertThat(notificationStatusRepository.findByNotificationId(notificationId2), containsInAnyOrder(resultOrderNotificationStatusForFailed));
        assertThat(notificationStatusRepository.findByNotificationId(notificationId3), contains(resultOrderStatusForSucceeded));

        notificationStatusRepository.remove(notificationRequestOrderStatus);
        notificationStatusRepository.remove(resultOrderNotificationStatusForAccepted);
        notificationStatusRepository.remove(resultOrderNotificationStatusForFailed);
        notificationStatusRepository.remove(resultOrderStatusForSucceeded);
    }

    @Test
    public void shouldGetNotificationStatus() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID notificationId = randomUUID();

        final NotificationStatusEntity notificationRequestOrderStatus = notificationStatusBuilder()
                .withCaseId(prosecutionCaseId)
                .withNotificationId(notificationId)
                .withMaterialId(randomUUID())
                .withNotificationStatus(NOTIFICATION_REQUEST)
                .withNotificationType(PRINT)
                .withUpdated(now())
                .build();

        notificationStatusRepository.save(notificationRequestOrderStatus);
        assertThat(notificationStatusRepository.findByCaseId(prosecutionCaseId), hasSize(1));
        notificationStatusRepository.remove(notificationRequestOrderStatus);
    }
}
