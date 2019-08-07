package uk.gov.moj.cpp.prosecution.event.listener;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST_ACCEPTED;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST_FAILED;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST_SUCCEEDED;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.NotificationStatus;
import uk.gov.moj.cpp.prosecutioncase.event.listener.NotificationListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NotificationStatusRepository;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NotificationListenerTest {

    private UUID caseId;
    private UUID materialId;
    private ZonedDateTime now;
    private NotificationStatusEntity notificationStatus;

    @Captor
    private ArgumentCaptor<NotificationStatusEntity> notificationStatusCaptor;

    @Mock
    private NotificationStatusRepository notificationStatusRepository;

    @InjectMocks
    private NotificationListener notificationListener;

    @Before
    public void init() {
        caseId = randomUUID();
        materialId = randomUUID();
        now = ZonedDateTime.now();
        notificationStatus = new NotificationStatusEntity();
        notificationStatus.setMaterialId(materialId);
    }

    @Test
    public void shouldCreateNewStatusWithPrintRequestedStatus() {
        final UUID notificationId = randomUUID();

        final JsonEnvelope printRequestedEvent = envelopeFrom(
                metadataWithRandomUUID("progression.event.notification-requested").createdAt(now),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("materialId", materialId.toString())
                        .add("notificationType", "PRINT")
                        .build());

        when(notificationStatusRepository.findBy(caseId)).thenReturn(null);

        notificationListener.printRequested(printRequestedEvent);

        verify(notificationStatusRepository).save(notificationStatusCaptor.capture());

        final NotificationStatusEntity actualStatus = notificationStatusCaptor.getValue();
        assertThat(actualStatus.getNotificationId(), equalTo(notificationId));
        assertThat(actualStatus.getMaterialId(), equalTo(materialId));
        assertThat(actualStatus.getNotificationStatus(), equalTo(NOTIFICATION_REQUEST));
        assertThat(actualStatus.getUpdated().toInstant(), equalTo(now.toInstant()));
    }

    @Test
    public void shouldCreateNewStatusWithPrintRequestAcceptedStatus() {
        final UUID notificationId = randomUUID();
        final ZonedDateTime acceptedTime = now;
        final JsonEnvelope printRequestAcceptedEvent = envelopeFrom(
                metadataWithRandomUUID("progression.event.notification-request-accepted").createdAt(now),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("materialId", materialId.toString())
                        .add("acceptedTime", ZonedDateTimes.toString(acceptedTime))
                        .build());

        notificationStatus.setNotificationStatus(NOTIFICATION_REQUEST_ACCEPTED);
        when(notificationStatusRepository.findByNotificationId(notificationId)).thenReturn(Arrays
                .asList(printStatus(notificationId, materialId, caseId, NOTIFICATION_REQUEST)));
        notificationListener.notificationRequestAccepted(printRequestAcceptedEvent);

        verify(notificationStatusRepository).save(notificationStatusCaptor.capture());

        final NotificationStatusEntity actualStatus = notificationStatusCaptor.getValue();
        assertThat(actualStatus.getCaseId(), equalTo(caseId));
        assertThat(actualStatus.getNotificationId(), equalTo(notificationId));
        assertThat(actualStatus.getNotificationStatus(), equalTo(NOTIFICATION_REQUEST_ACCEPTED));
        assertThat(actualStatus.getUpdated().toInstant(), equalTo(now.toInstant()));
    }

    @Test
    public void shouldCreateNewStatusWithPrintRequestFailedStatus() {
        final UUID notificationId = randomUUID();
        final ZonedDateTime failedTime = now;
        final String errorMessage = "error message";
        final int statusCode = SC_NOT_FOUND;

        final JsonEnvelope printRequestedEvent = envelopeFrom(
                metadataWithRandomUUID("progression.event.notification-request-failed").createdAt(now),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("errorMessage", errorMessage)
                        .add("statusCode", statusCode)
                        .add("failedTime", ZonedDateTimes.toString(failedTime))
                        .build());

        when(notificationStatusRepository.findByNotificationId(notificationId)).thenReturn(Arrays
                .asList(printStatus(notificationId, materialId, caseId, NOTIFICATION_REQUEST),
                        printStatus(notificationId, materialId, caseId, NOTIFICATION_REQUEST_ACCEPTED)));
        notificationListener.printRequestFailed(printRequestedEvent);

        verify(notificationStatusRepository).save(notificationStatusCaptor.capture());

        final NotificationStatusEntity actualStatus = notificationStatusCaptor.getValue();
        assertThat(actualStatus.getCaseId(), equalTo(caseId));
        assertThat(actualStatus.getNotificationId(), equalTo(notificationId));
        assertThat(actualStatus.getErrorMessage(), equalTo(errorMessage));
        assertThat(actualStatus.getStatusCode(), equalTo(statusCode));
        assertThat(actualStatus.getNotificationStatus(), equalTo(NOTIFICATION_REQUEST_FAILED));
        assertThat(actualStatus.getUpdated().toInstant(), equalTo(failedTime.toInstant()));
    }

    @Test
    public void shouldCreateNewStatusWithPrintRequestSucceededStatus() {
        final UUID notificationId = randomUUID();
        final ZonedDateTime sentTime = now;

        final JsonEnvelope printRequestedEvent = envelopeFrom(
                metadataWithRandomUUID("progression.event.notification-request-succeeded").createdAt(now),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("sentTime", ZonedDateTimes.toString(sentTime))
                        .build());

        when(notificationStatusRepository.findByNotificationId(notificationId)).thenReturn(Arrays
                .asList(printStatus(notificationId, materialId, caseId, NOTIFICATION_REQUEST),
                        printStatus(notificationId, materialId, caseId, NOTIFICATION_REQUEST_ACCEPTED)));

        notificationListener.printRequestSucceeded(printRequestedEvent);

        verify(notificationStatusRepository).save(notificationStatusCaptor.capture());

        final NotificationStatusEntity actualStatus = notificationStatusCaptor.getValue();
        assertThat(actualStatus.getCaseId(), equalTo(caseId));
        assertThat(actualStatus.getNotificationId(), equalTo(notificationId));
        assertThat(actualStatus.getNotificationStatus(), equalTo(NOTIFICATION_REQUEST_SUCCEEDED));
        assertThat(actualStatus.getUpdated().toInstant(), equalTo(sentTime.toInstant()));
    }

    private NotificationStatusEntity printStatus(final UUID notificationId,
                                    final UUID materialId,
                                    final UUID caseId,
                                    final NotificationStatus printStatusType) {
        final NotificationStatusEntity notificationStatus = new NotificationStatusEntity();
        notificationStatus.setNotificationStatus(printStatusType);
        notificationStatus.setCaseId(caseId);
        notificationStatus.setNotificationId(notificationId);
        notificationStatus.setMaterialId(materialId);
        return notificationStatus;
    }
}