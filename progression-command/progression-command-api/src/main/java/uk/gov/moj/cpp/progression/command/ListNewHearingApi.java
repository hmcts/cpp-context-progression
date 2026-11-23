package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.service.WeekCommencingSessionAdapter;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class ListNewHearingApi {

    private static final String LIST_NEW_HEARING = "listNewHearing";
    private static final String JURISDICTION_TYPE = "jurisdictionType";
    private static final String CROWN = "CROWN";
    private static final String WEEK_COMMENCING_DATE = "weekCommencingDate";
    private static final String BOOKED_SLOTS = "bookedSlots";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";

    @Inject
    private Sender sender;

    @Inject
    private WeekCommencingSessionAdapter weekCommencingSessionAdapter;

    @Handles("progression.list-new-hearing")
    public void handle(final JsonEnvelope envelope) {
        sender.send(envelop(withWeekCommencingDateOfBookedSession(envelope.payloadAsJsonObject()))
                        .withName("progression.command.list-new-hearing")
                        .withMetadataFrom(envelope));
    }

    private JsonObject withWeekCommencingDateOfBookedSession(final JsonObject payload) {
        final JsonObject listNewHearing = payload.getJsonObject(LIST_NEW_HEARING);
        if (!CROWN.equals(listNewHearing.getString(JURISDICTION_TYPE, null)) || hasValue(listNewHearing, WEEK_COMMENCING_DATE)) {
            return payload;
        }
        return weekCommencingSessionAdapter.weekCommencingDateOf(bookedCourtScheduleIds(listNewHearing))
                .map(weekCommencingDate -> createObjectBuilder(payload)
                        .add(LIST_NEW_HEARING, createObjectBuilder(listNewHearing).add(WEEK_COMMENCING_DATE, weekCommencingDate))
                        .build())
                .orElse(payload);
    }

    private List<String> bookedCourtScheduleIds(final JsonObject listNewHearing) {
        if (!hasValue(listNewHearing, BOOKED_SLOTS)) {
            return List.of();
        }
        return listNewHearing.getJsonArray(BOOKED_SLOTS).getValuesAs(JsonObject.class).stream()
                .map(bookedSlot -> bookedSlot.getString(COURT_SCHEDULE_ID, ""))
                .filter(courtScheduleId -> !courtScheduleId.isBlank())
                .toList();
    }

    private static boolean hasValue(final JsonObject json, final String key) {
        return json.containsKey(key) && !json.isNull(key);
    }
}
