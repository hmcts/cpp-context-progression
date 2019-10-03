package uk.gov.moj.cpp.results.domain.transformation.util;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ADDITIONAL_NATIONALITY_CODE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ADDITIONAL_NATIONALITY_DESCRIPTION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ADDITIONAL_NATIONALITY_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ADDRESS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.CONTACT;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.DATE_OF_BIRTH;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.DISABILITY_STATUS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.DOCUMENTATION_LANGUAGE_NEEDS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ETHNICITY;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.FIRST_NAME;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.GENDER;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.INTERPRETER_LANGUAGE_NEEDS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.LAST_NAME;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.MIDDLE_NAME;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.NATIONALITY_CODE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.NATIONALITY_DESCRIPTION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.NATIONALITY_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.NATIONAL_INSURANCE_NUMBER;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OBSERVED_ETHNICITY_CODE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OBSERVED_ETHNICITY_DESCRIPTION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OBSERVED_ETHNICITY_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OCCUPATION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OCCUPATION_CODE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.SELF_DEFINED_ETHNICITY_CODE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.SELF_DEFINED_ETHNICITY_DESCRIPTION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.SELF_ETHNICITY_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.SPECIFIC_REQUIREMENTS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.TITLE;

@SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S3776"})
public class PersonHelper {

    private PersonHelper() {
    }

    public static JsonObject transformPerson(final JsonObject person) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        if (person.containsKey(GENDER)) {
            jsonObjectBuilder.add(GENDER, person.getJsonString(GENDER));
        }
        if (person.containsKey(TITLE)) {
            jsonObjectBuilder.add(TITLE, person.getJsonString(TITLE));
        }
        if (person.containsKey(FIRST_NAME)) {
            jsonObjectBuilder.add(FIRST_NAME, person.getJsonString(FIRST_NAME));
        }
        if (person.containsKey(MIDDLE_NAME)) {
            jsonObjectBuilder.add(MIDDLE_NAME, person.getJsonString(MIDDLE_NAME));
        }
        if (person.containsKey(LAST_NAME)) {
            jsonObjectBuilder.add(LAST_NAME, person.getJsonString(LAST_NAME));
        }
        if (person.containsKey(DATE_OF_BIRTH)) {
            jsonObjectBuilder.add(DATE_OF_BIRTH, person.getJsonString(DATE_OF_BIRTH));
        }
        if (person.containsKey(NATIONALITY_ID)) {
            jsonObjectBuilder.add(NATIONALITY_ID, person.getJsonString(NATIONALITY_ID));
        }
        if (person.containsKey(NATIONALITY_CODE)) {
            jsonObjectBuilder.add(NATIONALITY_CODE, person.getJsonString(NATIONALITY_CODE));
        }
        if (person.containsKey(NATIONALITY_DESCRIPTION)) {
            jsonObjectBuilder.add(NATIONALITY_DESCRIPTION, person.getJsonString(NATIONALITY_DESCRIPTION));
        }
        if (person.containsKey(ADDITIONAL_NATIONALITY_ID)) {
            jsonObjectBuilder.add(ADDITIONAL_NATIONALITY_ID, person.getJsonString(ADDITIONAL_NATIONALITY_ID));
        }
        if (person.containsKey(ADDITIONAL_NATIONALITY_CODE)) {
            jsonObjectBuilder.add(ADDITIONAL_NATIONALITY_CODE, person.getJsonString(ADDITIONAL_NATIONALITY_CODE));
        }
        if (person.containsKey(ADDITIONAL_NATIONALITY_DESCRIPTION)) {
            jsonObjectBuilder.add(ADDITIONAL_NATIONALITY_DESCRIPTION, person.getJsonString(ADDITIONAL_NATIONALITY_DESCRIPTION));
        }
        if (person.containsKey(DISABILITY_STATUS)) {
            jsonObjectBuilder.add(DISABILITY_STATUS, person.getJsonString(DISABILITY_STATUS));
        }
        if (person.containsKey(INTERPRETER_LANGUAGE_NEEDS)) {
            jsonObjectBuilder.add(INTERPRETER_LANGUAGE_NEEDS, person.getJsonString(INTERPRETER_LANGUAGE_NEEDS));
        }
        if (person.containsKey(DOCUMENTATION_LANGUAGE_NEEDS)) {
            jsonObjectBuilder.add(DOCUMENTATION_LANGUAGE_NEEDS, person.getJsonString(DOCUMENTATION_LANGUAGE_NEEDS));
        }
        if (person.containsKey(NATIONAL_INSURANCE_NUMBER)) {
            jsonObjectBuilder.add(NATIONAL_INSURANCE_NUMBER, person.getJsonString(NATIONAL_INSURANCE_NUMBER));
        }
        if (person.containsKey(OCCUPATION)) {
            jsonObjectBuilder.add(OCCUPATION, person.getJsonString(OCCUPATION));
        }
        if (person.containsKey(OCCUPATION_CODE)) {
            jsonObjectBuilder.add(OCCUPATION_CODE, person.getJsonString(OCCUPATION_CODE));
        }
        if (person.containsKey(SPECIFIC_REQUIREMENTS)) {
            jsonObjectBuilder.add(SPECIFIC_REQUIREMENTS, person.getJsonString(SPECIFIC_REQUIREMENTS));
        }
        if (person.containsKey(ADDRESS)) {
            jsonObjectBuilder.add(ADDRESS, person.getJsonObject(ADDRESS));
        }
        if (person.containsKey(CONTACT)) {
            jsonObjectBuilder.add(CONTACT, person.getJsonObject(CONTACT));
        }

