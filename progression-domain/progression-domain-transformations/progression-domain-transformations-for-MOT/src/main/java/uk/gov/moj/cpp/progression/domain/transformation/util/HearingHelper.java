package uk.gov.moj.cpp.progression.domain.transformation.util;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICANT_COUNSELS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_PARTY_COUNSELS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.COURT_APPLICATIONS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.COURT_APPLICATION_PARTY_ATTENDANCE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.COURT_CENTRE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.CRACKED_INEFFECTIVE_TRIAL;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DEFENCE_COUNSELS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DEFENDANT_ATTENDANCE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DEFENDANT_REFERRAL_REASONS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.HAS_SHARED_RESULTS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.HEARING_CASE_NOTES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.HEARING_DAYS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.HEARING_LANGUAGE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.IS_BOX_HEARING;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.JUDICIARY;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.JURISDICTION_TYPE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTION_CASES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTION_COUNSELS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.REPORTING_RESTRICTION_REASON;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.RESPONDENT_COUNSELS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.TYPE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.ProsecutionCaseHelper.transformProsecutionCases;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S1188", "squid:S3776"})
public class HearingHelper {

    private HearingHelper() {
    }

    public static JsonObject transformHearing(final JsonObject hearing) {

        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(ID, hearing.getString(ID))
                .add(JURISDICTION_TYPE, hearing.getString(JURISDICTION_TYPE))
                .add(COURT_CENTRE, hearing.getJsonObject(COURT_CENTRE))
                .add(HEARING_DAYS, hearing.getJsonArray(HEARING_DAYS))
                .add(TYPE, hearing.getJsonObject(TYPE));


        if (hearing.containsKey(REPORTING_RESTRICTION_REASON)) {
            transformedPayloadObjectBuilder.add(REPORTING_RESTRICTION_REASON, hearing.getString(REPORTING_RESTRICTION_REASON));
        }

        if (hearing.containsKey(HEARING_LANGUAGE)) {
            transformedPayloadObjectBuilder.add(HEARING_LANGUAGE, hearing.getString(HEARING_LANGUAGE));
        }

        if (hearing.containsKey(PROSECUTION_CASES)) {
            transformedPayloadObjectBuilder.add(PROSECUTION_CASES, transformProsecutionCases(hearing.getJsonArray(PROSECUTION_CASES), hearing));
        }

        if (hearing.containsKey(HAS_SHARED_RESULTS)) {
            transformedPayloadObjectBuilder.add(HAS_SHARED_RESULTS, hearing.getBoolean(HAS_SHARED_RESULTS));
        }


        if (hearing.containsKey(COURT_APPLICATIONS)) {
            transformedPayloadObjectBuilder.add(COURT_APPLICATIONS, hearing.getJsonArray(COURT_APPLICATIONS));
        }

        if (hearing.containsKey(DEFENDANT_REFERRAL_REASONS)) {
            transformedPayloadObjectBuilder.add(DEFENDANT_REFERRAL_REASONS, hearing.getJsonArray(DEFENDANT_REFERRAL_REASONS));
        }

        if (hearing.containsKey(HEARING_CASE_NOTES)) {
            transformedPayloadObjectBuilder.add(HEARING_CASE_NOTES, hearing.getJsonArray(HEARING_CASE_NOTES));
        }

        if (hearing.containsKey(JUDICIARY)) {
            transformedPayloadObjectBuilder.add(JUDICIARY, hearing.getJsonArray(JUDICIARY));
        }

        if (hearing.containsKey(APPLICANT_COUNSELS)) {
            transformedPayloadObjectBuilder.add(APPLICANT_COUNSELS, hearing.getJsonArray(APPLICANT_COUNSELS));
        }

        if (hearing.containsKey(RESPONDENT_COUNSELS)) {
            transformedPayloadObjectBuilder.add(RESPONDENT_COUNSELS, hearing.getJsonArray(RESPONDENT_COUNSELS));
        }

        if (hearing.containsKey(PROSECUTION_COUNSELS)) {
            transformedPayloadObjectBuilder.add(PROSECUTION_COUNSELS, hearing.getJsonArray(PROSECUTION_COUNSELS));
        }

        if (hearing.containsKey(DEFENCE_COUNSELS)) {
            transformedPayloadObjectBuilder.add(DEFENCE_COUNSELS, hearing.getJsonArray(DEFENCE_COUNSELS));
        }

        if (hearing.containsKey(APPLICATION_PARTY_COUNSELS)) {
            transformedPayloadObjectBuilder.add(APPLICATION_PARTY_COUNSELS, hearing.getJsonArray(APPLICATION_PARTY_COUNSELS));
        }

        if (hearing.containsKey(DEFENDANT_ATTENDANCE)) {
            transformedPayloadObjectBuilder.add(DEFENDANT_ATTENDANCE, hearing.getJsonArray(DEFENDANT_ATTENDANCE));
        }

        if (hearing.containsKey(COURT_APPLICATION_PARTY_ATTENDANCE)) {
            transformedPayloadObjectBuilder.add(COURT_APPLICATION_PARTY_ATTENDANCE, hearing.getJsonArray(COURT_APPLICATION_PARTY_ATTENDANCE));
        }

        if (hearing.containsKey(CRACKED_INEFFECTIVE_TRIAL)) {
            transformedPayloadObjectBuilder.add(COURT_APPLICATIONS, hearing.getJsonObject(COURT_APPLICATIONS));
        }

        if (hearing.containsKey(IS_BOX_HEARING)) {
            transformedPayloadObjectBuilder.add(IS_BOX_HEARING, hearing.getBoolean(IS_BOX_HEARING));
        }

        return transformedPayloadObjectBuilder.build();
    }

}
