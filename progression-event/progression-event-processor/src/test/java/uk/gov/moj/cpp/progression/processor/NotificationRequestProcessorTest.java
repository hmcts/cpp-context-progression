package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.progression.service.NotificationService;

import java.util.UUID;

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


    @SuppressWarnings("deprecation")
    @Test
    public void shouldPrintResultOrder() throws Exception {

        final String clientId = randomUUID().toString();
        final UUID notificationId = randomUUID();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("resulting.events.result-order-print-requested")
                        .withClientCorrelationId(clientId),
                createObjectBuilder()
                        .add("materialId", materialId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("caseId", caseId.toString())
                        .build());

        notificationRequestProcessor.printDocument(event);

        verify(notificationNotifyService).sendLetterNotification(event, notificationId, materialId);
        verify(notificationService).recordPrintRequestAccepted(event);
    }


}
