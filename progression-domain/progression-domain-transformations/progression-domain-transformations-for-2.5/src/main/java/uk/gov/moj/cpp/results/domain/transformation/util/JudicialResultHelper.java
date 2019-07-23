package uk.gov.moj.cpp.results.domain.transformation.util;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.CATEGORY;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.COURT_CLERK;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.C_JS_CODE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.IS_ADJOURNMENT_RESULT;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.IS_AVAILABLE_FOR_COURT_EXTRACT;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.IS_CONVICTED_RESULT;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.IS_FINANCIAL_RESULT;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.JUDICIAL_RESULT_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.JUDICIAL_RESULT_PROMPTS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.LABEL;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.LAST_SHARED_DATE_TIME;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ORDERED_DATE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ORDERED_HEARING_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.RANK;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.USERGROUPS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.WELSH_LABEL;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.transformDelegatePowers;

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
                    .add(JUDICIAL_RESULT_ID, result.getJsonString(ID))
                    .add(ORDERED_HEARING_ID, hearing.getJsonString(ID))
                    .add(LABEL, result.getJsonString(LABEL))
                    .add(IS_ADJOURNMENT_RESULT, Boolean.FALSE)
                    .add(IS_FINANCIAL_RESULT, Boolean.FALSE)
                    .add(IS_CONVICTED_RESULT, Boolean.FALSE)
                    .add(IS_AVAILABLE_FOR_COURT_EXTRACT, Boolean.FALSE)
                    .add(ORDERED_DATE, result.getJsonString(ORDERED_DATE))
                    .add(CATEGORY, result.getJsonString(CATEGORY));

            if (result.containsKey(WELSH_LABEL)) {
                transformResultBuilder.add(WELSH_LABEL, result.getString(WELSH_LABEL));
            }

            if (result.containsKey(C_JS_CODE)) {
                transformResultBuilder.add(C_JS_CODE, result.getBoolean(C_JS_CODE));
            }
            if (result.containsKey(RANK)) {
                transformResultBuilder.add(RANK, result.getJsonNumber(RANK));
            }
            if (result.containsKey(LAST_SHARED_DATE_TIME)) {
                transformResultBuilder.add(LAST_SHARED_DATE_TIME, result.getJsonString(LAST_SHARED_DATE_TIME));
            }
            if (result.containsKey(COURT_CLERK)) {
                transformResultBuilder.add(COURT_CLERK, transformDelegatePowers(result.getJsonObject(COURT_CLERK)));
            }
            if (result.containsKey(USERGROUPS)) {
                transformResultBuilder.add(USERGROUPS, result.getJsonObject(USERGROUPS));
            }
            if (result.containsKey(CATEGORY)) {
                transformResultBuilder.add(CATEGORY, result.getJsonString(CATEGORY));
            }
            if (result.containsKey(JUDICIAL_RESULT_PROMPTS)) {
                transformResultBuilder.add(JUDICIAL_RESULT_PROMPTS, result.getJsonObject(JUDICIAL_RESULT_PROMPTS));
            }

            transformedPayloadObjectBuilder.add(transformResultBuilder);
        });
        return transformedPayloadObjectBuilder.build();
    }
}