package uk.gov.moj.cpp.progression.transformer;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.transformer.ApplicationHelper.transformApplications;
import static uk.gov.moj.cpp.progression.transformer.ProsecutionCaseHelper.transformProsecutionCases;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.APPLICANT_COUNSELS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.APPLICATION_PARTY_COUNSELS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.APPROVALS_REQUESTED;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COMPANY_REPRESENTATIVES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_APPLICATIONS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_APPLICATION_PARTY_ATTENDANCE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_CENTRE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.CRACKED_INEFFECTIVE_TRIAL;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.DEFENCE_COUNSELS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.DEFENDANT_ATTENDANCE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.DEFENDANT_HEARING_YOUTH_MARKERS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.DEFENDANT_JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.DEFENDANT_REFERRAL_REASONS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.HAS_SHARED_RESULTS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.HEARING_CASE_NOTES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.HEARING_DAYS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.HEARING_LANGUAGE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.ID;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.IS_BOX_HEARING;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.IS_EFFECTIVE_TRIAL;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.IS_VACATED_TRIAL;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.JUDICIAL_RESULT;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.JUDICIARY;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.JURISDICTION_TYPE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTION_CASES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTION_COUNSELS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PUBLISHED_FOR_NOWS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.REPORTING_RESTRICTION_REASON;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.RESPONDENT_COUNSELS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.SEEDING_HEARING;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.TYPE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.YOUTH_COURT;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.YOUTH_COURT_DEFENDANT_IDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S3776"})
public class HearingHelper {

    private HearingHelper() {}

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingHelper.class);

    public static JsonObject transformedHearing(final JsonObject hearingPayload) {

        LOGGER.info("Transforming Hearing");
        final JsonObject hearing = hearingPayload.getJsonObject("hearing");
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(ID, hearing.getString(ID))
                .add(JURISDICTION_TYPE, hearing.getString(JURISDICTION_TYPE))
                .add(COURT_CENTRE, hearing.getJsonObject(COURT_CENTRE))
                .add(TYPE, hearing.getJsonObject(TYPE));

        if (hearing.containsKey(HEARING_DAYS)) {
            transformedPayloadObjectBuilder.add(HEARING_DAYS, hearing.getJsonArray(HEARING_DAYS));
        }

        if (hearing.containsKey(REPORTING_RESTRICTION_REASON)) {
            transformedPayloadObjectBuilder.add(REPORTING_RESTRICTION_REASON, hearing.getString(REPORTING_RESTRICTION_REASON));
        }

        if (hearing.containsKey(HEARING_LANGUAGE)) {
            transformedPayloadObjectBuilder.add(HEARING_LANGUAGE, hearing.getString(HEARING_LANGUAGE));
        }

        if (hearing.containsKey(DEFENDANT_JUDICIAL_RESULTS)) {
            transformedPayloadObjectBuilder.add(DEFENDANT_JUDICIAL_RESULTS, filterDefendantJudicialResults(hearing.getJsonArray(DEFENDANT_JUDICIAL_RESULTS)));
        }

        if (hearing.containsKey(PROSECUTION_CASES)) {
            transformedPayloadObjectBuilder.add(PROSECUTION_CASES, transformProsecutionCases(hearing.getJsonArray(PROSECUTION_CASES)));
        }

        if (hearing.containsKey(HAS_SHARED_RESULTS)) {
            transformedPayloadObjectBuilder.add(HAS_SHARED_RESULTS, hearing.getBoolean(HAS_SHARED_RESULTS));
        }

        if (hearing.containsKey(COURT_APPLICATIONS)) {
            transformedPayloadObjectBuilder.add(COURT_APPLICATIONS, transformApplications(hearing.getJsonArray(COURT_APPLICATIONS)));
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
            transformedPayloadObjectBuilder.add(CRACKED_INEFFECTIVE_TRIAL, hearing.getJsonObject(CRACKED_INEFFECTIVE_TRIAL));
        }

        if (hearing.containsKey(IS_BOX_HEARING)) {
            transformedPayloadObjectBuilder.add(IS_BOX_HEARING, hearing.getBoolean(IS_BOX_HEARING));
        }

        if (hearing.containsKey(YOUTH_COURT)) {
            transformedPayloadObjectBuilder.add(YOUTH_COURT, hearing.getJsonObject(YOUTH_COURT));
        }
        if (hearing.containsKey(YOUTH_COURT_DEFENDANT_IDS)) {
            transformedPayloadObjectBuilder.add(YOUTH_COURT_DEFENDANT_IDS, hearing.getJsonArray(YOUTH_COURT_DEFENDANT_IDS));
        }

        if (hearing.containsKey(DEFENDANT_HEARING_YOUTH_MARKERS)) {
            transformedPayloadObjectBuilder.add(DEFENDANT_HEARING_YOUTH_MARKERS, hearing.getJsonArray(DEFENDANT_HEARING_YOUTH_MARKERS));
        }

        if (hearing.containsKey(IS_EFFECTIVE_TRIAL)) {
            transformedPayloadObjectBuilder.add(IS_EFFECTIVE_TRIAL, hearing.getBoolean(IS_EFFECTIVE_TRIAL));
        }

        if (hearing.containsKey(COMPANY_REPRESENTATIVES)) {
            transformedPayloadObjectBuilder.add(COMPANY_REPRESENTATIVES, hearing.getJsonArray(COMPANY_REPRESENTATIVES));
        }

        if (hearing.containsKey(IS_VACATED_TRIAL)) {
            transformedPayloadObjectBuilder.add(IS_VACATED_TRIAL, hearing.getBoolean(IS_VACATED_TRIAL));
        }

        if (hearing.containsKey(APPROVALS_REQUESTED)) {
            transformedPayloadObjectBuilder.add(APPROVALS_REQUESTED, hearing.getJsonArray(APPROVALS_REQUESTED));
        }

        if (hearing.containsKey(SEEDING_HEARING)) {
            transformedPayloadObjectBuilder.add(SEEDING_HEARING, hearing.getJsonObject(SEEDING_HEARING));
        }

        return createObjectBuilder().add("hearing", transformedPayloadObjectBuilder.build()).build();
    }

    private static JsonArray filterDefendantJudicialResults(final JsonArray judicialResults) {
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();
        final List<JsonObject> filteredResults = judicialResults.getValuesAs(JsonObject.class).stream().filter(jr -> !jr.getJsonObject(JUDICIAL_RESULT).getBoolean(PUBLISHED_FOR_NOWS))
                .collect(Collectors.toList());

        filteredResults.forEach(jsonArrayBuilder::add);
        return jsonArrayBuilder.build();
    }

    public static JsonArray filterJudicialResults(final JsonArray judicialResults) {
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();
        final List<JsonObject> filteredResults = judicialResults.getValuesAs(JsonObject.class).stream().filter(jr -> !jr.getBoolean(PUBLISHED_FOR_NOWS))
                .collect(Collectors.toList());

        filteredResults.forEach(jsonArrayBuilder::add);
        return jsonArrayBuilder.build();
    }
}
