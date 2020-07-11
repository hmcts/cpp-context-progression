package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.progression.service.NotificationService;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("WeakerAccess")
@RunWith(MockitoJUnitRunner.class)
public class NotificationRequestProcessorTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private NotificationService notificationService;

    @Mock
    private Sender sender;

    @Mock
    private NotificationNotifyService notificationNotifyService;

    @InjectMocks
    private NotificationRequestProcessor notificationRequestProcessor;

    private UUID caseId;
    private UUID materialId;

    @Before
    public void setup() {
        caseId = randomUUID();
        materialId = randomUUID();
    }


    @Test
    public void shouldPrintDocument() throws Exception {

        final String clientId = randomUUID().toString();
        final UUID notificationId = randomUUID();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.print-requested")
                        .withClientCorrelationId(clientId),
                createObjectBuilder()
                        .add("materialId", materialId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("caseId", caseId.toString())
                        .add("postage", false)
                        .build());

        notificationRequestProcessor.printDocument(event);

        verify(notificationNotifyService).sendLetterNotification(event, notificationId, materialId, false);
        verify(notificationService).recordPrintRequestAccepted(event);
    }

    @Test
    public void shouldEmailDocument() {

        final String clientId = randomUUID().toString();
        final UUID notificationId = randomUUID();
        final JsonObject notification = createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("notificationId", notificationId.toString())
                .add("caseId", caseId.toString())
                .add("postage", false)
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("notifications", createArrayBuilder()
                        .add(notification)
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.email-requested")
                        .withClientCorrelationId(clientId), payload
        );

        notificationRequestProcessor.emailDocument(event);

        verify(notificationNotifyService).sendEmailNotification(event, notification);
        verify(notificationService).recordEmailRequestAccepted(event);
    }


}
