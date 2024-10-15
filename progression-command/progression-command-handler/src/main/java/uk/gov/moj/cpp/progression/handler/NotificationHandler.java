package uk.gov.moj.cpp.progression.handler;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.nonNull;
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
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;
import uk.gov.moj.cpp.progression.domain.EmailNotification;

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
public class NotificationHandler {
    private static final String CASE_ID = "caseId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String MATERIAL_ID = "materialId";
    private static final String POSTAGE = "postage";
    private static final String FAILED_TIME = "failedTime";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String STATUS_CODE = "statusCode";
    private static final String SENT_TIME = "sentTime";
    private static final String COMPLETED_AT = "completedAt";
    private static final String ACCEPTED_TIME = "acceptedTime";

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

        final UUID notificationId = fromString(payload.getString(NOTIFICATION_ID));
        final UUID materialId = fromString(payload.getString(MATERIAL_ID));
        final boolean postage = payload.containsKey(POSTAGE) && payload.getBoolean(POSTAGE);

        if (payload.containsKey(CASE_ID)) {
            final UUID caseId = fromString(payload.getString(CASE_ID));
            appendAggregateEvents(command, caseId, CaseAggregate.class,
                    aggregate -> aggregate.recordPrintRequest(caseId, notificationId, materialId, postage));
        }

        if (payload.containsKey(APPLICATION_ID)) {
            final UUID applicationId = fromString(payload.getString(APPLICATION_ID));
            appendAggregateEvents(command, applicationId, ApplicationAggregate.class,
                    aggregate -> aggregate.recordPrintRequest(applicationId, notificationId, materialId, postage));
        }

