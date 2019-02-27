package uk.gov.moj.cpp.progression.handler;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

/**
 * Created by satishkumar on 13/11/2018.
 */
@SuppressWarnings("WeakerAccess")
@ServiceComponent(COMMAND_HANDLER)
public class PrintHandler {
    private static final String CASE_ID = "caseId";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String MATERIAL_ID = "materialId";

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter converter;


    @Handles("progression.command.print")
    public void print(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID caseId = fromString(payload.getString(CASE_ID));
        final UUID notificationId = fromString(payload.getString(NOTIFICATION_ID));
        final UUID materialId = fromString(payload.getString(MATERIAL_ID));

        appendAggregateEvents(command, caseId, CaseAggregate.class,
                aggregate -> aggregate.recordPrintRequest(caseId, notificationId, materialId));
    }

    @Handles("progression.command.record-print-request-failure")
    public void recordPrintRequestFailure(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();

        final ZonedDateTime failedTime = ZonedDateTimes.fromString(payload.getString("failedTime"));
        final String errorMessage = payload.getString("errorMessage");
        final UUID caseId = fromString(payload.getString(CASE_ID));
        final UUID notificationId = fromString(payload.getString(NOTIFICATION_ID));
        final Optional<Integer> statusCode = payload.containsKey("statusCode") ? of(payload.getInt("statusCode")) : empty();

        appendAggregateEvents(command, caseId, CaseAggregate.class,
                aggregate -> aggregate.recordPrintRequestFailure(caseId, notificationId, failedTime, errorMessage, statusCode));
    }

    @Handles("progression.command.record-print-request-success")
    public void recordPrintRequestSuccess(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();

        final ZonedDateTime sentTime = ZonedDateTimes.fromString(payload.getString("sentTime"));
        final UUID caseId = fromString(payload.getString(CASE_ID));
        final UUID notificationId = fromString(payload.getString(NOTIFICATION_ID));

        appendAggregateEvents(command, caseId, CaseAggregate.class,
                aggregate -> aggregate.recordPrintRequestSuccess(caseId, notificationId, sentTime));
    }

    @Handles("progression.command.record-print-request-accepted")
    public void recordPrintRequestAccepted(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();

        final UUID caseId = fromString(payload.getString(CASE_ID));
        final UUID notificationId = fromString(payload.getString(NOTIFICATION_ID));
        final ZonedDateTime acceptedTime = ZonedDateTimes.fromString(payload.getString("acceptedTime"));

        appendAggregateEvents(command, caseId, CaseAggregate.class,
                aggregate -> aggregate.recordPrintRequestAccepted(caseId, notificationId, acceptedTime));
    }

    private <T extends Aggregate> Stream<Object> appendAggregateEvents(final JsonEnvelope command,
                                                                       final UUID streamId,
                                                                       final Class<T> aggregateClass,
                                                                       final Function<T, Stream<Object>> function) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(streamId);
        final T aggregate = aggregateService.get(eventStream, aggregateClass);
        final Stream<Object> events = function.apply(aggregate);
        if (events != null) {
            eventStream.append(events.map(enveloper.withMetadataFrom(command)));
        }
        return events;
    }
}
