package uk.gov.moj.cpp.results.domain.transformation.util;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.APPEAL_PROCEEDINGS_PENDING;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.BREACH_PROCEEDINGS_PENDING;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.CASE_MARKERS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.CASE_STATUS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.DEFENDANTS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.INITIATION_CODE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ORIGINATING_ORGANISATION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.POLICE_OFFICER_IN_CASE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.PROSECUTION_CASE_IDENTIFIER;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.STATEMENT_OF_FACTS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.STATEMENT_OF_FACTS_WELSH;
import static uk.gov.moj.cpp.results.domain.transformation.util.DefendantHelper.transformDefendants;

public class ProsecutionCaseHelper {

    private ProsecutionCaseHelper() {
    }

    public static JsonObject transformProsecutionCase(final JsonObject prosecutionCase) {
        final JsonObjectBuilder transformProsecutionCaseBuilder = createObjectBuilder()
                .add("id", prosecutionCase.getString("id"))
                .add("prosecutionCaseIdentifier", prosecutionCase.getJsonObject("prosecutionCaseIdentifier"))
                .add("initiationCode", prosecutionCase.getString("initiationCode"))
                .add("defendants", transformDefendants(prosecutionCase.getJsonArray("defendants")));

        addOptionalFields(prosecutionCase, transformProsecutionCaseBuilder);

        return transformProsecutionCaseBuilder.build();
    }

    public static JsonArray transformProsecutionCases(final JsonArray prosecutionCases,
                                                      final JsonObject hearing) {

        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();

        prosecutionCases.forEach(o -> {
            final JsonObject prosecutionCase = (JsonObject) o;

            final JsonObjectBuilder transformProsecutionCaseBuilder = createObjectBuilder()
                    .add(ID, prosecutionCase.getString(ID))
                    .add(PROSECUTION_CASE_IDENTIFIER, prosecutionCase.getJsonObject(PROSECUTION_CASE_IDENTIFIER))
                    .add(INITIATION_CODE, prosecutionCase.getString(INITIATION_CODE))
                    .add(DEFENDANTS, transformDefendants(prosecutionCase.getJsonArray(DEFENDANTS), hearing));

            // add optional fields
            addOptionalFields(prosecutionCase, transformProsecutionCaseBuilder);

            transformedPayloadObjectBuilder.add(transformProsecutionCaseBuilder);
        });
        return transformedPayloadObjectBuilder.build();
    }

    private static void addOptionalFields(final JsonObject prosecutionCase, final JsonObjectBuilder transformProsecutionCaseBuilder) {
        if (prosecutionCase.containsKey(ORIGINATING_ORGANISATION)) {
            transformProsecutionCaseBuilder.add(ORIGINATING_ORGANISATION, prosecutionCase.getString(ORIGINATING_ORGANISATION));
        }

        if (prosecutionCase.containsKey(CASE_STATUS)) {
            transformProsecutionCaseBuilder.add(CASE_STATUS, prosecutionCase.getString(CASE_STATUS));
        }

        if (prosecutionCase.containsKey(STATEMENT_OF_FACTS)) {
            transformProsecutionCaseBuilder.add(STATEMENT_OF_FACTS, prosecutionCase.getString(STATEMENT_OF_FACTS));
        }

        if (prosecutionCase.containsKey(STATEMENT_OF_FACTS_WELSH)) {
            transformProsecutionCaseBuilder.add(STATEMENT_OF_FACTS_WELSH, prosecutionCase.getString(STATEMENT_OF_FACTS_WELSH));
        }

        if (prosecutionCase.containsKey(BREACH_PROCEEDINGS_PENDING)) {
            transformProsecutionCaseBuilder.add(BREACH_PROCEEDINGS_PENDING, prosecutionCase.getBoolean(BREACH_PROCEEDINGS_PENDING));
        }

        if (prosecutionCase.containsKey(APPEAL_PROCEEDINGS_PENDING)) {
            transformProsecutionCaseBuilder.add(APPEAL_PROCEEDINGS_PENDING, prosecutionCase.getBoolean(APPEAL_PROCEEDINGS_PENDING));
        }

        if (prosecutionCase.containsKey(POLICE_OFFICER_IN_CASE)) {
            transformProsecutionCaseBuilder.add(POLICE_OFFICER_IN_CASE, prosecutionCase.getJsonObject(POLICE_OFFICER_IN_CASE));
        }

        if (prosecutionCase.containsKey(CASE_MARKERS)) {
            transformProsecutionCaseBuilder.add(CASE_MARKERS, prosecutionCase.getJsonObject(CASE_MARKERS));
        }

    }
}