        if (!payload.containsKey(CASE_ID) && !payload.containsKey(APPLICATION_ID)) {
            appendAggregateEvents(command, materialId, MaterialAggregate.class,
                    aggregate -> aggregate.recordPrintRequest(materialId, notificationId, false));
        }
    }

    @Handles("progression.command.email")
    public void email(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();
        final EmailNotification emailNotification = converter.convert(payload, EmailNotification.class);

        if (nonNull(emailNotification.getCaseId())) {
            appendAggregateEvents(command, emailNotification.getCaseId(), CaseAggregate.class,
                    aggregate -> aggregate.recordEmailRequest(emailNotification.getCaseId(),
                            emailNotification.getMaterialId(), emailNotification.getNotifications()));
        }

        if (nonNull(emailNotification.getApplicationId())) {
            appendAggregateEvents(command, emailNotification.getApplicationId(), ApplicationAggregate.class,
                    aggregate -> aggregate.recordEmailRequest(emailNotification.getApplicationId(),
                            emailNotification.getMaterialId(), emailNotification.getNotifications()));
        }

        if (!payload.containsKey(CASE_ID) && !payload.containsKey(APPLICATION_ID) && payload.containsKey(MATERIAL_ID)) {
            final UUID materialId = fromString(payload.getString(MATERIAL_ID));
            appendAggregateEvents(command, materialId, MaterialAggregate.class,
                    aggregate -> aggregate.recordEmailRequest(materialId, emailNotification.getNotifications()));
        }
    }


    @Handles("progression.command.record-notification-request-failure")
    public void recordNotificationRequestFailure(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();

        final ZonedDateTime failedTime = ZonedDateTimes.fromString(payload.getString(FAILED_TIME));

        final String errorMessage = payload.getString(ERROR_MESSAGE);

        final UUID notificationId = fromString(payload.getString(NOTIFICATION_ID));

        final Optional<Integer> statusCode = payload.containsKey(STATUS_CODE) ? of(payload.getInt(STATUS_CODE)) : empty();

        if (payload.containsKey(CASE_ID)) {

            final UUID caseId = fromString(payload.getString(CASE_ID));

            appendAggregateEvents(command, caseId, CaseAggregate.class,
                    aggregate -> aggregate.recordNotificationRequestFailure(caseId, notificationId, failedTime, errorMessage, statusCode));
        } else if (payload.containsKey(APPLICATION_ID)) {

            final UUID applicationId = fromString(payload.getString(APPLICATION_ID));

            appendAggregateEvents(command, applicationId, ApplicationAggregate.class,
                    aggregate -> aggregate.recordNotificationRequestFailure(applicationId, notificationId, failedTime, errorMessage, statusCode));
        } else {
            final UUID materialId = fromString(payload.getString(MATERIAL_ID));

            appendAggregateEvents(command, materialId, MaterialAggregate.class,
                    aggregate -> aggregate.recordNotificationRequestFailure(materialId, notificationId, failedTime, errorMessage, statusCode));
        }

    }

    @Handles("progression.command.record-notification-request-success")
    public void recordNotificationRequestSuccess(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final ZonedDateTime sentTime = ZonedDateTimes.fromString(payload.getString(SENT_TIME));
        // optional attribute
        final ZonedDateTime completedAt = Optional.ofNullable(payload.getString(COMPLETED_AT, null)).map(ZonedDateTimes::fromString).orElse(null);
        final UUID notificationId = fromString(payload.getString(NOTIFICATION_ID));

        if (payload.containsKey(CASE_ID)) {
            final UUID caseId = fromString(payload.getString(CASE_ID));
            appendAggregateEvents(command, caseId, CaseAggregate.class,
                    aggregate -> aggregate.recordNotificationRequestSuccess(caseId, notificationId, sentTime, completedAt));

        } else if (payload.containsKey(APPLICATION_ID)) {
            final UUID applicationId = fromString(payload.getString(APPLICATION_ID));
            appendAggregateEvents(command, applicationId, ApplicationAggregate.class,
                    aggregate -> aggregate.recordNotificationRequestSuccess(applicationId, notificationId, sentTime, completedAt));
        } else {
            final UUID materialId = fromString(payload.getString(MATERIAL_ID));
            appendAggregateEvents(command, materialId, MaterialAggregate.class,
                    aggregate -> aggregate.recordNotificationRequestSuccess(materialId, notificationId, sentTime, completedAt));
        }
    }

    @Handles("progression.command.record-notification-request-accepted")
    public void recordNotificationRequestAccepted(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();

        final UUID notificationId = fromString(payload.getString(NOTIFICATION_ID));
        final ZonedDateTime acceptedTime = payload.containsKey(ACCEPTED_TIME) ? ZonedDateTimes.fromString(payload.getString(ACCEPTED_TIME)) : ZonedDateTime.now(UTC);

        if (payload.containsKey(CASE_ID)) {
            final UUID caseId = fromString(payload.getString(CASE_ID));
            final UUID materialId = payload.containsKey(MATERIAL_ID) ? fromString(payload.getString(MATERIAL_ID)) : null;
            appendAggregateEvents(command, caseId, CaseAggregate.class,
                    aggregate -> aggregate.recordNotificationRequestAccepted(caseId, materialId, notificationId, acceptedTime));
        }

        if (payload.containsKey(APPLICATION_ID)) {
            final UUID applicationId = fromString(payload.getString(APPLICATION_ID));
            final UUID materialId = payload.containsKey(MATERIAL_ID) ? fromString(payload.getString(MATERIAL_ID)) : null;
            appendAggregateEvents(command, applicationId, ApplicationAggregate.class,
                    aggregate -> aggregate.recordNotificationRequestAccepted(applicationId, materialId, notificationId, acceptedTime));
        }

        if (!payload.containsKey(CASE_ID) && !payload.containsKey(APPLICATION_ID) && payload.containsKey(MATERIAL_ID)) {
            final UUID materialId = fromString(payload.getString(MATERIAL_ID));
            appendAggregateEvents(command, materialId, MaterialAggregate.class,
                    aggregate -> aggregate.recordNotificationRequestAccepted(materialId, notificationId, acceptedTime));
        }
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
