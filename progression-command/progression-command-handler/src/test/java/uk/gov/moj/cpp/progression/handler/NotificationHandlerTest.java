package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
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
import uk.gov.moj.cpp.progression.domain.EmailNotification;
import uk.gov.moj.cpp.progression.domain.Notification;
import uk.gov.moj.cpp.progression.domain.NotificationRequestAccepted;
import uk.gov.moj.cpp.progression.domain.NotificationRequestFailed;
import uk.gov.moj.cpp.progression.domain.NotificationRequestSucceeded;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequested;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequested;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class NotificationHandlerTest {

    public static Stream<Arguments> completedAtTime() {
        return Stream.of(
                Arguments.of(new Object[]{null}),
                Arguments.of(now(UTC)));
    }

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
            NotificationRequestAccepted.class,
            EmailRequested.class);

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> eventStreamCaptor;

    @BeforeEach
    public void setUp() {
        initMocks(this);
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

    @ParameterizedTest
    @MethodSource("completedAtTime")
    public void shouldRecordResultOrderPrintRequestSuccessForCase(final ZonedDateTime completedAt) throws EventStreamException {

        final UUID notificationId = randomUUID();
        final ZonedDateTime sentTime = now;

        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("notificationId", notificationId.toString())
                .add("sentTime", sentTime.toString());
        if (nonNull(completedAt)) {
            objectBuilder.add("completedAt", completedAt.toString());
        }

        final JsonEnvelope printResultOrderSuccessEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.command.record-notification-request-success"),
                objectBuilder.build());

        final NotificationRequestSucceeded resultOrderPrintRequestSucceeded = new NotificationRequestSucceeded(caseId, null, null, notificationId, sentTime, completedAt);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.recordNotificationRequestSuccess(caseId, notificationId, sentTime, completedAt)).thenReturn(Stream.of(resultOrderPrintRequestSucceeded));

        notificationHandler.recordNotificationRequestSuccess(printResultOrderSuccessEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderSuccessEnvelope)
                                        .withName("progression.event.notification-request-succeeded"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.caseId", is(caseId.toString())),
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.sentTime", is(ZonedDateTimes.toString(sentTime))),
                                                nonNull(completedAt) ?
                                                        withJsonPath("$.completedAt", is(ZonedDateTimes.toString(completedAt))) :
                                                        withoutJsonPath("$.completedAt")

                                        )
                                ))
                )));
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("completedAtTime")
    public void shouldRecordResultOrderPrintRequestSuccessForMaterial(final ZonedDateTime completedAt) throws Exception {

        final UUID notificationId = randomUUID();
        final ZonedDateTime sentTime = now;
        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("notificationId", notificationId.toString())
                .add("sentTime", sentTime.toString());

        if (nonNull(completedAt)) {
            objectBuilder.add("completedAt", completedAt.toString());
        }

        final JsonEnvelope printResultOrderSuccessEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.command.record-notification-request-success"),
                objectBuilder.build());


        final NotificationRequestSucceeded resultOrderPrintRequestSucceeded = new NotificationRequestSucceeded(null, null, materialId, notificationId, sentTime, completedAt);

        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);
        when(materialAggregate.recordNotificationRequestSuccess(materialId, notificationId, sentTime, completedAt)).thenReturn(Stream.of(resultOrderPrintRequestSucceeded));

        notificationHandler.recordNotificationRequestSuccess(printResultOrderSuccessEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderSuccessEnvelope)
                                        .withName("progression.event.notification-request-succeeded"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.materialId", is(materialId.toString())),
                                                withJsonPath("$.notificationId", is(notificationId.toString())),
                                                withJsonPath("$.sentTime", is(ZonedDateTimes.toString(sentTime))), nonNull(completedAt) ?
                                                        withJsonPath("$.completedAt", is(ZonedDateTimes.toString(completedAt))) :
                                                        withoutJsonPath("$.completedAt")

                                        )
                                ))
                )));
    }

    @Test
    public void shouldRecordResultOrderPrintRequestAccepted() throws EventStreamException {

        final UUID notificationId = randomUUID();
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
    public void shouldRecordResultOrderPrintRequestAcceptedForApplicationId() throws
            EventStreamException {

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
    public void shouldRecordResultOrderPrintRequestAcceptedForMaterial() throws
            EventStreamException {

        final UUID notificationId = randomUUID();

        final ZonedDateTime acceptedTime = now;
        final JsonEnvelope jsonEnvelope = envelopeFrom(
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

        notificationHandler.recordNotificationRequestAccepted(jsonEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(jsonEnvelope)
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

    @Test
    public void shouldPrintEmail() throws EventStreamException {

        final UUID applicationId = randomUUID();

        final JsonEnvelope emailResultOrder = envelopeFrom(
                metadataWithRandomUUID("progression.command.email"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("applicationId", applicationId.toString())
                        .add("materialId", materialId.toString())
                        .add("notifications", createArrayBuilder().add(createObjectBuilder()
                                .add("notificationId", randomUUID().toString())
                                .add("templateId", randomUUID().toString())
                                .add("sendToAddress", "sendToAddress")
                                .build()))
                        .build());

        final List<Notification> notifications = new ArrayList<>();

        final EmailNotification emailNotification = new EmailNotification(caseId,materialId, null,notifications);

        final EmailRequested emailRequested = new EmailRequested(caseId,materialId, applicationId,notifications);

        when(converter.convert(emailResultOrder.payloadAsJsonObject(), EmailNotification.class)).thenReturn(emailNotification);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.recordEmailRequest(caseId, materialId,notifications)).thenReturn(Stream.of(emailRequested));

        notificationHandler.email(emailResultOrder);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(emailResultOrder)
                                        .withName("progression.event.email-requested"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.caseId", is(caseId.toString())),
                                                withJsonPath("$.materialId", is(materialId.toString()))
                                        )
                                ))
                )));
    }

}
