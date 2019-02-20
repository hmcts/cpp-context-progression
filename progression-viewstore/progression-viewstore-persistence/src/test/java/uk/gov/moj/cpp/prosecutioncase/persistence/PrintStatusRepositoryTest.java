package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST_ACCEPTED;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST_FAILED;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST_SUCCEEDED;
import static uk.gov.moj.cpp.prosecutioncase.persistence.builder.PrintStatusBuilder.printStatus;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrintStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PrintStatusRepository;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class PrintStatusRepositoryTest {

    @Inject
    private PrintStatusRepository printStatusRepository;

    @Test
    public void shouldSaveReadAndUpdateResultOrderPrintFailedStatus() {

        final PrintStatus printRequestOrderStatus = printStatus()
                .withCaseId(randomUUID())
                .withNotificationId(randomUUID())
                .withMaterialId(randomUUID())
                .withStatus(PRINT_REQUEST)
                .withUpdated(now())
                .build();

        final PrintStatus printRequestFailedOrderStatus = printStatus()
                .withCaseId(randomUUID())
                .withNotificationId(randomUUID())
                .withMaterialId(randomUUID())
                .withErrorMessage("The end of the world is nigh")
                .withStatusCode(SC_BAD_REQUEST)
                .withStatus(PRINT_REQUEST_FAILED)
                .withUpdated(now())
                .build();

        final PrintStatus printRequestSucceededOrderStatus = printStatus()
                .withCaseId(randomUUID())
                .withNotificationId(randomUUID())
                .withStatus(PRINT_REQUEST_SUCCEEDED)
                .withUpdated(now())
                .build();

        final PrintStatus printRequestAcceptedOrderStatus = printStatus()
                .withCaseId(randomUUID())
                .withNotificationId(randomUUID())
                .withStatus(PRINT_REQUEST_ACCEPTED)
                .withUpdated(now())
                .build();

        assertThat(printStatusRepository.findByStatus(PRINT_REQUEST), hasSize(0));
        assertThat(printStatusRepository.findByStatus(PRINT_REQUEST_FAILED), hasSize(0));
        assertThat(printStatusRepository.findByStatus(PRINT_REQUEST_SUCCEEDED), hasSize(0));
        assertThat(printStatusRepository.findByStatus(PRINT_REQUEST_ACCEPTED), hasSize(0));

        printStatusRepository.save(printRequestOrderStatus);
        printStatusRepository.save(printRequestFailedOrderStatus);
        printStatusRepository.save(printRequestSucceededOrderStatus);
        printStatusRepository.save(printRequestAcceptedOrderStatus);

        assertThat(printStatusRepository.findByStatus(PRINT_REQUEST), containsInAnyOrder(printRequestOrderStatus));
        assertThat(printStatusRepository.findByStatus(PRINT_REQUEST_ACCEPTED), containsInAnyOrder(printRequestAcceptedOrderStatus));
        assertThat(printStatusRepository.findByStatus(PRINT_REQUEST_FAILED), containsInAnyOrder(printRequestFailedOrderStatus));
        assertThat(printStatusRepository.findByStatus(PRINT_REQUEST_SUCCEEDED), containsInAnyOrder(printRequestSucceededOrderStatus));

        printStatusRepository.remove(printRequestOrderStatus);
        printStatusRepository.remove(printRequestFailedOrderStatus);
        printStatusRepository.remove(printRequestSucceededOrderStatus);
        printStatusRepository.remove(printRequestAcceptedOrderStatus);
    }

    @Test
    public void shouldSaveReadAndRetrieveNowsPrintStatusByNotificationId() {
        final UUID notificationId1 = randomUUID();
        final UUID notificationId2 = randomUUID();
        final UUID notificationId3 = randomUUID();
        final UUID notificationId4 = randomUUID();


        final PrintStatus printRequestOrderStatus = printStatus()
                .withCaseId(randomUUID())
                .withNotificationId(notificationId1)
                .withMaterialId(randomUUID())
                .withStatus(PRINT_REQUEST)
                .withUpdated(now())
                .build();

        final PrintStatus resultOrderPrintStatusForFailed = printStatus()
                .withCaseId(randomUUID())
                .withNotificationId(notificationId2)
                .withMaterialId(randomUUID())
                .withErrorMessage("The end of the world is nigh")
                .withStatusCode(SC_BAD_REQUEST)
                .withStatus(PRINT_REQUEST_FAILED)
                .withUpdated(now())
                .build();

        final PrintStatus resultOrderStatusForSucceeded = printStatus()
                .withCaseId(randomUUID())
                .withNotificationId(notificationId3)
                .withStatus(PRINT_REQUEST_SUCCEEDED)
                .withUpdated(now())
                .build();

        final PrintStatus resultOrderPrintStatusForAccepted = printStatus()
                .withCaseId(randomUUID())
                .withNotificationId(notificationId4)
                .withStatus(PRINT_REQUEST_ACCEPTED)
                .withUpdated(now())
                .build();

        assertThat(printStatusRepository.findByNotificationId(notificationId1), hasSize(0));
        assertThat(printStatusRepository.findByNotificationId(notificationId2), hasSize(0));
        assertThat(printStatusRepository.findByNotificationId(notificationId3), hasSize(0));
        assertThat(printStatusRepository.findByNotificationId(notificationId4), hasSize(0));

        printStatusRepository.save(printRequestOrderStatus);
        printStatusRepository.save(resultOrderPrintStatusForAccepted);
        printStatusRepository.save(resultOrderPrintStatusForFailed);
        printStatusRepository.save(resultOrderStatusForSucceeded);

        assertThat(printStatusRepository.findByNotificationId(notificationId1), containsInAnyOrder(printRequestOrderStatus));
        assertThat(printStatusRepository.findByNotificationId(notificationId4), contains(resultOrderPrintStatusForAccepted));
        assertThat(printStatusRepository.findByNotificationId(notificationId2), containsInAnyOrder(resultOrderPrintStatusForFailed));
        assertThat(printStatusRepository.findByNotificationId(notificationId3), contains(resultOrderStatusForSucceeded));

        printStatusRepository.remove(printRequestOrderStatus);
        printStatusRepository.remove(resultOrderPrintStatusForAccepted);
        printStatusRepository.remove(resultOrderPrintStatusForFailed);
        printStatusRepository.remove(resultOrderStatusForSucceeded);
    }
}
