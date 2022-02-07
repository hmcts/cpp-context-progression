package uk.gov.moj.cpp.progression.query.utils;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.progression.courts.Hearings;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

public class CaseHearingsQueryHelper {

    public static final String HEARINGS = "hearings";
    public static final String HEARING_ID = "hearingId";
    public static final String COURT_CENTRE = "courtCentre";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String ROOM_ID = "roomId";
    public static final String ROOM_NAME = "roomName";
    public static final String HEARING_TYPES = "hearingTypes";

    private CaseHearingsQueryHelper() {
    }

    public static JsonObject buildCaseHearingsResponse(final List<Hearings> hearings) {
        final JsonArrayBuilder hearingsBuilder = createArrayBuilder();

        hearings.forEach(hearing -> {
            final JsonObject hearingJsonObject = buildHearing(hearing);
            hearingsBuilder.add(hearingJsonObject);
        });

        return createObjectBuilder().add(HEARINGS, hearingsBuilder).build();
    }

    private static JsonObject buildHearing(final Hearings hearing) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(HEARING_ID, hearing.getId().toString());

        if (nonNull(hearing.getCourtCentre())) {
            builder.add(COURT_CENTRE, buildCourtCentre(hearing.getCourtCentre()));
        }

        return builder.build();
    }

    private static JsonObject buildCourtCentre(final CourtCentre courtCentre) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(ID, courtCentre.getId().toString())
                .add(NAME, courtCentre.getName());

        if (nonNull(courtCentre.getRoomId())) {
            builder.add(ROOM_ID, courtCentre.getRoomId().toString());
            builder.add(ROOM_NAME, courtCentre.getRoomName());
        }

        return builder.build();
    }

    public static JsonObject buildCaseHearingTypesResponse(final Map<UUID, String> hearingTypes) {
        final JsonArrayBuilder hearingTypesBuilder = createArrayBuilder();
        hearingTypes.entrySet().stream().forEach(hearingType ->
                hearingTypesBuilder.add(createObjectBuilder().add("hearingId", String.valueOf(hearingType.getKey())).add("type", hearingType.getValue()).build()));
        return createObjectBuilder().add(HEARING_TYPES, hearingTypesBuilder).build();
    }
}
