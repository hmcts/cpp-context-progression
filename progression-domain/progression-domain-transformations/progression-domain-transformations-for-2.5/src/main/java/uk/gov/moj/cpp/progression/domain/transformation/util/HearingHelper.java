package uk.gov.moj.cpp.progression.domain.transformation.util;


import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.ProsecutionCaseHelper.transformProsecutionCases;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@SuppressWarnings({"squid:MethodCyclomaticComplexity","squid:S1188", "squid:S3776"})
public class HearingHelper {

    private HearingHelper() {
    }

    public static JsonObject transformHearing(final JsonObject hearing) {

        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(CommonHelper.COURT_CENTRE, hearing.getJsonObject(CommonHelper.COURT_CENTRE))
                .add(CommonHelper.HEARING_DAYS, hearing.getJsonArray(CommonHelper.HEARING_DAYS))
                .add(CommonHelper.ID, hearing.getString(CommonHelper.ID))
                .add(CommonHelper.JURISDICTION_TYPE, hearing.getString(CommonHelper.JURISDICTION_TYPE))
                .add(CommonHelper.TYPE, hearing.getJsonObject(CommonHelper.TYPE));

        if (hearing.containsKey(CommonHelper.DEFENDANT_REFERRAL_REASONS)) {
            transformedPayloadObjectBuilder.add(CommonHelper.DEFENDANT_REFERRAL_REASONS, hearing.getJsonArray(CommonHelper.DEFENDANT_REFERRAL_REASONS));
        }

        if (hearing.containsKey(CommonHelper.HEARING_LANGUAGE)) {
            transformedPayloadObjectBuilder.add(CommonHelper.HEARING_LANGUAGE, hearing.getString(CommonHelper.HEARING_LANGUAGE));
        }

        if (hearing.containsKey(CommonHelper.JUDICIARY)) {
            transformedPayloadObjectBuilder.add(CommonHelper.JUDICIARY, hearing.getJsonArray(CommonHelper.JUDICIARY));
        }

        if (hearing.containsKey(CommonHelper.PROSECUTION_CASES)) {
            transformedPayloadObjectBuilder.add(CommonHelper.PROSECUTION_CASES, transformProsecutionCases(hearing.getJsonArray(CommonHelper.PROSECUTION_CASES), hearing));
        }

        if (hearing.containsKey(CommonHelper.REPORTING_RESTRICTION_REASON)) {
            transformedPayloadObjectBuilder.add(CommonHelper.REPORTING_RESTRICTION_REASON, hearing.getString(CommonHelper.REPORTING_RESTRICTION_REASON));
        }

        if (hearing.containsKey(CommonHelper.HAS_SHARED_RESULTS)) {
            transformedPayloadObjectBuilder.add(CommonHelper.HAS_SHARED_RESULTS, hearing.getBoolean(CommonHelper.HAS_SHARED_RESULTS));
        }

        if (hearing.containsKey(CommonHelper.COURT_APPLICATIONS)) {
            transformedPayloadObjectBuilder.add(CommonHelper.COURT_APPLICATIONS, hearing.getJsonArray(CommonHelper.COURT_APPLICATIONS));
        }

        if (hearing.containsKey(CommonHelper.HEARING_CASE_NOTES)) {
            transformedPayloadObjectBuilder.add(CommonHelper.HEARING_CASE_NOTES, transformHearingCaseNotes(hearing.getJsonArray(CommonHelper.HEARING_CASE_NOTES)));
        }

        if (hearing.containsKey(CommonHelper.PROSECUTION_COUNSELS)) {
            transformedPayloadObjectBuilder.add(CommonHelper.PROSECUTION_COUNSELS, hearing.getJsonArray(CommonHelper.PROSECUTION_COUNSELS));
        }

        if (hearing.containsKey(CommonHelper.DEFENCE_COUNSELS)) {
            transformedPayloadObjectBuilder.add(CommonHelper.DEFENCE_COUNSELS, hearing.getJsonArray(CommonHelper.DEFENCE_COUNSELS));
        }

        if (hearing.containsKey(CommonHelper.APPLICATION_PARTY_COUNSELS)) {
            transformedPayloadObjectBuilder.add(CommonHelper.APPLICATION_PARTY_COUNSELS, hearing.getJsonArray(CommonHelper.APPLICATION_PARTY_COUNSELS));
        }

        if (hearing.containsKey(CommonHelper.DEFENDANT_ATTENDANCE)) {
            transformedPayloadObjectBuilder.add(CommonHelper.DEFENDANT_ATTENDANCE, hearing.getJsonArray(CommonHelper.DEFENDANT_ATTENDANCE));
        }

        if (hearing.containsKey(CommonHelper.COURT_APPLICATION_PARTY_ATTENDANCE)) {
            transformedPayloadObjectBuilder.add(CommonHelper.COURT_APPLICATION_PARTY_ATTENDANCE, hearing.getJsonArray(CommonHelper.COURT_APPLICATION_PARTY_ATTENDANCE));
        }

        if (hearing.containsKey(CommonHelper.COURT_APPLICATIONS)) {
            transformedPayloadObjectBuilder.add(CommonHelper.COURT_APPLICATIONS, hearing.getJsonArray(CommonHelper.COURT_APPLICATIONS));
        }

        if (hearing.containsKey(CommonHelper.CRACKED_INEFFECTIVE_TRIAL)) {
            transformedPayloadObjectBuilder.add(CommonHelper.COURT_APPLICATIONS, hearing.getJsonObject(CommonHelper.COURT_APPLICATIONS));
        }

        return transformedPayloadObjectBuilder.build();
    }

    private static JsonArray transformHearingCaseNotes(final JsonArray existingHearingCaseNotes) {
        final JsonArrayBuilder newHearingCaseNotes = createArrayBuilder();
        existingHearingCaseNotes.forEach(o -> {
            final JsonObject hearingCaseNote = (JsonObject) o;
            final JsonObject existingCourtClerk = hearingCaseNote.getJsonObject("courtClerk");

            final JsonObject newCourtClerk = CommonHelper.transformDelegatePowers(existingCourtClerk);

            final JsonObject newCaseNote = createObjectBuilder()
                    .add(CommonHelper.ORIGINATING_HEARING_ID, hearingCaseNote.getString(CommonHelper.ORIGINATING_HEARING_ID))
                    .add(CommonHelper.ID, hearingCaseNote.getString(CommonHelper.ID))
                    .add(CommonHelper.NOTE_DATE_TIME, hearingCaseNote.getString(CommonHelper.NOTE_DATE_TIME))
                    .add(CommonHelper.NOTE_TYPE, hearingCaseNote.getString(CommonHelper.NOTE_TYPE))
                    .add(CommonHelper.NOTE, hearingCaseNote.getString(CommonHelper.NOTE))
                    .add(CommonHelper.COURT_CLERK, newCourtClerk)
                    .add(CommonHelper.PROSECUTION_CASES, hearingCaseNote.getJsonArray(CommonHelper.PROSECUTION_CASES)
                    ).build();
            newHearingCaseNotes.add(newCaseNote);
        });
        return newHearingCaseNotes.build();
    }
}
