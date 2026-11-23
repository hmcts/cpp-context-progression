package uk.gov.moj.cpp.progression.command.service;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.progression.listing.common.service.CourtScheduleService;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeekCommencingSessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeekCommencingSessionService.class);
    private static final String COURT_SCHEDULES = "courtSchedules";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    private static final String IS_WEEK_COMMENCING = "isWeekCommencing";

    @Inject
    private CourtScheduleService courtScheduleService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public List<String> getWeekCommencingSessionIds(final List<String> courtScheduleIds) {
        if (courtScheduleIds.isEmpty()) {
            return List.of();
        }

        final List<JsonObject> courtSchedules = getCourtSchedulesByIds(courtScheduleIds);
        if (courtSchedules.size() < courtScheduleIds.stream().distinct().count()) {
            LOGGER.warn("Court scheduler returned {} court schedules for ids {}", courtSchedules.size(), courtScheduleIds);
        }

        return courtSchedules.stream()
                .filter(courtSchedule -> courtSchedule.getBoolean(IS_WEEK_COMMENCING, false))
                .map(courtSchedule -> courtSchedule.getString(COURT_SCHEDULE_ID))
                .toList();
    }

    private List<JsonObject> getCourtSchedulesByIds(final List<String> courtScheduleIds) {
        final Response response = courtScheduleService.getCourtSchedulesById(courtScheduleIds);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new InternalServerErrorException("Court schedules by id endpoint returned an error");
        }

        final JsonObject responseJson = objectToJsonObjectConverter.convert(response.getEntity());
        if (!responseJson.containsKey(COURT_SCHEDULES) || responseJson.isNull(COURT_SCHEDULES)) {
            return List.of();
        }
        return responseJson.getJsonArray(COURT_SCHEDULES).getValuesAs(JsonObject.class);
    }
}
