package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.service.WeekCommencingSessionService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class ListNewHearingApi {

    private static final String LIST_NEW_HEARING = "listNewHearing";
    private static final String JURISDICTION_TYPE = "jurisdictionType";
    private static final String CROWN = "CROWN";
    private static final String WEEK_COMMENCING_DATE = "weekCommencingDate";
    private static final String START_DATE = "startDate";
    private static final String BOOKED_SLOTS = "bookedSlots";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    private static final String START_TIME = "startTime";
    private static final ZoneId UK_TIME_ZONE = ZoneId.of("Europe/London");

    @Inject
    private Sender sender;

    @Inject
    private WeekCommencingSessionService weekCommencingSessionService;

    @Handles("progression.list-new-hearing")
    public void handle(final JsonEnvelope envelope) {
        sender.send(envelop(enrichWithWeekCommencingDate(envelope.payloadAsJsonObject()))
                        .withName("progression.command.list-new-hearing")
                        .withMetadataFrom(envelope));
    }

    private JsonObject enrichWithWeekCommencingDate(final JsonObject payload) {
        final JsonObject listNewHearing = payload.getJsonObject(LIST_NEW_HEARING);
        if (!CROWN.equals(listNewHearing.getString(JURISDICTION_TYPE, null))) {
            return payload;
        }

        final List<JsonObject> bookedSlots = getBookedSlotsWithCourtScheduleId(listNewHearing);
        final List<String> weekCommencingSessionIds = weekCommencingSessionService.getWeekCommencingSessionIds(
                bookedSlots.stream().map(bookedSlot -> bookedSlot.getString(COURT_SCHEDULE_ID)).toList());

        return bookedSlots.stream()
                .filter(bookedSlot -> weekCommencingSessionIds.contains(bookedSlot.getString(COURT_SCHEDULE_ID)))
                .findFirst()
                .map(bookedSlot -> createObjectBuilder(payload)
                        .add(LIST_NEW_HEARING, createObjectBuilder(listNewHearing)
                                .add(WEEK_COMMENCING_DATE, createObjectBuilder()
                                        .add(START_DATE, toUkLocalDate(bookedSlot.getString(START_TIME)).toString())))
                        .build())
                .orElse(payload);
    }

    private static LocalDate toUkLocalDate(final String startTime) {
        return ZonedDateTime.parse(startTime).withZoneSameInstant(UK_TIME_ZONE).toLocalDate();
    }

    private List<JsonObject> getBookedSlotsWithCourtScheduleId(final JsonObject listNewHearing) {
        if (!listNewHearing.containsKey(BOOKED_SLOTS) || listNewHearing.isNull(BOOKED_SLOTS)) {
            return List.of();
        }
        return listNewHearing.getJsonArray(BOOKED_SLOTS).getValuesAs(JsonObject.class).stream()
                .filter(bookedSlot -> !bookedSlot.getString(COURT_SCHEDULE_ID, "").isBlank())
                .toList();
    }
}
