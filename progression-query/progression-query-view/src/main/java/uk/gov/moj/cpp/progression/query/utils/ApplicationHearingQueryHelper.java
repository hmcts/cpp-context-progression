package uk.gov.moj.cpp.progression.query.utils;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.List;

import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

public class ApplicationHearingQueryHelper {
    public static final String HEARINGS = "hearings";
    public static final String HEARING_ID = "hearingId";
    public static final String COURT_CENTRE = "courtCentre";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String ROOM_ID = "roomId";
    public static final String ROOM_NAME = "roomName";

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

    private static JsonObject buildHearing(final JsonObject hearing){
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(HEARING_ID, hearing.getString(ID));

        final JsonObject courtCentre = hearing.getJsonObject(COURT_CENTRE);
        if (nonNull(courtCentre)) {
            builder.add(COURT_CENTRE, buildCourtCentre(courtCentre));
        }

        return builder.build();
    }

    private static JsonObject buildCourtCentre(final JsonObject courtCentre){
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(ID, courtCentre.getString(ID))
                .add(NAME, courtCentre.getString(NAME));

        if (courtCentre.containsKey(ROOM_ID) && courtCentre.containsKey(ROOM_NAME)) {
            builder.add(ROOM_ID, courtCentre.getString(ROOM_ID));
            builder.add(ROOM_NAME, courtCentre.getString(ROOM_NAME));
        }

        return builder.build();
    }
}
