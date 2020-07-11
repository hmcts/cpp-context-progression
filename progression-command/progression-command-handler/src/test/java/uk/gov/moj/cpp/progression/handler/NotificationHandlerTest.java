package uk.gov.moj.cpp.progression.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;
import uk.gov.moj.cpp.progression.domain.NotificationRequestAccepted;
import uk.gov.moj.cpp.progression.domain.NotificationRequestFailed;
import uk.gov.moj.cpp.progression.domain.NotificationRequestSucceeded;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequested;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

@RunWith(MockitoJUnitRunner.class)
public class NotificationHandlerTest {

    private UUID caseId;
    private UUID materialId;
    private ZonedDateTime now = now(UTC);

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private JsonObjectToObjectConverter converter;

    @InjectMocks
    private NotificationHandler notificationHandler;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private Stream<Object> events;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventStream magistratesQueueEventStream;

    @Mock
    private EventStream delegatedPowersQueueEventStream;

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private ApplicationAggregate applicationAggregate;

    @Mock
    private MaterialAggregate materialAggregate;

    @Mock
    private UtcClock utcClock;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            PrintRequested.class,
            NotificationRequestFailed.class,
            NotificationRequestSucceeded.class,
            NotificationRequestAccepted.class);

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> eventStreamCaptor;

    @Before
    public void setUp() {
        caseId = randomUUID();
        materialId = randomUUID();
    }

    @Test
    public void shouldPrintResultOrder() throws EventStreamException {

        final UUID notificationId = randomUUID();

        final JsonEnvelope printResultOrder = envelopeFrom(
                metadataWithRandomUUID("progression.command.print"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("materialId", materialId.toString())
                        .add("postage", false)
                        .build());

        final PrintRequested resultOrderPrintRequested = new PrintRequested(notificationId, null, caseId, materialId, false);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.recordPrintRequest(caseId, notificationId, materialId, false)).thenReturn(Stream.of(resultOrderPrintRequested));

        notificationHandler.print(printResultOrder);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrder)
                                        .withName("progression.event.print-requested"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.caseId", is(caseId.toString())),
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.materialId", is(materialId.toString()))
                                        )
                                ))
                )));
    }

    @Test
    public void shouldPrintResultOrderForApplication() throws EventStreamException {

        final UUID notificationId = randomUUID();
        final UUID applicationId = randomUUID();

        final JsonEnvelope printResultOrder = envelopeFrom(
                metadataWithRandomUUID("progression.command.print"),
                createObjectBuilder()
                        .add("applicationId", applicationId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("materialId", materialId.toString())
                        .add("postage", false)
                        .build());

        final PrintRequested resultOrderPrintRequested = new PrintRequested(notificationId, applicationId, null, materialId, false);

        when(eventSource.getStreamById(applicationId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        when(applicationAggregate.recordPrintRequest(applicationId, notificationId, materialId, false)).thenReturn(Stream.of(resultOrderPrintRequested));

        notificationHandler.print(printResultOrder);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrder)
                                        .withName("progression.event.print-requested"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.applicationId", is(applicationId.toString())),
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.materialId", is(materialId.toString()))
                                        )
                                ))
                )));
    }

    @Test
    public void shouldPrintResultOrderForMaterial() throws EventStreamException {

        final UUID notificationId = randomUUID();

        final JsonEnvelope printResultOrder = envelopeFrom(
                metadataWithRandomUUID("progression.command.print"),
                createObjectBuilder()
                        .add("notificationId", notificationId.toString())
                        .add("materialId", materialId.toString())
                        .add("postage", false)
                        .build());

        final PrintRequested resultOrderPrintRequested = new PrintRequested(notificationId, null, null, materialId, false);

        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);
        when(materialAggregate.recordPrintRequest(materialId, notificationId, false)).thenReturn(Stream.of(resultOrderPrintRequested));

        notificationHandler.print(printResultOrder);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrder)
                                        .withName("progression.event.print-requested"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.materialId", is(materialId.toString())),
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.materialId", is(materialId.toString()))
                                        )
                                ))
                )));
    }

    @Test
    public void shouldRecordResultOrderPrintRequestFailureForCase() throws EventStreamException {

        final UUID notificationId = randomUUID();
        final ZonedDateTime failedTime = now;
        final String errorMessage = "Permanent failure to send letter";
        final int statusCode = 400;
        final JsonEnvelope printResultOrderFailedEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.command.record-notification-request-failure"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("errorMessage", errorMessage)
                        .add("failedTime", failedTime.toString())
                        .add("statusCode", statusCode)
                        .build());


        final NotificationRequestFailed resultOrderPrintRequestFailed = new NotificationRequestFailed(caseId, null, null, notificationId, failedTime, errorMessage, of(statusCode));

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.recordNotificationRequestFailure(caseId, notificationId, failedTime, errorMessage, of(statusCode))).thenReturn(Stream.of(resultOrderPrintRequestFailed));

        notificationHandler.recordNotificationRequestFailure(printResultOrderFailedEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderFailedEnvelope)
                                        .withName("progression.event.notification-request-failed"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.caseId", is(caseId.toString())),
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.errorMessage", is(errorMessage)),
                                                withJsonPath("$.statusCode", is(statusCode)),
                                                withJsonPath("$.failedTime", is(ZonedDateTimes.toString(failedTime)))

                                        )
                                ))
                )));
    }

    @Test
    public void shouldRecordResultOrderPrintRequestFailureForMaterial() throws EventStreamException {

        final UUID notificationId = randomUUID();
        final ZonedDateTime failedTime = now;
        final String errorMessage = "Permanent failure to send letter";
        final int statusCode = 400;
        final JsonEnvelope printResultOrderFailedEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.command.record-notification-request-failure"),
                createObjectBuilder()
                        .add("materialId", materialId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("errorMessage", errorMessage)
                        .add("failedTime", failedTime.toString())
                        .add("statusCode", statusCode)
                        .build());


        final NotificationRequestFailed resultOrderPrintRequestFailed = new NotificationRequestFailed(null, null, materialId, notificationId, failedTime, errorMessage, of(statusCode));

        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);
        when(materialAggregate.recordNotificationRequestFailure(materialId, notificationId, failedTime, errorMessage, of(statusCode))).thenReturn(Stream.of(resultOrderPrintRequestFailed));

        notificationHandler.recordNotificationRequestFailure(printResultOrderFailedEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderFailedEnvelope)
                                        .withName("progression.event.notification-request-failed"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.materialId", is(materialId.toString())),
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.errorMessage", is(errorMessage)),
                                                withJsonPath("$.statusCode", is(statusCode)),
                                                withJsonPath("$.failedTime", is(ZonedDateTimes.toString(failedTime)))

                                        )
                                ))
                )));
    }


    @Test
    public void shouldRecordResultOrderPrintRequestSuccessForCase() throws EventStreamException {

        final UUID notificationId = randomUUID();
        final ZonedDateTime sentTime = now;
        final JsonEnvelope printResultOrderSucceessEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.command.record-notification-request-success"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("sentTime", sentTime.toString())
                        .build());


        final NotificationRequestSucceeded resultOrderPrintRequestSucceeded = new NotificationRequestSucceeded(caseId, null, null, notificationId, sentTime);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.recordNotificationRequestSuccess(caseId, notificationId, sentTime)).thenReturn(Stream.of(resultOrderPrintRequestSucceeded));

        notificationHandler.recordNotificationRequestSuccess(printResultOrderSucceessEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderSucceessEnvelope)
                                        .withName("progression.event.notification-request-succeeded"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.caseId", is(caseId.toString())),
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.sentTime", is(ZonedDateTimes.toString(sentTime)))

                                        )
                                ))
                )));
    }

    @Test
    public void shouldRecordResultOrderPrintRequestSuccessForMaterial() throws EventStreamException {

        final UUID notificationId = randomUUID();
        final ZonedDateTime sentTime = now;
        final JsonEnvelope printResultOrderSucceessEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.command.record-notification-request-success"),
                createObjectBuilder()
                        .add("materialId", materialId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("sentTime", sentTime.toString())
                        .build());


        final NotificationRequestSucceeded resultOrderPrintRequestSucceeded = new NotificationRequestSucceeded(null, null, materialId, notificationId, sentTime);

        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);
        when(materialAggregate.recordNotificationRequestSuccess(materialId, notificationId, sentTime)).thenReturn(Stream.of(resultOrderPrintRequestSucceeded));

        notificationHandler.recordNotificationRequestSuccess(printResultOrderSucceessEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderSucceessEnvelope)
                                        .withName("progression.event.notification-request-succeeded"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.materialId", is(materialId.toString())),
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.sentTime", is(ZonedDateTimes.toString(sentTime)))

                                        )
                                ))
                )));
    }

    @Test
    public void shouldRecordResultOrderPrintRequestAccepted() throws EventStreamException {

        final UUID notificationId = randomUUID();
        final String caseUrn = "TFL75947ZQ8UE";
        final ZonedDateTime acceptedTime = now;
        final JsonEnvelope printResultOrderAcceptedEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.command.record-notification-request-accepted"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("materialId", materialId.toString())
                        .add("acceptedTime", acceptedTime.toString())
                        .build());

        final NotificationRequestAccepted resultOrderNotificationRequestAccepted = new NotificationRequestAccepted(
                caseId,
                null,
                materialId,
                notificationId,
                acceptedTime);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.recordNotificationRequestAccepted(caseId, materialId, notificationId, acceptedTime)).thenReturn(Stream.of(resultOrderNotificationRequestAccepted));

        notificationHandler.recordNotificationRequestAccepted(printResultOrderAcceptedEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderAcceptedEnvelope)
                                        .withName("progression.event.notification-request-accepted"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.caseId", is(caseId.toString())),
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.acceptedTime", is(ZonedDateTimes.toString(acceptedTime)))

                                        )
                                ))
                )));
    }

    @Test
    public void shouldRecordResultOrderPrintRequestAcceptedForApplicationId() throws EventStreamException {

        final UUID notificationId = randomUUID();
        final UUID applicationId = randomUUID();

        final ZonedDateTime acceptedTime = now;
        final JsonEnvelope printResultOrderAcceptedEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.command.record-notification-request-accepted"),
                createObjectBuilder()
                        .add("applicationId", applicationId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("materialId", materialId.toString())
                        .add("acceptedTime", acceptedTime.toString())
                        .build());

        final NotificationRequestAccepted resultOrderNotificationRequestAccepted = new NotificationRequestAccepted(
                null,
                applicationId,
                materialId,
                notificationId,
                acceptedTime);

        when(eventSource.getStreamById(applicationId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        when(applicationAggregate.recordNotificationRequestAccepted(applicationId, materialId, notificationId, acceptedTime)).thenReturn(Stream.of(resultOrderNotificationRequestAccepted));

        notificationHandler.recordNotificationRequestAccepted(printResultOrderAcceptedEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderAcceptedEnvelope)
                                        .withName("progression.event.notification-request-accepted"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.applicationId", is(applicationId.toString())),
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.acceptedTime", is(ZonedDateTimes.toString(acceptedTime)))

                                        )
                                ))
                )));
    }

    @Test
    public void shouldRecordResultOrderPrintRequestAcceptedForMaterial() throws EventStreamException {

        final UUID notificationId = randomUUID();

        final ZonedDateTime acceptedTime = now;
        final JsonEnvelope printResultOrderAcceptedEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.command.record-notification-request-accepted"),
                createObjectBuilder()
                        .add("notificationId", notificationId.toString())
                        .add("materialId", materialId.toString())
                        .add("acceptedTime", acceptedTime.toString())
                        .build());

        final NotificationRequestAccepted resultOrderNotificationRequestAccepted = new NotificationRequestAccepted(
                null,
                null,
                materialId,
                notificationId,
                acceptedTime);

        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);
        when(materialAggregate.recordNotificationRequestAccepted(materialId, notificationId, acceptedTime)).thenReturn(Stream.of(resultOrderNotificationRequestAccepted));

        notificationHandler.recordNotificationRequestAccepted(printResultOrderAcceptedEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderAcceptedEnvelope)
                                        .withName("progression.event.notification-request-accepted"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.acceptedTime", is(ZonedDateTimes.toString(acceptedTime)))

                                        )
                                ))
                )));
    }

    @Test
    public void shouldHandleCommandsAndPassThrough() {
        assertThat(NotificationHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(allOf(
                        method("recordNotificationRequestAccepted")
                                .thatHandles("progression.command.record-notification-request-accepted"),
                        method("recordNotificationRequestSuccess")
                                .thatHandles("progression.command.record-notification-request-success"),
                        method("print")
                                .thatHandles("progression.command.print"),
                        method("recordNotificationRequestFailure")
                                .thatHandles("progression.command.record-notification-request-failure")
                )));
    }

}
