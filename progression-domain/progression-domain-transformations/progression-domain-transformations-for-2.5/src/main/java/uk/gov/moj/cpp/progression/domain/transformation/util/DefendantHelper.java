package uk.gov.moj.cpp.progression.domain.transformation.util;

import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.JudicialResultHelper.transformJudicialResults;
import static uk.gov.moj.cpp.progression.domain.transformation.util.OffenceHelper.transformOffences;
import static uk.gov.moj.cpp.progression.domain.transformation.util.PersonHelper.transformPerson;

import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@SuppressWarnings({"squid:MethodCyclomaticComplexity","squid:S1188", "squid:S3776"})
public class DefendantHelper {

    private DefendantHelper() {
    }

    public static JsonArray transformDefendants(final JsonArray defendants,
                                                final JsonObject hearing) {
        final Map<String, JsonArray> resultLines = CommonHelper.arrangeSharedResultLineByLevel(hearing.getJsonArray("sharedResultLines"));

        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();
        defendants.forEach(o -> {
            final JsonObject defendant = (JsonObject) o;
            final JsonObjectBuilder transformDefendantBuilder = createObjectBuilder()
                    .add(CommonHelper.ID, defendant.getJsonString(CommonHelper.ID))
                    .add(CommonHelper.PROSECUTION_CASE_ID, defendant.getJsonString(CommonHelper.PROSECUTION_CASE_ID))
                    .add(CommonHelper.OFFENCES, transformOffences(defendant.getJsonArray(CommonHelper.OFFENCES), hearing));

            if (defendant.containsKey(CommonHelper.NUMBER_OF_PREVIOUS_CONVICTIONS_CITED)) {
                transformDefendantBuilder.add(CommonHelper.NUMBER_OF_PREVIOUS_CONVICTIONS_CITED, defendant.getInt(CommonHelper.NUMBER_OF_PREVIOUS_CONVICTIONS_CITED));
            }

            if (defendant.containsKey(CommonHelper.PROSECUTION_AUTHORITY_REFERENCE)) {
                transformDefendantBuilder.add(CommonHelper.PROSECUTION_AUTHORITY_REFERENCE, defendant.getString(CommonHelper.PROSECUTION_AUTHORITY_REFERENCE));
            }

            if (defendant.containsKey(CommonHelper.WITNESS_STATEMENT)) {
                transformDefendantBuilder.add(CommonHelper.WITNESS_STATEMENT, defendant.getString(CommonHelper.WITNESS_STATEMENT));
            }

            if (defendant.containsKey(CommonHelper.WITNESS_STATEMENT_WELSH)) {
                transformDefendantBuilder.add(CommonHelper.WITNESS_STATEMENT_WELSH, defendant.getString(CommonHelper.WITNESS_STATEMENT_WELSH));
            }

            if (defendant.containsKey(CommonHelper.MITIGATION)) {
                transformDefendantBuilder.add(CommonHelper.MITIGATION, defendant.getString(CommonHelper.MITIGATION));
            }

            if (defendant.containsKey(CommonHelper.CRO_NUMBER)) {
                transformDefendantBuilder.add(CommonHelper.CRO_NUMBER, defendant.getString(CommonHelper.CRO_NUMBER));
            }

            if (defendant.getJsonObject(CommonHelper.PERSON_DEFENDANT).containsKey(CommonHelper.PNC_ID)) {
                transformDefendantBuilder.add(CommonHelper.PNC_ID, defendant.getJsonObject(CommonHelper.PERSON_DEFENDANT).getString(CommonHelper.PNC_ID));
            }

            if (defendant.containsKey(CommonHelper.ASSOCIATED_PERSONS)) {
                transformDefendantBuilder.add(CommonHelper.ASSOCIATED_PERSONS, transformAssociatedPersons(defendant.getJsonArray(CommonHelper.ASSOCIATED_PERSONS)));
            }

            if (defendant.containsKey(CommonHelper.DEFENCE_ORGANISATION)) {
                transformDefendantBuilder.add(CommonHelper.DEFENCE_ORGANISATION, defendant.getJsonObject(CommonHelper.DEFENCE_ORGANISATION));
            }

            if (defendant.containsKey(CommonHelper.PERSON_DEFENDANT)) {
                transformDefendantBuilder.add(CommonHelper.PERSON_DEFENDANT, PersonDefendantHelper.transformPersonDefendants(defendant.getJsonObject(CommonHelper.PERSON_DEFENDANT)));
            }

            if (defendant.containsKey(CommonHelper.LEGAL_ENTITY_DEFENDANT)) {
                transformDefendantBuilder.add(CommonHelper.LEGAL_ENTITY_DEFENDANT, defendant.getJsonObject(CommonHelper.LEGAL_ENTITY_DEFENDANT));
            }

            if (nonNull(resultLines.get(CommonHelper.DEFENDANT))) {
                transformDefendantBuilder.add(CommonHelper.JUDICIAL_RESULTS, transformJudicialResults(resultLines.get(CommonHelper.DEFENDANT), hearing));
            }

            if (defendant.getJsonObject(CommonHelper.PERSON_DEFENDANT).containsKey(CommonHelper.ALIASES)) {
                transformDefendantBuilder.add(CommonHelper.ALIASES, transformAliases(defendant.getJsonObject(CommonHelper.PERSON_DEFENDANT).getJsonArray(CommonHelper.ALIASES)));
            }
            transformedPayloadObjectBuilder.add(transformDefendantBuilder);
        });
        return transformedPayloadObjectBuilder.build();
    }

