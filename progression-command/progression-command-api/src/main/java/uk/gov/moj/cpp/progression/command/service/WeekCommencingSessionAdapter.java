package uk.gov.moj.cpp.progression.command.service;

import static java.util.Optional.empty;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.progression.listing.common.service.CourtSchedulesByIdService;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeekCommencingSessionAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeekCommencingSessionAdapter.class);
    private static final String COURT_SCHEDULES = "courtSchedules";
    private static final String IS_WEEK_COMMENCING = "isWeekCommencing";
    private static final String SESSION_DATE = "sessionDate";
    private static final String START_DATE = "startDate";
    private static final String DURATION = "duration";
    private static final int WEEK_COMMENCING_DURATION_IN_WEEKS = 1;

    @Inject
    private CourtSchedulesByIdService courtSchedulesByIdService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public Optional<JsonObject> weekCommencingDateOf(final List<String> courtScheduleIds) {
        if (courtScheduleIds.isEmpty()) {
            return empty();
        }

        final Response response = courtSchedulesByIdService.getCourtSchedulesById(courtScheduleIds);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            final String responsePayload = response.hasEntity() ? response.getEntity().toString() : "";
            throw new ServerErrorException("Court schedules by id endpoint returned an error: " + responsePayload, response.getStatus());
        }

        final JsonObject responseJson = objectToJsonObjectConverter.convert(response.getEntity());
        final JsonArray courtSchedules = responseJson.containsKey(COURT_SCHEDULES) && !responseJson.isNull(COURT_SCHEDULES)
                ? responseJson.getJsonArray(COURT_SCHEDULES)
                : JsonValue.EMPTY_JSON_ARRAY;
        if (courtSchedules.isEmpty()) {
            LOGGER.warn("No court schedules found for ids {}, week commencing date not added", courtScheduleIds);
            return empty();
        }

        return courtSchedules.getValuesAs(JsonObject.class).stream()
                .filter(courtSchedule -> courtSchedule.getBoolean(IS_WEEK_COMMENCING, false))
                .filter(courtSchedule -> courtSchedule.containsKey(SESSION_DATE) && !courtSchedule.isNull(SESSION_DATE))
                .findFirst()
                .map(courtSchedule -> createObjectBuilder()
                        .add(START_DATE, courtSchedule.getString(SESSION_DATE))
                        .add(DURATION, WEEK_COMMENCING_DURATION_IN_WEEKS)
                        .build());
    }
}