        return jsonObjectBuilder.build();
    }

    public static JsonObject transformPerson(final JsonObject person, final JsonObject personDefendant) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        if (person.containsKey(GENDER)) {
            jsonObjectBuilder.add(GENDER, person.getJsonString(GENDER));
        }
        if (person.containsKey(TITLE)) {
            jsonObjectBuilder.add(TITLE, person.getJsonString(TITLE));
        }
        if (person.containsKey(FIRST_NAME)) {
            jsonObjectBuilder.add(FIRST_NAME, person.getJsonString(FIRST_NAME));
        }
        if (person.containsKey(MIDDLE_NAME)) {
            jsonObjectBuilder.add(MIDDLE_NAME, person.getJsonString(MIDDLE_NAME));
        }
        if (person.containsKey(LAST_NAME)) {
            jsonObjectBuilder.add(LAST_NAME, person.getJsonString(LAST_NAME));
        }
        if (person.containsKey(DATE_OF_BIRTH)) {
            jsonObjectBuilder.add(DATE_OF_BIRTH, person.getJsonString(DATE_OF_BIRTH));
        }
        if (person.containsKey(NATIONALITY_ID)) {
            jsonObjectBuilder.add(NATIONALITY_ID, person.getJsonString(NATIONALITY_ID));
        }
        if (person.containsKey(NATIONALITY_CODE)) {
            jsonObjectBuilder.add(NATIONALITY_CODE, person.getJsonString(NATIONALITY_CODE));
        }
        if (person.containsKey(NATIONALITY_DESCRIPTION)) {
            jsonObjectBuilder.add(NATIONALITY_DESCRIPTION, person.getJsonString(NATIONALITY_DESCRIPTION));
        }
        if (person.containsKey(ADDITIONAL_NATIONALITY_ID)) {
            jsonObjectBuilder.add(ADDITIONAL_NATIONALITY_ID, person.getJsonString(ADDITIONAL_NATIONALITY_ID));
        }
        if (person.containsKey(ADDITIONAL_NATIONALITY_CODE)) {
            jsonObjectBuilder.add(ADDITIONAL_NATIONALITY_CODE, person.getJsonString(ADDITIONAL_NATIONALITY_CODE));
        }
        if (person.containsKey(ADDITIONAL_NATIONALITY_DESCRIPTION)) {
            jsonObjectBuilder.add(ADDITIONAL_NATIONALITY_DESCRIPTION, person.getJsonString(ADDITIONAL_NATIONALITY_DESCRIPTION));
        }
        if (person.containsKey(DISABILITY_STATUS)) {
            jsonObjectBuilder.add(DISABILITY_STATUS, person.getJsonString(DISABILITY_STATUS));
        }
        if (person.containsKey(INTERPRETER_LANGUAGE_NEEDS)) {
            jsonObjectBuilder.add(INTERPRETER_LANGUAGE_NEEDS, person.getJsonString(INTERPRETER_LANGUAGE_NEEDS));
        }
        if (person.containsKey(DOCUMENTATION_LANGUAGE_NEEDS)) {
            jsonObjectBuilder.add(DOCUMENTATION_LANGUAGE_NEEDS, person.getJsonString(DOCUMENTATION_LANGUAGE_NEEDS));
        }
        if (person.containsKey(NATIONAL_INSURANCE_NUMBER)) {
            jsonObjectBuilder.add(NATIONAL_INSURANCE_NUMBER, person.getJsonString(NATIONAL_INSURANCE_NUMBER));
        }
        if (person.containsKey(OCCUPATION)) {
            jsonObjectBuilder.add(OCCUPATION, person.getJsonString(OCCUPATION));
        }
        if (person.containsKey(OCCUPATION_CODE)) {
            jsonObjectBuilder.add(OCCUPATION_CODE, person.getJsonString(OCCUPATION_CODE));
        }
        if (person.containsKey(SPECIFIC_REQUIREMENTS)) {
            jsonObjectBuilder.add(SPECIFIC_REQUIREMENTS, person.getJsonString(SPECIFIC_REQUIREMENTS));
        }
        if (person.containsKey(ADDRESS)) {
            jsonObjectBuilder.add(ADDRESS, person.getJsonObject(ADDRESS));
        }
        if (person.containsKey(CONTACT)) {
            jsonObjectBuilder.add(CONTACT, person.getJsonObject(CONTACT));
        }
        if(personDefendant.containsKey(OBSERVED_ETHNICITY_ID) ||  personDefendant.containsKey(SELF_ETHNICITY_ID)) {
            jsonObjectBuilder.add(ETHNICITY, buildEthnicityObject(personDefendant));
        }
        return jsonObjectBuilder.build();
    }

    public static JsonObject buildEthnicityObject(final JsonObject personDefendant) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        if (personDefendant.containsKey(OBSERVED_ETHNICITY_ID)) {
            jsonObjectBuilder.add(OBSERVED_ETHNICITY_ID, personDefendant.getJsonString(OBSERVED_ETHNICITY_ID));
        }
        if (personDefendant.containsKey(OBSERVED_ETHNICITY_CODE)) {
            jsonObjectBuilder.add(OBSERVED_ETHNICITY_CODE, personDefendant.getJsonString(OBSERVED_ETHNICITY_CODE));
        }
        if (personDefendant.containsKey(OBSERVED_ETHNICITY_DESCRIPTION)) {
            jsonObjectBuilder.add(OBSERVED_ETHNICITY_DESCRIPTION, personDefendant.getJsonString(OBSERVED_ETHNICITY_DESCRIPTION));
        }
        if (personDefendant.containsKey(SELF_ETHNICITY_ID)) {
            jsonObjectBuilder.add(SELF_ETHNICITY_ID, personDefendant.getJsonString(SELF_ETHNICITY_ID));
        }
        if (personDefendant.containsKey(SELF_DEFINED_ETHNICITY_CODE)) {
            jsonObjectBuilder.add(SELF_DEFINED_ETHNICITY_CODE, personDefendant.getJsonString(SELF_DEFINED_ETHNICITY_CODE));
        }
        if (personDefendant.containsKey(SELF_DEFINED_ETHNICITY_DESCRIPTION)) {
            jsonObjectBuilder.add(SELF_DEFINED_ETHNICITY_DESCRIPTION, personDefendant.getJsonString(SELF_DEFINED_ETHNICITY_DESCRIPTION));
        }

        return jsonObjectBuilder.build();
    }
}
