package uk.gov.moj.cpp.progression.query.utils;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.progression.courts.Hearings;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class CaseHearingsQueryHelper {

    public static final String HEARINGS = "hearings";
    public static final String HEARING_ID = "hearingId";
    public static final String COURT_CENTRE = "courtCentre";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String ROOM_ID = "roomId";
    public static final String ROOM_NAME = "roomName";
    public static final String HEARING_TYPES = "hearingTypes";
    public static final String CASE_ID = "caseId";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String HEARING_LISTING_STATUS = "hearingListingStatus";
    public static final String COURT_CENTRE_ID = "courtCentreId";
    public static final String HEARING_DAYS = "hearingDays";
    public static final String SITTING_DAY = "sittingDay";
    public static final DateTimeFormatter ZONE_DATETIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final String JURISDICTION_TYPE = "jurisdictionType";

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

    public static JsonObject buildCaseDefendantHearingsResponse(final List<Hearings> hearings, final UUID caseId, final UUID defendantId) {
        final JsonArrayBuilder hearingsBuilder = createArrayBuilder();

        hearings.forEach(hearing -> {
            final JsonObject hearingJsonObject = buildHearingWithHearingDays(hearing);
            hearingsBuilder.add(hearingJsonObject);
        });

        return createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(DEFENDANT_ID, defendantId.toString())
                .add(HEARINGS, hearingsBuilder).build();
    }

    private static JsonObject buildHearing(final Hearings hearing) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(HEARING_ID, hearing.getId().toString())
                .add(ID, hearing.getId().toString())
                .add(JURISDICTION_TYPE, hearing.getJurisdictionType().toString());

        if (nonNull(hearing.getCourtCentre())) {
            builder.add(COURT_CENTRE, buildCourtCentre(hearing.getCourtCentre()));
        }

        if (nonNull(hearing.getHearingDays())) {
            builder.add(HEARING_DAYS, buildHearingDays(hearing.getHearingDays()));
        }

        return builder.build();
    }

    private static JsonObject buildHearingWithHearingDays(final Hearings hearing) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(HEARING_ID, hearing.getId().toString());

        ofNullable(hearing.getHearingListingStatus())
                .ifPresent(status -> builder.add(HEARING_LISTING_STATUS, hearing.getHearingListingStatus().name()));

        ofNullable(hearing.getCourtCentre())
                .ifPresent(cc -> builder.add(COURT_CENTRE, buildCourtCentre(cc)));

        ofNullable(hearing.getHearingDays())
                .filter(hdays -> !hdays.isEmpty())
                .ifPresent(hdays -> builder.add(HEARING_DAYS, buildHearingDays(hdays)));

        return builder.build();
    }

    private static JsonArray buildHearingDays(final List<HearingDay> hdays) {
        final JsonArrayBuilder hearingDaysArrayBuilder = createArrayBuilder();

        hdays.forEach(hd -> {
            final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
            ofNullable(hd.getCourtCentreId())
                    .ifPresent(ccId -> jsonObjectBuilder.add(COURT_CENTRE_ID, ccId.toString()));

            ofNullable(hd.getCourtRoomId())
                    .ifPresent(rId -> jsonObjectBuilder.add(ROOM_ID, rId.toString()));

            ofNullable(hd.getSittingDay())
                    .ifPresent(sDay -> jsonObjectBuilder.add(SITTING_DAY, sDay.format(ZONE_DATETIME_FORMATTER)));

            hearingDaysArrayBuilder.add(jsonObjectBuilder.build());
        });
        return hearingDaysArrayBuilder.build();
    }

    private static JsonObject buildCourtCentre(final CourtCentre courtCentre) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(ID, courtCentre.getId().toString())
                .add(NAME, courtCentre.getName());

        if (nonNull(courtCentre.getRoomId())) {
            builder.add(ROOM_ID, courtCentre.getRoomId().toString());
            if (nonNull(courtCentre.getRoomName())) {
                builder.add(ROOM_NAME, courtCentre.getRoomName());
            }
        }
        return builder.build();
    }
    public static JsonObject buildCaseHearingTypesResponse(final Map<UUID, String> hearingTypes) {
        final JsonArrayBuilder hearingTypesBuilder = createArrayBuilder();
        hearingTypes.entrySet().stream().forEach(hearingType ->
                hearingTypesBuilder.add(createObjectBuilder().add(HEARING_ID, String.valueOf(hearingType.getKey())).add("type", hearingType.getValue()).build()));
        return createObjectBuilder().add(HEARING_TYPES, hearingTypesBuilder).build();
    }
}
