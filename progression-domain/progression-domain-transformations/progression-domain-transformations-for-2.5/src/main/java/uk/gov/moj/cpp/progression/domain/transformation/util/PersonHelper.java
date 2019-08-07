package uk.gov.moj.cpp.progression.domain.transformation.util;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static javax.json.Json.createObjectBuilder;

@SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S3776"})
public class PersonHelper {

    private PersonHelper() {
    }

    public static JsonObject transformPerson(final JsonObject person) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        if (person.containsKey(CommonHelper.GENDER)) {
            jsonObjectBuilder.add(CommonHelper.GENDER, person.getJsonString(CommonHelper.GENDER));
        }
        if (person.containsKey(CommonHelper.TITLE)) {
            jsonObjectBuilder.add(CommonHelper.TITLE, person.getJsonString(CommonHelper.TITLE));
        }
        if (person.containsKey(CommonHelper.FIRST_NAME)) {
            jsonObjectBuilder.add(CommonHelper.FIRST_NAME, person.getJsonString(CommonHelper.FIRST_NAME));
        }
        if (person.containsKey(CommonHelper.MIDDLE_NAME)) {
            jsonObjectBuilder.add(CommonHelper.MIDDLE_NAME, person.getJsonString(CommonHelper.MIDDLE_NAME));
        }
        if (person.containsKey(CommonHelper.LAST_NAME)) {
            jsonObjectBuilder.add(CommonHelper.LAST_NAME, person.getJsonString(CommonHelper.LAST_NAME));
        }
        if (person.containsKey(CommonHelper.DATE_OF_BIRTH)) {
            jsonObjectBuilder.add(CommonHelper.DATE_OF_BIRTH, person.getJsonString(CommonHelper.DATE_OF_BIRTH));
        }
        if (person.containsKey(CommonHelper.NATIONALITY_ID)) {
            jsonObjectBuilder.add(CommonHelper.NATIONALITY_ID, person.getJsonString(CommonHelper.NATIONALITY_ID));
        }
        if (person.containsKey(CommonHelper.NATIONALITY_CODE)) {
            jsonObjectBuilder.add(CommonHelper.NATIONALITY_CODE, person.getJsonString(CommonHelper.NATIONALITY_CODE));
        }
        if (person.containsKey(CommonHelper.NATIONALITY_DESCRIPTION)) {
            jsonObjectBuilder.add(CommonHelper.NATIONALITY_DESCRIPTION, person.getJsonString(CommonHelper.NATIONALITY_DESCRIPTION));
        }
        if (person.containsKey(CommonHelper.ADDITIONAL_NATIONALITY_ID)) {
            jsonObjectBuilder.add(CommonHelper.ADDITIONAL_NATIONALITY_ID, person.getJsonString(CommonHelper.ADDITIONAL_NATIONALITY_ID));
        }
        if (person.containsKey(CommonHelper.ADDITIONAL_NATIONALITY_CODE)) {
            jsonObjectBuilder.add(CommonHelper.ADDITIONAL_NATIONALITY_CODE, person.getJsonString(CommonHelper.ADDITIONAL_NATIONALITY_CODE));
        }
        if (person.containsKey(CommonHelper.ADDITIONAL_NATIONALITY_DESCRIPTION)) {
            jsonObjectBuilder.add(CommonHelper.ADDITIONAL_NATIONALITY_DESCRIPTION, person.getJsonString(CommonHelper.ADDITIONAL_NATIONALITY_DESCRIPTION));
        }
        if (person.containsKey(CommonHelper.DISABILITY_STATUS)) {
            jsonObjectBuilder.add(CommonHelper.DISABILITY_STATUS, person.getJsonString(CommonHelper.DISABILITY_STATUS));
        }
        if (person.containsKey(CommonHelper.INTERPRETER_LANGUAGE_NEEDS)) {
            jsonObjectBuilder.add(CommonHelper.INTERPRETER_LANGUAGE_NEEDS, person.getJsonString(CommonHelper.INTERPRETER_LANGUAGE_NEEDS));
        }
        if (person.containsKey(CommonHelper.DOCUMENTATION_LANGUAGE_NEEDS)) {
            jsonObjectBuilder.add(CommonHelper.DOCUMENTATION_LANGUAGE_NEEDS, person.getJsonString(CommonHelper.DOCUMENTATION_LANGUAGE_NEEDS));
        }
        if (person.containsKey(CommonHelper.NATIONAL_INSURANCE_NUMBER)) {
            jsonObjectBuilder.add(CommonHelper.NATIONAL_INSURANCE_NUMBER, person.getJsonString(CommonHelper.NATIONAL_INSURANCE_NUMBER));
        }
        if (person.containsKey(CommonHelper.OCCUPATION)) {
            jsonObjectBuilder.add(CommonHelper.OCCUPATION, person.getJsonString(CommonHelper.OCCUPATION));
        }
        if (person.containsKey(CommonHelper.OCCUPATION_CODE)) {
            jsonObjectBuilder.add(CommonHelper.OCCUPATION_CODE, person.getJsonString(CommonHelper.OCCUPATION_CODE));
        }
        if (person.containsKey(CommonHelper.SPECIFIC_REQUIREMENTS)) {
            jsonObjectBuilder.add(CommonHelper.SPECIFIC_REQUIREMENTS, person.getJsonString(CommonHelper.SPECIFIC_REQUIREMENTS));
        }
        if (person.containsKey(CommonHelper.ADDRESS)) {
            jsonObjectBuilder.add(CommonHelper.ADDRESS, person.getJsonObject(CommonHelper.ADDRESS));
        }
        if (person.containsKey(CommonHelper.CONTACT)) {
            jsonObjectBuilder.add(CommonHelper.CONTACT, person.getJsonObject(CommonHelper.CONTACT));
        }

