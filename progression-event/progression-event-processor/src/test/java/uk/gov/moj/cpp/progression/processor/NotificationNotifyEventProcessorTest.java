package uk.gov.moj.cpp.progression.processor;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.APPLICATION;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.CASE;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.MATERIAL;

import uk.gov.justice.core.courts.UpdateCourtDocumentPrintTime;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingNotificationHelper;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.SystemIdMapperService;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import javax.json.JsonObject;

@ExtendWith(MockitoExtension.class)
public class NotificationNotifyEventProcessorTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private HearingNotificationHelper hearingNotificationHelper;

    @Mock
    private SystemIdMapping systemIdMapping;

    @Mock
    private FileStorer fileStorer;

    @Mock
    private Logger logger;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<UpdateCourtDocumentPrintTime>> envelopeCaptor;

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

        verify(notificationService).recordNotificationRequestFailure(letterNotification, systemIdMapping.get().getTargetId(), CASE);
    }

    @Test
    public void shouldFailSilentlyAndLogMessage() throws FileServiceException {
        final UUID notificationId = randomUUID();

        final JsonEnvelope letterNotification = envelope()
                .withPayloadOf(notificationId.toString(), "notificationId").build();

        when(systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString())).thenReturn(empty());

        when(systemIdMapperService.getCppApplicationIdForNotificationId(notificationId.toString())).thenReturn(empty());

        when(systemIdMapperService.getCppMaterialIdForNotificationId(notificationId.toString())).thenReturn(empty());

        notificationNotifyEventProcessor.markNotificationAsFailed(letterNotification);

        verify(logger).info(format("No Case, Application or Material found for the given notification id: %s", notificationId));
    }

    @Test
    public void shouldHandleSucceededPrintOrderRequest() {
        final UUID notificationId = randomUUID();
        final Optional<SystemIdMapping> systemIdMapping = of(mock(SystemIdMapping.class));

        final JsonEnvelope letterNotification = envelope().with(metadataWithRandomUUID(UUID.randomUUID().toString()).withSource("LETTER"))
                .withPayloadOf(notificationId.toString(), "notificationId").build();
        when(systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString())).thenReturn(systemIdMapping);

        notificationNotifyEventProcessor.markNotificationAsSucceeded(letterNotification);

        verify(notificationService).recordNotificationRequestSuccess(letterNotification, systemIdMapping.get().getTargetId(), CASE);
    }

    @Test
    public void shouldHandleFailedPrintOrderRequestForApplication() throws FileServiceException {
        final UUID notificationId = randomUUID();
        final Optional<SystemIdMapping> systemIdMapping = of(mock(SystemIdMapping.class));

        final JsonEnvelope letterNotification = envelope()
                .withPayloadOf(notificationId.toString(), "notificationId").build();
        when(systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString())).thenReturn(Optional.empty());
        when(systemIdMapperService.getCppApplicationIdForNotificationId(notificationId.toString())).thenReturn(systemIdMapping);

        notificationNotifyEventProcessor.markNotificationAsFailed(letterNotification);

        verify(notificationService).recordNotificationRequestFailure(letterNotification, systemIdMapping.get().getTargetId(), APPLICATION);
    }

    @Test
    public void shouldHandleFailedPrintOrderRequestForMaterial() throws FileServiceException {
        final UUID notificationId = randomUUID();
        final Optional<SystemIdMapping> systemIdMapping = of(mock(SystemIdMapping.class));

        final JsonEnvelope letterNotification = envelope()
                .withPayloadOf(notificationId.toString(), "notificationId").build();
        when(systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString())).thenReturn(empty());
        when(systemIdMapperService.getCppApplicationIdForNotificationId(notificationId.toString())).thenReturn(empty());
        when(systemIdMapperService.getCppMaterialIdForNotificationId(notificationId.toString())).thenReturn(systemIdMapping);

        notificationNotifyEventProcessor.markNotificationAsFailed(letterNotification);

        verify(notificationService).recordNotificationRequestFailure(letterNotification, systemIdMapping.get().getTargetId(), MATERIAL);
    }

    @Test
    public void shouldHandleSucceededPrintOrderRequestForApplication() throws FileServiceException {
        final UUID notificationId = randomUUID();
        final Optional<SystemIdMapping> systemIdMapping = of(mock(SystemIdMapping.class));

        final JsonEnvelope letterNotification = envelope().with(metadataWithRandomUUID(UUID.randomUUID().toString()).withSource("EMAIL"))
                .withPayloadOf(notificationId.toString(), "notificationId")
                .withPayloadOf("defendant", "recipientType")
                .withPayloadOf(notificationId.toString(), "caseId")
                .withPayloadOf("emailBody", "emailBody")
                .withPayloadOf("emailSubject", "emailSubject")
                .withPayloadOf("sendToAddress@gmail.com", "sendToAddress")
                .withPayloadOf("replyToAddress@gmail.com", "replyToAddress")
                .build();
        when(systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString())).thenReturn(empty());
        when(systemIdMapperService.getCppApplicationIdForNotificationId(notificationId.toString())).thenReturn(systemIdMapping);
        doNothing().when(documentGeneratorService).generateNonNowDocument(eq(letterNotification), any(JsonObject.class), anyString(), any(), anyString());
        doNothing().when(hearingNotificationHelper).addCourtDocument(eq(letterNotification),any(), any(), anyString() );

        notificationNotifyEventProcessor.markNotificationAsSucceeded(letterNotification);

        verify(notificationService).recordNotificationRequestSuccess(letterNotification, systemIdMapping.get().getTargetId(), APPLICATION);
        verify(documentGeneratorService).generateNonNowDocument(eq(letterNotification), any(JsonObject.class), anyString(), any(), anyString());
        verify(hearingNotificationHelper).addCourtDocument(eq(letterNotification),any(), any(), anyString());
    }

    @Test
    public void shouldHandleSucceededPrintOrderRequestForMaterial() throws FileServiceException {
        final UUID notificationId = randomUUID();
        final Optional<SystemIdMapping> systemIdMapping = of(mock(SystemIdMapping.class));

        final JsonEnvelope letterNotification = envelope().with(metadataWithRandomUUID(UUID.randomUUID().toString()).withSource("LETTER"))
                .withPayloadOf(notificationId.toString(), "notificationId").build();
        when(systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString())).thenReturn(empty());
        when(systemIdMapperService.getCppApplicationIdForNotificationId(notificationId.toString())).thenReturn(empty());
        when(systemIdMapperService.getCppMaterialIdForNotificationId(notificationId.toString())).thenReturn(systemIdMapping);

        notificationNotifyEventProcessor.markNotificationAsSucceeded(letterNotification);

        verify(notificationService).recordNotificationRequestSuccess(letterNotification, systemIdMapping.get().getTargetId(), MATERIAL);
    }

    @Test
    public void shouldHandlePrintOrderRequestFailSilentlyAndLogMessage() throws FileServiceException {
        final UUID notificationId = randomUUID();

        final JsonEnvelope letterNotification = envelope().with(metadataWithRandomUUID(UUID.randomUUID().toString()).withSource("LETTER"))
                .withPayloadOf(notificationId.toString(), "notificationId").build();

        when(systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString())).thenReturn(empty());

        when(systemIdMapperService.getCppApplicationIdForNotificationId(notificationId.toString())).thenReturn(empty());

        when(systemIdMapperService.getCppMaterialIdForNotificationId(notificationId.toString())).thenReturn(empty());

        notificationNotifyEventProcessor.markNotificationAsSucceeded(letterNotification);

        verify(logger).info(format("No Case, Application or Material found for the given notification id: %s", notificationId));
    }

    @Test
    public void shouldDeleteAssociatedFileAndUpdatePrintDateTimeWhenNotificationSendSucceeded() throws FileServiceException {
        final UUID notificationId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final ZonedDateTime completedAt = now();
        final JsonEnvelope notificationSucceededEvent = envelope().with(metadataWithRandomUUID("progression.event.notification-request-succeeded"))
                .withPayloadOf(notificationId.toString(), "notificationId")
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(completedAt.toString(), "completedAt")
                .build();
        when(systemIdMapperService.getDocumentIdForMaterialId(anyString())).thenReturn(of(this.systemIdMapping));
        when(this.systemIdMapping.getTargetId()).thenReturn(courtDocumentId);
        notificationNotifyEventProcessor.handleNotificationRequestSucceeded(notificationSucceededEvent);

        verify(fileStorer).delete(notificationId);
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<UpdateCourtDocumentPrintTime> command = envelopeCaptor.getValue();
        final UpdateCourtDocumentPrintTime courtDocumentPrintTime = command.payload();
        assertThat(command.metadata().name(), is("progression.command.update-court-document-print-time"));
        assertThat(courtDocumentPrintTime.getCourtDocumentId(), is(courtDocumentId));
        assertThat(courtDocumentPrintTime.getMaterialId(), is(materialId));
        assertThat(courtDocumentPrintTime.getPrintedAt(), is(completedAt));
    }

    @Test
    public void shouldOnlyDeleteAssociatedFileAndNotUpdatePrintDateTimeWhenCompletionTimeNotAvailable() throws FileServiceException {
        final UUID notificationId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final JsonEnvelope notificationSucceededEvent = envelope().with(metadataWithRandomUUID("progression.event.notification-request-succeeded"))
                .withPayloadOf(notificationId.toString(), "notificationId")
                .withPayloadOf(materialId.toString(), "materialId")
                .build();
        notificationNotifyEventProcessor.handleNotificationRequestSucceeded(notificationSucceededEvent);

        verify(fileStorer).delete(notificationId);
        verifyNoInteractions(systemIdMapperService, systemIdMapping);
    }

    @Test
    public void shouldOnlyDeleteAssociatedFileAndNotUpdatePrintDateTimeWhenMaterialIsNotAvailable() throws FileServiceException {
        final UUID notificationId = randomUUID();
        final ZonedDateTime completedAt = now();
        final UUID courtDocumentId = randomUUID();
        final JsonEnvelope notificationSucceededEvent = envelope().with(metadataWithRandomUUID("progression.event.notification-request-succeeded"))
                .withPayloadOf(notificationId.toString(), "notificationId")
                .withPayloadOf(completedAt.toString(), "completedAt")
                .build();
         notificationNotifyEventProcessor.handleNotificationRequestSucceeded(notificationSucceededEvent);

        verify(fileStorer).delete(notificationId);
        verifyNoInteractions(systemIdMapperService, systemIdMapping);
    }

    @Test
    public void shouldDeleteAssociatedFileWhenNotificationSendFailed() throws FileServiceException {
        final UUID notificationId = randomUUID();
        final JsonEnvelope notificationFailedEvent = envelope()
                .withPayloadOf(notificationId.toString(), "notificationId").build();

        notificationNotifyEventProcessor.handleNotificationRequestFailed(notificationFailedEvent);

        verify(fileStorer).delete(notificationId);
    }

    @Test
    public void shouldSilentlyFailAndLogWhenUnableToDeleteFileAssociatedWithNotification() throws FileServiceException {
        final UUID notificationId = randomUUID();
        final JsonEnvelope notificationFailedEvent = envelope()
                .withPayloadOf(notificationId.toString(), "notificationId").build();

        final FileServiceException exception = new FileServiceException("Delete from metadata table affected 0 rows!");
        doThrow(exception).when(fileStorer).delete(notificationId);

        notificationNotifyEventProcessor.handleNotificationRequestFailed(notificationFailedEvent);

        verify(fileStorer).delete(notificationId);

        verify(logger).debug(format("Failed to delete file for given notification id: '%s' from FileService. This could be due to the notification not having an associated file.", notificationId), exception);

    }
}