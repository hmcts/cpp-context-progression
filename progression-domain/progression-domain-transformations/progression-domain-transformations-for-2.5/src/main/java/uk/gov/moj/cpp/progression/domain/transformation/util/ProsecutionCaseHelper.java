package uk.gov.moj.cpp.progression.domain.transformation.util;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.DefendantHelper.transformDefendants;

public class ProsecutionCaseHelper {

    private ProsecutionCaseHelper() {
    }

    public static JsonObject transformProsecutionCase(final JsonObject prosecutionCase) {
        JsonObjectBuilder transformProsecutionCaseBuilder = createObjectBuilder()
                .add("id", prosecutionCase.getString("id"))
                .add("prosecutionCaseIdentifier", prosecutionCase.getJsonObject("prosecutionCaseIdentifier"))
                .add("initiationCode", prosecutionCase.getString("initiationCode"))
                .add("defendants", DefendantHelper.transformDefendants(prosecutionCase.getJsonArray("defendants")));

        addOptionalFields(prosecutionCase, transformProsecutionCaseBuilder);

        return transformProsecutionCaseBuilder.build();
    }

    public static JsonArray transformProsecutionCases(final JsonArray prosecutionCases,
                                                      final JsonObject hearing) {

        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();

        prosecutionCases.forEach(o -> {
            final JsonObject prosecutionCase = (JsonObject) o;

            final JsonObjectBuilder transformProsecutionCaseBuilder = createObjectBuilder()
                    .add(CommonHelper.ID, prosecutionCase.getString(CommonHelper.ID))
                    .add(CommonHelper.PROSECUTION_CASE_IDENTIFIER, prosecutionCase.getJsonObject(CommonHelper.PROSECUTION_CASE_IDENTIFIER))
                    .add(CommonHelper.INITIATION_CODE, prosecutionCase.getString(CommonHelper.INITIATION_CODE))
                    .add(CommonHelper.DEFENDANTS, DefendantHelper.transformDefendants(prosecutionCase.getJsonArray(CommonHelper.DEFENDANTS), hearing));

            // add optional fields
            addOptionalFields(prosecutionCase, transformProsecutionCaseBuilder);

            transformedPayloadObjectBuilder.add(transformProsecutionCaseBuilder);
        });
        return transformedPayloadObjectBuilder.build();
    }

    private static void addOptionalFields(final JsonObject prosecutionCase, final JsonObjectBuilder transformProsecutionCaseBuilder) {
        if (prosecutionCase.containsKey(CommonHelper.ORIGINATING_ORGANISATION)) {
            transformProsecutionCaseBuilder.add(CommonHelper.ORIGINATING_ORGANISATION, prosecutionCase.getString(CommonHelper.ORIGINATING_ORGANISATION));
        }

        if (prosecutionCase.containsKey(CommonHelper.CASE_STATUS)) {
            transformProsecutionCaseBuilder.add(CommonHelper.CASE_STATUS, prosecutionCase.getString(CommonHelper.CASE_STATUS));
        }

        if (prosecutionCase.containsKey(CommonHelper.STATEMENT_OF_FACTS)) {
            transformProsecutionCaseBuilder.add(CommonHelper.STATEMENT_OF_FACTS, prosecutionCase.getString(CommonHelper.STATEMENT_OF_FACTS));
        }

        if (prosecutionCase.containsKey(CommonHelper.STATEMENT_OF_FACTS_WELSH)) {
            transformProsecutionCaseBuilder.add(CommonHelper.STATEMENT_OF_FACTS_WELSH, prosecutionCase.getString(CommonHelper.STATEMENT_OF_FACTS_WELSH));
        }

        if (prosecutionCase.containsKey(CommonHelper.BREACH_PROCEEDINGS_PENDING)) {
            transformProsecutionCaseBuilder.add(CommonHelper.BREACH_PROCEEDINGS_PENDING, prosecutionCase.getBoolean(CommonHelper.BREACH_PROCEEDINGS_PENDING));
        }

        if (prosecutionCase.containsKey(CommonHelper.APPEAL_PROCEEDINGS_PENDING)) {
            transformProsecutionCaseBuilder.add(CommonHelper.APPEAL_PROCEEDINGS_PENDING, prosecutionCase.getBoolean(CommonHelper.APPEAL_PROCEEDINGS_PENDING));
        }

        if (prosecutionCase.containsKey(CommonHelper.POLICE_OFFICER_IN_CASE)) {
            transformProsecutionCaseBuilder.add(CommonHelper.POLICE_OFFICER_IN_CASE, prosecutionCase.getJsonObject(CommonHelper.POLICE_OFFICER_IN_CASE));
        }

        if (prosecutionCase.containsKey(CommonHelper.CASE_MARKERS)) {
            transformProsecutionCaseBuilder.add(CommonHelper.CASE_MARKERS, prosecutionCase.getJsonObject(CommonHelper.CASE_MARKERS));
        }

    }
}