        return jsonObjectBuilder.build();
    }

    public static JsonObject transformPerson(final JsonObject person, final JsonObject personDefendant) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        if (person.containsKey(CommonHelper.GENDER)) {
            jsonObjectBuilder.add(CommonHelper.GENDER, person.getJsonString(CommonHelper.GENDER));
        }
        if (person.containsKey(CommonHelper.TITLE)) {
            jsonObjectBuilder.add(CommonHelper.TITLE, person.getJsonString(CommonHelper.TITLE));
        }
        if (person.containsKey(CommonHelper.FIRST_NAME)) {
            jsonObjectBuilder.add(CommonHelper.FIRST_NAME, person.getJsonString(CommonHelper.FIRST_NAME));
        }
        if (person.containsKey(CommonHelper.MIDDLE_NAME)) {
            jsonObjectBuilder.add(CommonHelper.MIDDLE_NAME, person.getJsonString(CommonHelper.MIDDLE_NAME));
        }
        if (person.containsKey(CommonHelper.LAST_NAME)) {
            jsonObjectBuilder.add(CommonHelper.LAST_NAME, person.getJsonString(CommonHelper.LAST_NAME));
        }
        if (person.containsKey(CommonHelper.DATE_OF_BIRTH)) {
            jsonObjectBuilder.add(CommonHelper.DATE_OF_BIRTH, person.getJsonString(CommonHelper.DATE_OF_BIRTH));
        }
        if (person.containsKey(CommonHelper.NATIONALITY_ID)) {
            jsonObjectBuilder.add(CommonHelper.NATIONALITY_ID, person.getJsonString(CommonHelper.NATIONALITY_ID));
        }
        if (person.containsKey(CommonHelper.NATIONALITY_CODE)) {
            jsonObjectBuilder.add(CommonHelper.NATIONALITY_CODE, person.getJsonString(CommonHelper.NATIONALITY_CODE));
        }
        if (person.containsKey(CommonHelper.NATIONALITY_DESCRIPTION)) {
            jsonObjectBuilder.add(CommonHelper.NATIONALITY_DESCRIPTION, person.getJsonString(CommonHelper.NATIONALITY_DESCRIPTION));
        }
        if (person.containsKey(CommonHelper.ADDITIONAL_NATIONALITY_ID)) {
            jsonObjectBuilder.add(CommonHelper.ADDITIONAL_NATIONALITY_ID, person.getJsonString(CommonHelper.ADDITIONAL_NATIONALITY_ID));
        }
        if (person.containsKey(CommonHelper.ADDITIONAL_NATIONALITY_CODE)) {
            jsonObjectBuilder.add(CommonHelper.ADDITIONAL_NATIONALITY_CODE, person.getJsonString(CommonHelper.ADDITIONAL_NATIONALITY_CODE));
        }
        if (person.containsKey(CommonHelper.ADDITIONAL_NATIONALITY_DESCRIPTION)) {
            jsonObjectBuilder.add(CommonHelper.ADDITIONAL_NATIONALITY_DESCRIPTION, person.getJsonString(CommonHelper.ADDITIONAL_NATIONALITY_DESCRIPTION));
        }
        if (person.containsKey(CommonHelper.DISABILITY_STATUS)) {
            jsonObjectBuilder.add(CommonHelper.DISABILITY_STATUS, person.getJsonString(CommonHelper.DISABILITY_STATUS));
        }
        if (person.containsKey(CommonHelper.INTERPRETER_LANGUAGE_NEEDS)) {
            jsonObjectBuilder.add(CommonHelper.INTERPRETER_LANGUAGE_NEEDS, person.getJsonString(CommonHelper.INTERPRETER_LANGUAGE_NEEDS));
        }
        if (person.containsKey(CommonHelper.DOCUMENTATION_LANGUAGE_NEEDS)) {
            jsonObjectBuilder.add(CommonHelper.DOCUMENTATION_LANGUAGE_NEEDS, person.getJsonString(CommonHelper.DOCUMENTATION_LANGUAGE_NEEDS));
        }
        if (person.containsKey(CommonHelper.NATIONAL_INSURANCE_NUMBER)) {
            jsonObjectBuilder.add(CommonHelper.NATIONAL_INSURANCE_NUMBER, person.getJsonString(CommonHelper.NATIONAL_INSURANCE_NUMBER));
        }
        if (person.containsKey(CommonHelper.OCCUPATION)) {
            jsonObjectBuilder.add(CommonHelper.OCCUPATION, person.getJsonString(CommonHelper.OCCUPATION));
        }
        if (person.containsKey(CommonHelper.OCCUPATION_CODE)) {
            jsonObjectBuilder.add(CommonHelper.OCCUPATION_CODE, person.getJsonString(CommonHelper.OCCUPATION_CODE));
        }
        if (person.containsKey(CommonHelper.SPECIFIC_REQUIREMENTS)) {
            jsonObjectBuilder.add(CommonHelper.SPECIFIC_REQUIREMENTS, person.getJsonString(CommonHelper.SPECIFIC_REQUIREMENTS));
        }
        if (person.containsKey(CommonHelper.ADDRESS)) {
            jsonObjectBuilder.add(CommonHelper.ADDRESS, person.getJsonObject(CommonHelper.ADDRESS));
        }
        if (person.containsKey(CommonHelper.CONTACT)) {
            jsonObjectBuilder.add(CommonHelper.CONTACT, person.getJsonObject(CommonHelper.CONTACT));
        }
        if(personDefendant.containsKey(CommonHelper.OBSERVED_ETHNICITY_ID) ||  personDefendant.containsKey(CommonHelper.SELF_ETHNICITY_ID)) {
            jsonObjectBuilder.add(CommonHelper.ETHNICITY, buildEthnicityObject(personDefendant));
        }
        return jsonObjectBuilder.build();
    }

    public static JsonObject buildEthnicityObject(final JsonObject personDefendant) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        if (personDefendant.containsKey(CommonHelper.OBSERVED_ETHNICITY_ID)) {
            jsonObjectBuilder.add(CommonHelper.OBSERVED_ETHNICITY_ID, personDefendant.getJsonString(CommonHelper.OBSERVED_ETHNICITY_ID));
        }
        if (personDefendant.containsKey(CommonHelper.OBSERVED_ETHNICITY_CODE)) {
            jsonObjectBuilder.add(CommonHelper.OBSERVED_ETHNICITY_CODE, personDefendant.getJsonString(CommonHelper.OBSERVED_ETHNICITY_CODE));
        }
        if (personDefendant.containsKey(CommonHelper.OBSERVED_ETHNICITY_DESCRIPTION)) {
            jsonObjectBuilder.add(CommonHelper.OBSERVED_ETHNICITY_DESCRIPTION, personDefendant.getJsonString(CommonHelper.OBSERVED_ETHNICITY_DESCRIPTION));
        }
        if (personDefendant.containsKey(CommonHelper.SELF_ETHNICITY_ID)) {
            jsonObjectBuilder.add(CommonHelper.SELF_ETHNICITY_ID, personDefendant.getJsonString(CommonHelper.SELF_ETHNICITY_ID));
        }
        if (personDefendant.containsKey(CommonHelper.SELF_DEFINED_ETHNICITY_CODE)) {
            jsonObjectBuilder.add(CommonHelper.SELF_DEFINED_ETHNICITY_CODE, personDefendant.getJsonString(CommonHelper.SELF_DEFINED_ETHNICITY_CODE));
        }
        if (personDefendant.containsKey(CommonHelper.SELF_DEFINED_ETHNICITY_DESCRIPTION)) {
            jsonObjectBuilder.add(CommonHelper.SELF_DEFINED_ETHNICITY_DESCRIPTION, personDefendant.getJsonString(CommonHelper.SELF_DEFINED_ETHNICITY_DESCRIPTION));
        }

        return jsonObjectBuilder.build();
    }
}
