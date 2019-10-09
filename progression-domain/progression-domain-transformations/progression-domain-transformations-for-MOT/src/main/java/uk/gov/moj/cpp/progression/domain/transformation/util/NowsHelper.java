package uk.gov.moj.cpp.progression.domain.transformation.util;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.COURT_CENTRE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.HEARING_DAYS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.JUDICIARY;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.JURISDICTION_TYPE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTION_CASES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.TYPE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.ProsecutionCaseHelper.transformProsecutionCases;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class NowsHelper {


    public static final String REPORTING_RESTRICTION_REASON = "reportingRestrictionReason";
    public static final String HEARING_LANGUAGE = "hearingLanguage";
    public static final String HAS_SHARED_RESULTS = "hasSharedResults";
    public static final String PROSECUTION_COUNSELS = "prosecutionCounsels";
    public static final String DEFENCE_COUNSELS = "defenceCounsels";
    public static final String DEFENDANT_ATTENDANCE = "defendantAttendance";
    public static final String SHARED_RESULT_LINES = "sharedResultLines";
    public static final String NOWS = "nows";
    public static final String NOW_TYPES = "nowTypes";

    private NowsHelper() {
    }

    public static JsonObject transformNowsHearing(final JsonObject hearing) {
        //Add Mandatory Fields
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add(ID, hearing.getString(ID));
        jsonObjectBuilder.add(TYPE, hearing.getJsonObject(TYPE));
        jsonObjectBuilder.add(JURISDICTION_TYPE, hearing.getString(JURISDICTION_TYPE));
        jsonObjectBuilder.add(PROSECUTION_CASES, transformProsecutionCases(hearing.getJsonArray(PROSECUTION_CASES)));
        jsonObjectBuilder.add(JUDICIARY, hearing.getJsonArray(JUDICIARY));
        jsonObjectBuilder.add(HEARING_DAYS, hearing.getJsonArray(HEARING_DAYS));
        jsonObjectBuilder.add(COURT_CENTRE, hearing.getJsonObject(COURT_CENTRE));

        if (hearing.containsKey(REPORTING_RESTRICTION_REASON)) {
            jsonObjectBuilder.add(REPORTING_RESTRICTION_REASON, hearing.getString(REPORTING_RESTRICTION_REASON));
        }
        if (hearing.containsKey(HEARING_LANGUAGE)) {
            jsonObjectBuilder.add(HEARING_LANGUAGE, hearing.getString(HEARING_LANGUAGE));
        }
        if (hearing.containsKey(HAS_SHARED_RESULTS)) {
            jsonObjectBuilder.add(HAS_SHARED_RESULTS, hearing.getBoolean(HAS_SHARED_RESULTS));
        }
        if (hearing.containsKey(PROSECUTION_COUNSELS)) {
            jsonObjectBuilder.add(PROSECUTION_COUNSELS, hearing.getJsonArray(PROSECUTION_COUNSELS));
        }
        if (hearing.containsKey(DEFENCE_COUNSELS)) {
            jsonObjectBuilder.add(DEFENCE_COUNSELS, hearing.getJsonArray(DEFENCE_COUNSELS));
        }
        if (hearing.containsKey(DEFENDANT_ATTENDANCE)) {
            jsonObjectBuilder.add(DEFENDANT_ATTENDANCE, hearing.getJsonArray(DEFENDANT_ATTENDANCE));
        }
        if (hearing.containsKey(SHARED_RESULT_LINES)) {
            jsonObjectBuilder.add(SHARED_RESULT_LINES, hearing.getJsonArray(SHARED_RESULT_LINES));
        }
        if (hearing.containsKey(NOWS)) {
            jsonObjectBuilder.add(NOWS, hearing.getJsonArray(NOWS));
        }
        if (hearing.containsKey(NOW_TYPES)) {
            jsonObjectBuilder.add(NOW_TYPES, hearing.getJsonArray(NOW_TYPES));
        }
        return jsonObjectBuilder.build();
    }
}