    public static JsonArray transformAliases(final JsonArray aliases) {
        final JsonArrayBuilder aliasObjectArray = createArrayBuilder();

        for(int i =0; i<aliases.size();i++) {
            aliasObjectArray.add(createObjectBuilder().add("lastName", aliases.get(i)).build());
        }
        return aliasObjectArray.build();
    }

    public static JsonArray transformDefendants(final JsonArray defendants) {
        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();
        defendants.forEach(o -> {
            final JsonObject defendant = (JsonObject) o;
            final JsonObjectBuilder transformDefendantBuilder = createObjectBuilder()
                    .add(CommonHelper.ID, defendant.getJsonString(CommonHelper.ID))
                    .add(CommonHelper.PROSECUTION_CASE_ID, defendant.getJsonString(CommonHelper.PROSECUTION_CASE_ID))
                    .add(CommonHelper.OFFENCES, transformOffences(defendant.getJsonArray(CommonHelper.OFFENCES)));

            if (defendant.containsKey(CommonHelper.NUMBER_OF_PREVIOUS_CONVICTIONS_CITED)) {
                transformDefendantBuilder.add(CommonHelper.NUMBER_OF_PREVIOUS_CONVICTIONS_CITED, defendant.getInt(CommonHelper.NUMBER_OF_PREVIOUS_CONVICTIONS_CITED));
            }

            if (defendant.containsKey(CommonHelper.PROSECUTION_AUTHORITY_REFERENCE)) {
                transformDefendantBuilder.add(CommonHelper.PROSECUTION_AUTHORITY_REFERENCE, defendant.getString(CommonHelper.PROSECUTION_AUTHORITY_REFERENCE));
            }

            if (defendant.containsKey(CommonHelper.WITNESS_STATEMENT)) {
                transformDefendantBuilder.add(CommonHelper.WITNESS_STATEMENT, defendant.getString(CommonHelper.WITNESS_STATEMENT));
            }

            if (defendant.containsKey(CommonHelper.WITNESS_STATEMENT_WELSH)) {
                transformDefendantBuilder.add(CommonHelper.WITNESS_STATEMENT_WELSH, defendant.getString(CommonHelper.WITNESS_STATEMENT_WELSH));
            }

            if (defendant.containsKey(CommonHelper.MITIGATION)) {
                transformDefendantBuilder.add(CommonHelper.MITIGATION, defendant.getString(CommonHelper.MITIGATION));
            }

            if (defendant.containsKey(CommonHelper.CRO_NUMBER)) {
                transformDefendantBuilder.add(CommonHelper.CRO_NUMBER, defendant.getString(CommonHelper.CRO_NUMBER));
            }

            if (defendant.getJsonObject(CommonHelper.PERSON_DEFENDANT).containsKey(CommonHelper.PNC_ID)) {
                transformDefendantBuilder.add(CommonHelper.PNC_ID, defendant.getJsonObject(CommonHelper.PERSON_DEFENDANT).getString(CommonHelper.PNC_ID));
            }

            if (defendant.containsKey(CommonHelper.ASSOCIATED_PERSONS)) {
                transformDefendantBuilder.add(CommonHelper.ASSOCIATED_PERSONS, transformAssociatedPersons(defendant.getJsonArray(CommonHelper.ASSOCIATED_PERSONS)));
            }

            if (defendant.containsKey(CommonHelper.DEFENCE_ORGANISATION)) {
                transformDefendantBuilder.add(CommonHelper.DEFENCE_ORGANISATION, defendant.getJsonObject(CommonHelper.DEFENCE_ORGANISATION));
            }

            if (defendant.containsKey(CommonHelper.PERSON_DEFENDANT)) {
                transformDefendantBuilder.add(CommonHelper.PERSON_DEFENDANT, PersonDefendantHelper.transformPersonDefendants(defendant.getJsonObject(CommonHelper.PERSON_DEFENDANT)));
            }

            if (defendant.containsKey(CommonHelper.LEGAL_ENTITY_DEFENDANT)) {
                transformDefendantBuilder.add(CommonHelper.LEGAL_ENTITY_DEFENDANT, defendant.getJsonObject(CommonHelper.LEGAL_ENTITY_DEFENDANT));
            }

            if (defendant.containsKey(CommonHelper.ALIASES)) {
                transformDefendantBuilder.add(CommonHelper.ALIASES, defendant.getJsonArray(CommonHelper.ALIASES));
            }
            transformedPayloadObjectBuilder.add(transformDefendantBuilder);
        });
        return transformedPayloadObjectBuilder.build();
    }

    private static JsonArray transformAssociatedPersons(final JsonArray associatedPersons) {
        final JsonArrayBuilder associatedPersonsList = createArrayBuilder();
        associatedPersons.forEach(o -> {
            final JsonObject assoc = (JsonObject) o;

            //add required fields
            final JsonObjectBuilder associatedPersonBuilder = createObjectBuilder()
                    .add(CommonHelper.ROLE, assoc.getJsonString(CommonHelper.ROLE))
                    .add(CommonHelper.PERSON, transformPerson(assoc.getJsonObject(CommonHelper.PERSON)));
            associatedPersonsList.add(associatedPersonBuilder);
        });
        return associatedPersonsList.build();
    }


}