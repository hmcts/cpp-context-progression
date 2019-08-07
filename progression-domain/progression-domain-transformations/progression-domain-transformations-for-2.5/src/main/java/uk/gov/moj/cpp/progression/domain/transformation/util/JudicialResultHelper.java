package uk.gov.moj.cpp.progression.domain.transformation.util;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

@SuppressWarnings({"squid:S1188", "squid:S3776"})
public class JudicialResultHelper {

    private JudicialResultHelper() {
    }

    public static JsonArray transformJudicialResults(final JsonArray judicialResults,
                                                     final JsonObject hearing) {

        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();
        judicialResults.forEach(o -> {
            final JsonObject result = (JsonObject) o;
            final JsonObjectBuilder transformResultBuilder = createObjectBuilder()
                    .add(CommonHelper.JUDICIAL_RESULT_ID, result.getJsonString(CommonHelper.ID))
                    .add(CommonHelper.ORDERED_HEARING_ID, hearing.getJsonString(CommonHelper.ID))
                    .add(CommonHelper.LABEL, result.getJsonString(CommonHelper.LABEL))
                    .add(CommonHelper.IS_ADJOURNMENT_RESULT, Boolean.FALSE)
                    .add(CommonHelper.IS_FINANCIAL_RESULT, Boolean.FALSE)
                    .add(CommonHelper.IS_CONVICTED_RESULT, Boolean.FALSE)
                    .add(CommonHelper.IS_AVAILABLE_FOR_COURT_EXTRACT, Boolean.FALSE)
                    .add(CommonHelper.ORDERED_DATE, result.getJsonString(CommonHelper.ORDERED_DATE));

            if(result.containsKey(CommonHelper.CATEGORY)) {
                transformResultBuilder.add(CommonHelper.CATEGORY, result.getJsonString(CommonHelper.CATEGORY));
            }
            if (result.containsKey(CommonHelper.WELSH_LABEL)) {
                transformResultBuilder.add(CommonHelper.WELSH_LABEL, result.getString(CommonHelper.WELSH_LABEL));
            }

            if (result.containsKey(CommonHelper.C_JS_CODE)) {
                transformResultBuilder.add(CommonHelper.C_JS_CODE, result.getBoolean(CommonHelper.C_JS_CODE));
            }
            if (result.containsKey(CommonHelper.RANK)) {
                transformResultBuilder.add(CommonHelper.RANK, result.getJsonNumber(CommonHelper.RANK));
            }
            if (result.containsKey(CommonHelper.LAST_SHARED_DATE_TIME)) {
                transformResultBuilder.add(CommonHelper.LAST_SHARED_DATE_TIME, result.getJsonString(CommonHelper.LAST_SHARED_DATE_TIME));
            }
            if (result.containsKey(CommonHelper.COURT_CLERK)) {
                transformResultBuilder.add(CommonHelper.COURT_CLERK, CommonHelper.transformDelegatePowers(result.getJsonObject(CommonHelper.COURT_CLERK)));
            }
            if (result.containsKey(CommonHelper.USERGROUPS)) {
                transformResultBuilder.add(CommonHelper.USERGROUPS, result.getJsonObject(CommonHelper.USERGROUPS));
            }
            if (result.containsKey(CommonHelper.CATEGORY)) {
                transformResultBuilder.add(CommonHelper.CATEGORY, result.getJsonString(CommonHelper.CATEGORY));
            }
            if (result.containsKey(CommonHelper.JUDICIAL_RESULT_PROMPTS)) {
                transformResultBuilder.add(CommonHelper.JUDICIAL_RESULT_PROMPTS, result.getJsonObject(CommonHelper.JUDICIAL_RESULT_PROMPTS));
            }

            transformedPayloadObjectBuilder.add(transformResultBuilder);
        });
        return transformedPayloadObjectBuilder.build();
    }
}