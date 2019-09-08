package uk.gov.moj.cpp.progression.domain.transformation.util;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ADDRESS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ALIASES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ASSOCIATED_PERSONS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.BAIL_STATUS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.CRO_NUMBER;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.CUSTODY_TIME_LIMIT_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DATE_OF_BIRTH;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DEFENCE_ORGANISATION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.FIRST_NAME;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.GENDER;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.INTERPRETER;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.LAST_NAME;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.LEGAL_ENTITY_DEFENDANT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.MITIGATION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.MITIGATION_WELSH;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.NATIONALITY;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.NUMBER_OF_PREVIOUS_CONVICTIONS_CITED;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OFFENCES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PERSON_DEFENDANT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PERSON_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PNC_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTION_AUTHORITY_REFERENCE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTION_CASE_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.WITNESS_STATEMENT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.WITNESS_STATEMENT_WELSH;
import static uk.gov.moj.cpp.progression.domain.transformation.util.OffenceHelper.transformOffences;
import static uk.gov.moj.cpp.progression.domain.transformation.util.OffenceHelper.transformOffencesForSendingSheet;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S1188", "squid:S3776"})
public class DefendantHelper {

    private DefendantHelper() {
    }

    public static JsonArray transformDefendants(final JsonArray defendants,
                                                final JsonObject hearing) {
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

            if (defendant.containsKey(MITIGATION_WELSH)) {
                transformDefendantBuilder.add(MITIGATION_WELSH, defendant.getString(MITIGATION_WELSH));
            }

            if (defendant.containsKey(ASSOCIATED_PERSONS)) {
                transformDefendantBuilder.add(ASSOCIATED_PERSONS, defendant.getJsonArray(ASSOCIATED_PERSONS));
            }

            if (defendant.containsKey(DEFENCE_ORGANISATION)) {
                transformDefendantBuilder.add(DEFENCE_ORGANISATION, defendant.getJsonObject(DEFENCE_ORGANISATION));
            }

            if (defendant.containsKey(PERSON_DEFENDANT)) {
                transformDefendantBuilder.add(PERSON_DEFENDANT, defendant.getJsonObject(PERSON_DEFENDANT));
            }

            if (defendant.containsKey(LEGAL_ENTITY_DEFENDANT)) {
                transformDefendantBuilder.add(LEGAL_ENTITY_DEFENDANT, defendant.getJsonObject(LEGAL_ENTITY_DEFENDANT));
            }

            if (defendant.containsKey(ALIASES)) {
                transformDefendantBuilder.add(ALIASES, defendant.getJsonArray(ALIASES));
            }

            if (defendant.containsKey(JUDICIAL_RESULTS)) {
                transformDefendantBuilder.add(JUDICIAL_RESULTS, defendant.getJsonArray(JUDICIAL_RESULTS));
            }

            if (defendant.containsKey(CRO_NUMBER)) {
                transformDefendantBuilder.add(CRO_NUMBER, defendant.getString(CRO_NUMBER));
            }

            if (defendant.containsKey(PNC_ID)) {
                transformDefendantBuilder.add(PNC_ID, defendant.getString(PNC_ID));
            }

            transformedPayloadObjectBuilder.add(transformDefendantBuilder);
        });
        return transformedPayloadObjectBuilder.build();
    }

    public static JsonObject transformDefendant(final JsonObject defendant) {

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

        if (defendant.containsKey(MITIGATION_WELSH)) {
            transformDefendantBuilder.add(MITIGATION_WELSH, defendant.getString(MITIGATION_WELSH));
        }

        if (defendant.containsKey(ASSOCIATED_PERSONS)) {
            transformDefendantBuilder.add(ASSOCIATED_PERSONS, defendant.getJsonArray(ASSOCIATED_PERSONS));
        }

        if (defendant.containsKey(DEFENCE_ORGANISATION)) {
            transformDefendantBuilder.add(DEFENCE_ORGANISATION, defendant.getJsonObject(DEFENCE_ORGANISATION));
        }

        if (defendant.containsKey(PERSON_DEFENDANT)) {
            transformDefendantBuilder.add(PERSON_DEFENDANT, defendant.getJsonObject(PERSON_DEFENDANT));
        }

        if (defendant.containsKey(LEGAL_ENTITY_DEFENDANT)) {
            transformDefendantBuilder.add(LEGAL_ENTITY_DEFENDANT, defendant.getJsonObject(LEGAL_ENTITY_DEFENDANT));
        }

        if (defendant.containsKey(ALIASES)) {
            transformDefendantBuilder.add(ALIASES, defendant.getJsonArray(ALIASES));
        }

        if (defendant.containsKey(JUDICIAL_RESULTS)) {
            transformDefendantBuilder.add(JUDICIAL_RESULTS, defendant.getJsonArray(JUDICIAL_RESULTS));
        }

        if (defendant.containsKey(CRO_NUMBER)) {
            transformDefendantBuilder.add(CRO_NUMBER, defendant.getString(CRO_NUMBER));
        }

        if (defendant.containsKey(PNC_ID)) {
            transformDefendantBuilder.add(PNC_ID, defendant.getString(PNC_ID));
        }

        return transformDefendantBuilder.build();
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

            if (defendant.containsKey(MITIGATION_WELSH)) {
                transformDefendantBuilder.add(MITIGATION_WELSH, defendant.getString(MITIGATION_WELSH));
            }

            if (defendant.containsKey(ASSOCIATED_PERSONS)) {
                transformDefendantBuilder.add(ASSOCIATED_PERSONS, defendant.getJsonArray(ASSOCIATED_PERSONS));
            }

            if (defendant.containsKey(DEFENCE_ORGANISATION)) {
                transformDefendantBuilder.add(DEFENCE_ORGANISATION, defendant.getJsonObject(DEFENCE_ORGANISATION));
            }

            if (defendant.containsKey(PERSON_DEFENDANT)) {
                transformDefendantBuilder.add(PERSON_DEFENDANT, defendant.getJsonObject(PERSON_DEFENDANT));
            }

            if (defendant.containsKey(LEGAL_ENTITY_DEFENDANT)) {
                transformDefendantBuilder.add(LEGAL_ENTITY_DEFENDANT, defendant.getJsonObject(LEGAL_ENTITY_DEFENDANT));
            }

            if (defendant.containsKey(ALIASES)) {
                transformDefendantBuilder.add(ALIASES, defendant.getJsonArray(ALIASES));
            }

            if (defendant.containsKey(JUDICIAL_RESULTS)) {
                transformDefendantBuilder.add(JUDICIAL_RESULTS, defendant.getJsonArray(JUDICIAL_RESULTS));
            }

            if (defendant.containsKey(CRO_NUMBER)) {
                transformDefendantBuilder.add(CRO_NUMBER, defendant.getString(CRO_NUMBER));
            }

            if (defendant.containsKey(PNC_ID)) {
                transformDefendantBuilder.add(PNC_ID, defendant.getString(PNC_ID));
            }
            transformedPayloadObjectBuilder.add(transformDefendantBuilder);
        });
        return transformedPayloadObjectBuilder.build();
    }


    public static JsonArray transformDefendentForSendingSheet(final JsonArray defendants) {
        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();
        defendants.stream().map(o -> (JsonObject) o).forEach(defendant -> {
            final JsonObjectBuilder transformDefendantBuilder = createObjectBuilder();
            transformDefendantBuilder.add(ID, defendant.getJsonString(ID));
            transformDefendantBuilder.add(PERSON_ID, defendant.getJsonString(PERSON_ID));
            transformDefendantBuilder.add(FIRST_NAME, defendant.getJsonString(FIRST_NAME));
            transformDefendantBuilder.add(LAST_NAME, defendant.getJsonString(LAST_NAME));
            transformDefendantBuilder.add(NATIONALITY, defendant.getJsonString(NATIONALITY));
            transformDefendantBuilder.add(GENDER, defendant.getJsonString(GENDER));
            transformDefendantBuilder.add(ADDRESS, defendant.getJsonObject(ADDRESS));
            transformDefendantBuilder.add(DATE_OF_BIRTH, defendant.getJsonString(DATE_OF_BIRTH));
            transformDefendantBuilder.add(INTERPRETER, defendant.getJsonObject(INTERPRETER));
            transformDefendantBuilder.add(OFFENCES, transformOffencesForSendingSheet(defendant.getJsonArray(OFFENCES)));
            if (defendant.containsKey(BAIL_STATUS)) {
                transformDefendantBuilder.add(BAIL_STATUS, defendant.getString(BAIL_STATUS));
            }
            if (defendant.containsKey(CUSTODY_TIME_LIMIT_DATE)) {
                transformDefendantBuilder.add(CUSTODY_TIME_LIMIT_DATE, defendant.getString(CUSTODY_TIME_LIMIT_DATE));
            }
            if (defendant.containsKey(DEFENCE_ORGANISATION)) {
                transformDefendantBuilder.add(DEFENCE_ORGANISATION, defendant.getString(DEFENCE_ORGANISATION));
            }
            transformedPayloadObjectBuilder.add(transformDefendantBuilder);
        });
        return transformedPayloadObjectBuilder.build();
    }

}
