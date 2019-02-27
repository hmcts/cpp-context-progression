package uk.gov.moj.cpp.progression.command;

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
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequestAccepted;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequestFailed;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequestSucceeded;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequested;
import uk.gov.moj.cpp.progression.handler.PrintHandler;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PrintHandlerTest {

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
    private PrintHandler printHandler;

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
    private UtcClock utcClock;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            PrintRequested.class,
            PrintRequestFailed.class,
            PrintRequestSucceeded.class,
            PrintRequestAccepted.class);

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
                metadataWithRandomUUID("resulting.command.print"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("materialId", materialId.toString())
                        .build());

        final PrintRequested resultOrderPrintRequested = new PrintRequested(caseId, notificationId, materialId);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.recordPrintRequest(caseId, notificationId, materialId)).thenReturn(Stream.of(resultOrderPrintRequested));

        printHandler.print(printResultOrder);

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
    public void shouldRecordResultOrderPrintRequestFailure() throws EventStreamException {

        final UUID notificationId = randomUUID();
        final ZonedDateTime failedTime = now;
        final String errorMessage = "Permanent failure to send letter";
        final int statusCode = 400;
        final JsonEnvelope printResultOrderFailedEnvelope = envelopeFrom(
                metadataWithRandomUUID("resulting.command.record-print-request-failure"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("errorMessage", errorMessage)
                        .add("failedTime", failedTime.toString())
                        .add("statusCode", statusCode)
                        .build());


        final PrintRequestFailed resultOrderPrintRequestFailed = new PrintRequestFailed(caseId, notificationId, failedTime, errorMessage, of(statusCode));

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.recordPrintRequestFailure(caseId, notificationId, failedTime, errorMessage, of(statusCode))).thenReturn(Stream.of(resultOrderPrintRequestFailed));

        printHandler.recordPrintRequestFailure(printResultOrderFailedEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderFailedEnvelope)
                                        .withName("progression.event.print-request-failed"),
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
    public void shouldRecordResultOrderPrintRequestSuccess() throws EventStreamException {

        final UUID notificationId = randomUUID();
        final ZonedDateTime sentTime = now;
        final JsonEnvelope printResultOrderSucceessEnvelope = envelopeFrom(
                metadataWithRandomUUID("resulting.command.record-print-request-success"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("sentTime", sentTime.toString())
                        .build());


        final PrintRequestSucceeded resultOrderPrintRequestSucceeded = new PrintRequestSucceeded(caseId, notificationId, sentTime);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.recordPrintRequestSuccess(caseId, notificationId, sentTime)).thenReturn(Stream.of(resultOrderPrintRequestSucceeded));

        printHandler.recordPrintRequestSuccess(printResultOrderSucceessEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderSucceessEnvelope)
                                        .withName("progression.event.print-request-succeeded"),
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
    public void shouldRecordResultOrderPrintRequestAccepted() throws EventStreamException {

        final UUID notificationId = randomUUID();
        final String caseUrn = "TFL75947ZQ8UE";
        final ZonedDateTime acceptedTime = now;
        final JsonEnvelope printResultOrderAcceptedEnvelope = envelopeFrom(
                metadataWithRandomUUID("resulting.command.record-print-request-accepted"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("acceptedTime", acceptedTime.toString())
                        .build());

        final PrintRequestAccepted resultOrderPrintRequestAccepted = new PrintRequestAccepted(
                caseId,
                notificationId,
                acceptedTime);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.recordPrintRequestAccepted(caseId, notificationId, acceptedTime)).thenReturn(Stream.of(resultOrderPrintRequestAccepted));

        printHandler.recordPrintRequestAccepted(printResultOrderAcceptedEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(printResultOrderAcceptedEnvelope)
                                        .withName("progression.event.print-request-accepted"),
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
    public void shouldHandleCommandsAndPassThrough() {
        assertThat(PrintHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(allOf(
                        method("recordPrintRequestAccepted")
                                .thatHandles("progression.command.record-print-request-accepted"),
                        method("recordPrintRequestSuccess")
                                .thatHandles("progression.command.record-print-request-success"),
                        method("print")
                                .thatHandles("progression.command.print"),
                        method("recordPrintRequestFailure")
                                .thatHandles("progression.command.record-print-request-failure")
                )));
    }

}
