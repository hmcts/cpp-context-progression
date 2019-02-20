package uk.gov.moj.cpp.prosecution.event.listener;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST_ACCEPTED;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST_FAILED;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST_SUCCEEDED;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.PrintStatusType;
import uk.gov.moj.cpp.prosecutioncase.event.listener.PrintListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrintStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PrintStatusRepository;

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
public class PrintListenerTest {

    private UUID caseId;
    private UUID materialId;
    private ZonedDateTime now;
    private PrintStatus printStatus;

    @Captor
    private ArgumentCaptor<PrintStatus> printStatusCaptor;

    @Mock
    private PrintStatusRepository printStatusRepository;

    @InjectMocks
    private PrintListener printListener;

    @Before
    public void init() {
        caseId = randomUUID();
        materialId = randomUUID();
        now = ZonedDateTime.now();
        printStatus = new PrintStatus();
        printStatus.setMaterialId(materialId);
    }

    @Test
    public void shouldCreateNewStatusWithPrintRequestedStatus() {
        final UUID notificationId = randomUUID();

        final JsonEnvelope printRequestedEvent = envelopeFrom(
                metadataWithRandomUUID("progression.event.print-requested").createdAt(now),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("materialId", materialId.toString())
                        .build());

        when(printStatusRepository.findBy(caseId)).thenReturn(null);

        printListener.printRequested(printRequestedEvent);

        verify(printStatusRepository).save(printStatusCaptor.capture());

        final PrintStatus actualStatus = printStatusCaptor.getValue();
        assertThat(actualStatus.getNotificationId(), equalTo(notificationId));
        assertThat(actualStatus.getMaterialId(), equalTo(materialId));
        assertThat(actualStatus.getStatus(), equalTo(PRINT_REQUEST));
        assertThat(actualStatus.getUpdated().toInstant(), equalTo(now.toInstant()));
    }

    @Test
    public void shouldCreateNewStatusWithPrintRequestAcceptedStatus() {
        final UUID notificationId = randomUUID();
        final ZonedDateTime acceptedTime = now;
        final JsonEnvelope printRequestAcceptedEvent = envelopeFrom(
                metadataWithRandomUUID("progression.event.print-request-accepted").createdAt(now),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("materialId", materialId.toString())
                        .add("acceptedTime", ZonedDateTimes.toString(acceptedTime))
                        .build());

        printStatus.setStatus(PRINT_REQUEST_ACCEPTED);
        when(printStatusRepository.findByNotificationId(notificationId)).thenReturn(Arrays
                .asList(printStatus(notificationId, materialId, caseId, PRINT_REQUEST)));
        printListener.printRequestAccepted(printRequestAcceptedEvent);

        verify(printStatusRepository).save(printStatusCaptor.capture());

        final PrintStatus actualStatus = printStatusCaptor.getValue();
        assertThat(actualStatus.getCaseId(), equalTo(caseId));
        assertThat(actualStatus.getNotificationId(), equalTo(notificationId));
        assertThat(actualStatus.getStatus(), equalTo(PRINT_REQUEST_ACCEPTED));
        assertThat(actualStatus.getUpdated().toInstant(), equalTo(now.toInstant()));
    }

    @Test
    public void shouldCreateNewStatusWithPrintRequestFailedStatus() {
        final UUID notificationId = randomUUID();
        final ZonedDateTime failedTime = now;
        final String errorMessage = "error message";
        final int statusCode = SC_NOT_FOUND;

        final JsonEnvelope printRequestedEvent = envelopeFrom(
                metadataWithRandomUUID("progression.event.print-request-failed").createdAt(now),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("errorMessage", errorMessage)
                        .add("statusCode", statusCode)
                        .add("failedTime", ZonedDateTimes.toString(failedTime))
                        .build());

        when(printStatusRepository.findByNotificationId(notificationId)).thenReturn(Arrays
                .asList(printStatus(notificationId, materialId, caseId, PRINT_REQUEST),
                        printStatus(notificationId, materialId, caseId, PRINT_REQUEST_ACCEPTED)));
        printListener.printRequestFailed(printRequestedEvent);

        verify(printStatusRepository).save(printStatusCaptor.capture());

        final PrintStatus actualStatus = printStatusCaptor.getValue();
        assertThat(actualStatus.getCaseId(), equalTo(caseId));
        assertThat(actualStatus.getNotificationId(), equalTo(notificationId));
        assertThat(actualStatus.getErrorMessage(), equalTo(errorMessage));
        assertThat(actualStatus.getStatusCode(), equalTo(statusCode));
        assertThat(actualStatus.getStatus(), equalTo(PRINT_REQUEST_FAILED));
        assertThat(actualStatus.getUpdated().toInstant(), equalTo(failedTime.toInstant()));
    }

    @Test
    public void shouldCreateNewStatusWithPrintRequestSucceededStatus() {
        final UUID notificationId = randomUUID();
        final ZonedDateTime sentTime = now;

        final JsonEnvelope printRequestedEvent = envelopeFrom(
                metadataWithRandomUUID("progression.event.print-request-succeeded").createdAt(now),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("sentTime", ZonedDateTimes.toString(sentTime))
                        .build());

        when(printStatusRepository.findByNotificationId(notificationId)).thenReturn(Arrays
                .asList(printStatus(notificationId, materialId, caseId, PRINT_REQUEST),
                        printStatus(notificationId, materialId, caseId, PRINT_REQUEST_ACCEPTED)));

        printListener.printRequestSucceeded(printRequestedEvent);

        verify(printStatusRepository).save(printStatusCaptor.capture());

        final PrintStatus actualStatus = printStatusCaptor.getValue();
        assertThat(actualStatus.getCaseId(), equalTo(caseId));
        assertThat(actualStatus.getNotificationId(), equalTo(notificationId));
        assertThat(actualStatus.getStatus(), equalTo(PRINT_REQUEST_SUCCEEDED));
        assertThat(actualStatus.getUpdated().toInstant(), equalTo(sentTime.toInstant()));
    }

    private PrintStatus printStatus(final UUID notificationId,
                                    final UUID materialId,
                                    final UUID caseId,
                                    final PrintStatusType printStatusType) {
        final PrintStatus printStatus = new PrintStatus();
        printStatus.setStatus(printStatusType);
        printStatus.setCaseId(caseId);
        printStatus.setNotificationId(notificationId);
        printStatus.setMaterialId(materialId);
        return printStatus;
    }
}