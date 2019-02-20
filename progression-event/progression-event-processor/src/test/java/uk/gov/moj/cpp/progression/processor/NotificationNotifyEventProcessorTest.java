package uk.gov.moj.cpp.progression.processor;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.PrintService;
import uk.gov.moj.cpp.progression.service.SystemIdMapperService;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class NotificationNotifyEventProcessorTest {

    @Mock
    private PrintService printService;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Mock
    private Logger logger;

    @InjectMocks
    private NotificationNotifyEventProcessor notificationNotifyEventProcessor;

    @Test
    public void shouldHandleFailedPrintOrderRequest() {
        final UUID notificationId = randomUUID();
        final Optional<SystemIdMapping> systemIdMapping = of(mock(SystemIdMapping.class));

        final JsonEnvelope letterNotification = envelope()
                .withPayloadOf(notificationId.toString(), "notificationId").build();
        when(systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString())).thenReturn(systemIdMapping);

        notificationNotifyEventProcessor.markNotificationAsFailed(letterNotification);

        verify(printService).recordPrintRequestFailure(letterNotification, systemIdMapping.get().getTargetId());
    }

    @Test
    public void shouldFailSilentlyAndLogMessage() {
        final UUID notificationId = randomUUID();

        final JsonEnvelope letterNotification = envelope()
                .withPayloadOf(notificationId.toString(), "notificationId").build();
        when(systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString())).thenReturn(empty());

        notificationNotifyEventProcessor.markNotificationAsFailed(letterNotification);

        verify(logger).error(format("No case found for the given notification id: %s", notificationId));
    }

    @Test
    public void shouldHandleSucceededPrintOrderRequest() {
        final UUID notificationId = randomUUID();
        final Optional<SystemIdMapping> systemIdMapping = of(mock(SystemIdMapping.class));

        final JsonEnvelope letterNotification = envelope()
                .withPayloadOf(notificationId.toString(), "notificationId").build();
        when(systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString())).thenReturn(systemIdMapping);

        notificationNotifyEventProcessor.markNotificationAsSucceeded(letterNotification);

        verify(printService).recordPrintRequestSuccess(letterNotification, systemIdMapping.get().getTargetId());
    }
}