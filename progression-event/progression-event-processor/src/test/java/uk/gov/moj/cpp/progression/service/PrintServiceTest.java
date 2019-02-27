package uk.gov.moj.cpp.progression.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PrintServiceTest {

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), Json.createObjectBuilder().build());
    private UUID caseId;
    private UUID materialId;
    private UUID notificationId;
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();
    @Spy
    private SystemIdMapperService systemIdMapperService;
    @Mock
    private Requester requester;
    @Mock
    private Sender sender;
    @Mock
    private UtcClock clock;
    @InjectMocks
    private PrintService printService = new PrintService();

    @Before
    public void setUp() {
        this.caseId = randomUUID();
        this.materialId = randomUUID();
        this.notificationId = randomUUID();
    }


    @Test
    public void shouldExecutePrint() {
        final UUID notificationId = randomUUID();

        doNothing().when(systemIdMapperService).mapNotificationIdToCaseId(caseId, notificationId);

        printService.print(envelope, caseId, notificationId, materialId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("progression.command.print"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.notificationId", equalTo(notificationId.toString())),
                        withJsonPath("$.materialId", equalTo(materialId.toString())))
                ))));
    }

    @Test
    public void shouldRecordPrintRecordFailure() {
        final UUID notificationId = randomUUID();
        final String failedTime = ZonedDateTimes.toString(new UtcClock().now());
        final String errorMessage = "error message";
        final int statusCode = SC_NOT_FOUND;

        final JsonEnvelope eventEnvelope = envelope()
                .with(metadataBuilder()
                        .withName("public.notificationnotify.events.notification-failed")
                        .withId(randomUUID()))
                .withPayloadOf(notificationId, "notificationId")
                .withPayloadOf(failedTime, "failedTime")
                .withPayloadOf(errorMessage, "errorMessage")
                .withPayloadOf(statusCode, "statusCode")
                .build();

        printService.recordPrintRequestFailure(eventEnvelope, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(eventEnvelope).withName("progression.command.record-print-request-failure"),
                payloadIsJson(allOf(
                        withJsonPath("$.notificationId", equalTo(notificationId.toString())),
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.failedTime", equalTo(failedTime)),
                        withJsonPath("$.errorMessage", equalTo(errorMessage)),
                        withJsonPath("$.statusCode", equalTo(statusCode)))
                ))));
    }

    @Test
    public void shouldRecordPrintSuccess() {
        final UUID notificationId = randomUUID();
        final String sentTime = ZonedDateTimes.toString(new UtcClock().now());

        final JsonEnvelope eventEnvelope = envelope()
                .with(metadataBuilder()
                        .withName("public.notificationnotify.events.notification-sent")
                        .withId(randomUUID()))
                .withPayloadOf(notificationId, "notificationId")
                .withPayloadOf(sentTime, "sentTime")
                .build();

        printService.recordPrintRequestSuccess(eventEnvelope, caseId);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(eventEnvelope).withName("progression.command.record-print-request-success"),
                payloadIsJson(allOf(
                        withJsonPath("$.notificationId", equalTo(notificationId.toString())),
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.sentTime", equalTo(sentTime)))
                ))));
    }

    @Test
    public void shouldRecordPrintRequestAcceptedCommand() throws Exception {

        final ZonedDateTime acceptedTime = new UtcClock().now();

        final JsonEnvelope sourceEnvelope = envelope()
                .with(metadataBuilder()
                        .withName("progression.event.print-requested")
                        .withId(randomUUID())
                )
                .withPayloadOf(notificationId, "notificationId")
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(materialId, "materialId")
                .build();


        when(clock.now()).thenReturn(acceptedTime);

        printService.recordPrintRequestAccepted(sourceEnvelope);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(sourceEnvelope).withName("progression.command.record-print-request-accepted"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.notificationId", equalTo(notificationId.toString())),
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.acceptedTime", equalTo(ZonedDateTimes.toString(acceptedTime)))
                        )))));

    }
}
