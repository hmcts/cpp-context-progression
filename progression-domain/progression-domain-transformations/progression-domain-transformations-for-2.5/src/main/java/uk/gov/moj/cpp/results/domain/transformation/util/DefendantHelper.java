package uk.gov.moj.cpp.results.domain.transformation.util;

import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ALIASES;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ASSOCIATED_PERSONS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.CRO_NUMBER;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.DEFENCE_ORGANISATION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.DEFENDANT;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.LEGAL_ENTITY_DEFENDANT;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.MITIGATION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.NUMBER_OF_PREVIOUS_CONVICTIONS_CITED;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OFFENCES;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.PERSON;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.PERSON_DEFENDANT;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.PNC_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.PROSECUTION_AUTHORITY_REFERENCE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.PROSECUTION_CASE_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ROLE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.WITNESS_STATEMENT;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.WITNESS_STATEMENT_WELSH;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.arrangeSharedResultLineByLevel;
import static uk.gov.moj.cpp.results.domain.transformation.util.JudicialResultHelper.transformJudicialResults;
import static uk.gov.moj.cpp.results.domain.transformation.util.OffenceHelper.transformOffences;
import static uk.gov.moj.cpp.results.domain.transformation.util.PersonDefendantHelper.transformPersonDefendants;
import static uk.gov.moj.cpp.results.domain.transformation.util.PersonHelper.transformPerson;

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
        final Map<String, JsonArray> resultLines = arrangeSharedResultLineByLevel(hearing.getJsonArray("sharedResultLines"));

        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();
        defendants.forEach(o -> {
            final JsonObject defendant = (JsonObject) o;
            final JsonObjectBuilder transformDefendantBuilder = createObjectBuilder()
                    .add(ID, defendant.getJsonString(ID))
                    .add(PROSECUTION_CASE_ID, defendant.getJsonString(PROSECUTION_CASE_ID))
                    .add(OFFENCES, transformOffences(defendant.getJsonArray(OFFENCES), hearing));

            if (defendant.containsKey(NUMBER_OF_PREVIOUS_CONVICTIONS_CITED)) {
                transformDefendantBuilder.add(NUMBER_OF_PREVIOUS_CONVICTIONS_CITED, defendant.getInt(NUMBER_OF_PREVIOUS_CONVICTIONS_CITED));
            }

            if (defendant.containsKey(PROSECUTION_AUTHORITY_REFERENCE)) {
                transformDefendantBuilder.add(PROSECUTION_AUTHORITY_REFERENCE, defendant.getString(PROSECUTION_AUTHORITY_REFERENCE));
            }

            if (defendant.containsKey(WITNESS_STATEMENT)) {
                transformDefendantBuilder.add(WITNESS_STATEMENT, defendant.getString(WITNESS_STATEMENT));
            }

            if (defendant.containsKey(WITNESS_STATEMENT_WELSH)) {
                transformDefendantBuilder.add(WITNESS_STATEMENT_WELSH, defendant.getString(WITNESS_STATEMENT_WELSH));
            }

            if (defendant.containsKey(MITIGATION)) {
                transformDefendantBuilder.add(MITIGATION, defendant.getString(MITIGATION));
            }

            if (defendant.containsKey(CRO_NUMBER)) {
                transformDefendantBuilder.add(CRO_NUMBER, defendant.getString(CRO_NUMBER));
            }

            if (defendant.getJsonObject(PERSON_DEFENDANT).containsKey(PNC_ID)) {
                transformDefendantBuilder.add(PNC_ID, defendant.getJsonObject(PERSON_DEFENDANT).getString(PNC_ID));
            }

            if (defendant.containsKey(ASSOCIATED_PERSONS)) {
                transformDefendantBuilder.add(ASSOCIATED_PERSONS, transformAssociatedPersons(defendant.getJsonArray(ASSOCIATED_PERSONS)));
            }

            if (defendant.containsKey(DEFENCE_ORGANISATION)) {
                transformDefendantBuilder.add(DEFENCE_ORGANISATION, defendant.getJsonObject(DEFENCE_ORGANISATION));
            }

            if (defendant.containsKey(PERSON_DEFENDANT)) {
                transformDefendantBuilder.add(PERSON_DEFENDANT, transformPersonDefendants(defendant.getJsonObject(PERSON_DEFENDANT)));
            }

            if (defendant.containsKey(LEGAL_ENTITY_DEFENDANT)) {
                transformDefendantBuilder.add(LEGAL_ENTITY_DEFENDANT, defendant.getJsonObject(LEGAL_ENTITY_DEFENDANT));
            }

            if (nonNull(resultLines.get(DEFENDANT))) {
                transformDefendantBuilder.add(JUDICIAL_RESULTS, transformJudicialResults(resultLines.get(DEFENDANT), hearing));
            }

            if (defendant.getJsonObject(PERSON_DEFENDANT).containsKey(ALIASES)) {
                transformDefendantBuilder.add(ALIASES, transformAliases(defendant.getJsonObject(PERSON_DEFENDANT).getJsonArray(ALIASES)));
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
                    .add(ID, defendant.getJsonString(ID))
                    .add(PROSECUTION_CASE_ID, defendant.getJsonString(PROSECUTION_CASE_ID))
                    .add(OFFENCES, transformOffences(defendant.getJsonArray(OFFENCES)));

            if (defendant.containsKey(NUMBER_OF_PREVIOUS_CONVICTIONS_CITED)) {
                transformDefendantBuilder.add(NUMBER_OF_PREVIOUS_CONVICTIONS_CITED, defendant.getInt(NUMBER_OF_PREVIOUS_CONVICTIONS_CITED));
            }

            if (defendant.containsKey(PROSECUTION_AUTHORITY_REFERENCE)) {
                transformDefendantBuilder.add(PROSECUTION_AUTHORITY_REFERENCE, defendant.getString(PROSECUTION_AUTHORITY_REFERENCE));
            }

            if (defendant.containsKey(WITNESS_STATEMENT)) {
                transformDefendantBuilder.add(WITNESS_STATEMENT, defendant.getString(WITNESS_STATEMENT));
            }

            if (defendant.containsKey(WITNESS_STATEMENT_WELSH)) {
                transformDefendantBuilder.add(WITNESS_STATEMENT_WELSH, defendant.getString(WITNESS_STATEMENT_WELSH));
            }

            if (defendant.containsKey(MITIGATION)) {
                transformDefendantBuilder.add(MITIGATION, defendant.getString(MITIGATION));
            }

            if (defendant.containsKey(CRO_NUMBER)) {
                transformDefendantBuilder.add(CRO_NUMBER, defendant.getString(CRO_NUMBER));
            }

            if (defendant.getJsonObject(PERSON_DEFENDANT).containsKey(PNC_ID)) {
                transformDefendantBuilder.add(PNC_ID, defendant.getJsonObject(PERSON_DEFENDANT).getString(PNC_ID));
            }

            if (defendant.containsKey(ASSOCIATED_PERSONS)) {
                transformDefendantBuilder.add(ASSOCIATED_PERSONS, transformAssociatedPersons(defendant.getJsonArray(ASSOCIATED_PERSONS)));
            }

            if (defendant.containsKey(DEFENCE_ORGANISATION)) {
                transformDefendantBuilder.add(DEFENCE_ORGANISATION, defendant.getJsonObject(DEFENCE_ORGANISATION));
            }

            if (defendant.containsKey(PERSON_DEFENDANT)) {
                transformDefendantBuilder.add(PERSON_DEFENDANT, transformPersonDefendants(defendant.getJsonObject(PERSON_DEFENDANT)));
            }

            if (defendant.containsKey(LEGAL_ENTITY_DEFENDANT)) {
                transformDefendantBuilder.add(LEGAL_ENTITY_DEFENDANT, defendant.getJsonObject(LEGAL_ENTITY_DEFENDANT));
            }

            if (defendant.containsKey(ALIASES)) {
                transformDefendantBuilder.add(ALIASES, defendant.getJsonArray(ALIASES));
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
                    .add(ROLE, assoc.getJsonString(ROLE))
                    .add(PERSON, transformPerson(assoc.getJsonObject(PERSON)));
            associatedPersonsList.add(associatedPersonBuilder);
        });
        return associatedPersonsList.build();
    }


}