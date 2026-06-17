package uk.gov.moj.cpp.progression.query.utils;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.query.utils.CaseHearingsQueryHelper.addHearing;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.progression.courts.Hearings;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.collections.CollectionUtils;

public class ApplicationHearingQueryHelper {
    public static final String HEARINGS = "hearings";
    public static final String HEARING_ID = "hearingId";
    public static final String COURT_CENTRE = "courtCentre";
    public static final String COURT_CENTRE_ID = "courtCentreId";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String ROOM_ID = "roomId";
    public static final String ROOM_NAME = "roomName";
    public static final String APPLICATIONS = "applications";
    public static final String TITLE = "title";
    public static final String JURISDICTION_TYPE = "jurisdictionType";
    public static final String HEARING_DAYS = "hearingDays";
    public static final String SITTING_DAY = "sittingDay";

    private ApplicationHearingQueryHelper() {
    }

    public static JsonObject buildApplicationHearingResponse(final List<JsonObject> hearingPayloads) {
        final JsonArrayBuilder hearingsBuilder = createArrayBuilder();

        hearingPayloads.forEach(hearing -> {
            final JsonObject hearingJsonObject = buildHearing(hearing);
            hearingsBuilder.add(hearingJsonObject);
        });

        return createObjectBuilder().add(HEARINGS, hearingsBuilder).build();
    }

    public static JsonArray getApplicationHearingsJson(final Map<CourtApplication, List<Hearings>> applicationHearings) {
        final List<JsonObject> hearingJsonObjects = new ArrayList<>();
        applicationHearings.forEach((courtApplication, hearings) ->
                hearings.stream().filter(h -> isNull(h.getIsBoxHearing()) || !h.getIsBoxHearing())
                        .forEach(hearing -> {
                            final JsonObjectBuilder applicationHearingsJsonBuilder = JsonObjects.createObjectBuilder()
                                    .add(ID, courtApplication.getId().toString())
                                    .add(TITLE, courtApplication.getType().getType());
                            addHearing(applicationHearingsJsonBuilder, hearing);
                            hearingJsonObjects.add(applicationHearingsJsonBuilder.build());
                        })
        );

        hearingJsonObjects.sort(Comparator.comparing(
                obj -> {
                    final JsonArray hearingDays = obj.getJsonArray(HEARING_DAYS);
                    if (CollectionUtils.isEmpty(hearingDays)) {
                        return null;
                    }
                    return hearingDays.stream()
                            .map(JsonValue::asJsonObject)
                            .map(dayObj -> dayObj.getString(SITTING_DAY, null))
                            .filter(Objects::nonNull)
                            .min(String::compareTo)
                            .orElse(null);
                },
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        final JsonArrayBuilder sortedArrayBuilder = JsonObjects.createArrayBuilder();
        hearingJsonObjects.forEach(sortedArrayBuilder::add);

        return sortedArrayBuilder.build();

    }

    public static JsonArray linkedApplicationHearingsResponse(final CourtApplication courtApplication, final List<JsonObject> applicationHearings) {
        final JsonArrayBuilder applicationHearingsBuilder = createArrayBuilder();

        applicationHearings.stream().filter(h -> !h.containsKey("isBoxHearing") || Boolean.FALSE.equals(h.getBoolean("isBoxHearing")))
                .forEach(hearing -> {
                    final JsonObjectBuilder applicationHearingsJson = createObjectBuilder()
                            .add(ID, courtApplication.getId().toString())
                            .add(TITLE, courtApplication.getType().getType())
                            .add(HEARING_ID, hearing.getString(ID));

                    if (hearing.containsKey(JURISDICTION_TYPE)) {
                        applicationHearingsJson.add(JURISDICTION_TYPE, hearing.getString(JURISDICTION_TYPE));
                    }

                    final JsonObject courtCentre = hearing.getJsonObject(COURT_CENTRE);
                    if (nonNull(courtCentre)) {
                        applicationHearingsJson.add(COURT_CENTRE, buildCourtCentre(courtCentre));
                    }

                    if (hearing.containsKey(HEARING_DAYS)) {
                        applicationHearingsJson.add(HEARING_DAYS, buildHearingDays(hearing.getJsonArray(HEARING_DAYS)));
                    }

                    applicationHearingsBuilder.add(applicationHearingsJson);
                });

        return applicationHearingsBuilder.build();
    }

    private static JsonObject buildHearing(final JsonObject hearing) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(HEARING_ID, hearing.getString(ID));

        if (hearing.containsKey(JURISDICTION_TYPE)) {
            builder.add(JURISDICTION_TYPE, hearing.getString(JURISDICTION_TYPE));
        }

        final JsonObject courtCentre = hearing.getJsonObject(COURT_CENTRE);
        if (nonNull(courtCentre)) {
            builder.add(COURT_CENTRE, buildCourtCentre(courtCentre));
        }

        if (hearing.containsKey(HEARING_DAYS)) {
            builder.add(HEARING_DAYS, buildHearingDays(hearing.getJsonArray(HEARING_DAYS)));
        }

        return builder.build();
    }

    private static JsonObject buildCourtCentre(final JsonObject courtCentre) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(ID, courtCentre.getString(ID))
                .add(NAME, courtCentre.getString(NAME));

        if (courtCentre.containsKey(ROOM_ID) && courtCentre.containsKey(ROOM_NAME)) {
            builder.add(ROOM_ID, courtCentre.getString(ROOM_ID));
            builder.add(ROOM_NAME, courtCentre.getString(ROOM_NAME));
        }

        return builder.build();
    }

    private static JsonArray buildHearingDays(final JsonArray hearingDaysJsonArray) {
        final JsonArrayBuilder hearingDaysArrayBuilder = createArrayBuilder();

        hearingDaysJsonArray.stream().map(JsonValue::asJsonObject)
                .forEach(hdJsonObject -> {
                    final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
                    if (hdJsonObject.containsKey(COURT_CENTRE_ID)) {
                        jsonObjectBuilder.add(COURT_CENTRE_ID, hdJsonObject.getString(COURT_CENTRE_ID));
                    }

                    if (hdJsonObject.containsKey(ROOM_ID)) {
                        jsonObjectBuilder.add(ROOM_ID, hdJsonObject.getString(ROOM_ID));
                    }

                    if (hdJsonObject.containsKey(SITTING_DAY)) {
                        jsonObjectBuilder.add(SITTING_DAY, hdJsonObject.getString(SITTING_DAY));
                    }

                    hearingDaysArrayBuilder.add(jsonObjectBuilder.build());
                });
        return hearingDaysArrayBuilder.build();
    }
}
