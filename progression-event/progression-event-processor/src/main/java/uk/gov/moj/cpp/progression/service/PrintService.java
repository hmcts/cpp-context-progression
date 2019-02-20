package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
/**
 * Created by satishkumar on 12/11/2018.
 */
@SuppressWarnings("WeakerAccess")
public class PrintService {

    public static final String CASE_ID = "caseId";
    public static final String NOTIFICATION_ID = "notificationId";
    public static final String MATERIAL_ID = "materialId";
    public static final String STATUS_CODE = "statusCode";
    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private UtcClock clock;


    public void print(final JsonEnvelope sourceEnvelope, final UUID caseId, final UUID notificationId, final UUID materialId) {

        systemIdMapperService.mapNotificationIdToCaseId(caseId, notificationId);

        final JsonObject payload = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(NOTIFICATION_ID, notificationId.toString())
                .add(MATERIAL_ID, materialId.toString())
                .build();

        sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.print")
                .apply(payload));
    }

    public void recordPrintRequestFailure(final JsonEnvelope sourceEnvelope, final UUID caseId) {
        final JsonObject payload = sourceEnvelope.payloadAsJsonObject();
        final String notificationId = payload.getString(NOTIFICATION_ID);
        final String failedTime = payload.getString("failedTime");
        final String errorMessage = payload.getString("errorMessage");

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(NOTIFICATION_ID, notificationId)
                .add(CASE_ID, caseId.toString())
                .add("failedTime", failedTime)
                .add("errorMessage", errorMessage);

        if (payload.containsKey(STATUS_CODE)) {
            final int statusCode = payload.getInt(STATUS_CODE);
            jsonObjectBuilder.add(STATUS_CODE, statusCode);
        }

        final JsonObject notificationFailedPayload = jsonObjectBuilder.build();

        sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.record-print-request-failure")
                .apply(notificationFailedPayload));
    }

    public void recordPrintRequestSuccess(final JsonEnvelope sourceEnvelope, final UUID caseId) {
        final JsonObject payload = sourceEnvelope.payloadAsJsonObject();
        final String notificationId = payload.getString(NOTIFICATION_ID);
        final String sentTime = payload.getString("sentTime");

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(NOTIFICATION_ID, notificationId)
                .add(CASE_ID, caseId.toString())
                .add("sentTime", sentTime);

        final JsonObject notificationSucceededPayload = jsonObjectBuilder.build();

        sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.record-print-request-success")
                .apply(notificationSucceededPayload));
    }

    public void recordPrintRequestAccepted(final JsonEnvelope sourceEnvelope) {

        final JsonObject payload = sourceEnvelope.payloadAsJsonObject();
        final String notificationId = payload.getString(NOTIFICATION_ID);
        final String caseId = payload.getString(CASE_ID);
        final ZonedDateTime acceptedTime = clock.now();

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(NOTIFICATION_ID, notificationId)
                .add(CASE_ID, caseId)
                .add("acceptedTime", ZonedDateTimes.toString(acceptedTime));

        final JsonObject notificationSucceededPayload = jsonObjectBuilder.build();

        sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.record-print-request-accepted")
                .apply(notificationSucceededPayload));
    }
}
