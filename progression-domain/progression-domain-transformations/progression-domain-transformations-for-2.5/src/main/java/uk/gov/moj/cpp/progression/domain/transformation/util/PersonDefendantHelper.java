package uk.gov.moj.cpp.progression.domain.transformation.util;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ARREST_SUMMONS_NUMBER;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.BAIL_STATUS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.CUSTODY_TIME_LIMIT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DRIVER_NUMBER;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.EMPLOYER_ORGANISATION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.EMPLOYER_PAYROLL_REFERENCE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PERCEIVED_BIRTH_YEAR;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PERSON_DETAILS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.PersonHelper.transformPerson;

public class PersonDefendantHelper {

    private PersonDefendantHelper() {
    }

    public static JsonObject transformPersonDefendants(final JsonObject personDefendant) {
            final JsonObjectBuilder personDefendantBuilder = createObjectBuilder()
                    .add(PERSON_DETAILS, transformPerson(personDefendant.getJsonObject(PERSON_DETAILS), personDefendant));

            // add optional fields
            if (personDefendant.containsKey(BAIL_STATUS)) {
                personDefendantBuilder.add(BAIL_STATUS, personDefendant.getJsonString(BAIL_STATUS));
            }
            if (personDefendant.containsKey(CUSTODY_TIME_LIMIT)) {
                personDefendantBuilder.add(CUSTODY_TIME_LIMIT, personDefendant.getJsonString(CUSTODY_TIME_LIMIT));
            }
            if (personDefendant.containsKey(PERCEIVED_BIRTH_YEAR)) {
                personDefendantBuilder.add(PERCEIVED_BIRTH_YEAR, personDefendant.getJsonNumber(PERCEIVED_BIRTH_YEAR));
            }
            if (personDefendant.containsKey(DRIVER_NUMBER)) {
                personDefendantBuilder.add(DRIVER_NUMBER, personDefendant.getJsonString(DRIVER_NUMBER));
            }

            if (personDefendant.containsKey(ARREST_SUMMONS_NUMBER)) {
                personDefendantBuilder.add(ARREST_SUMMONS_NUMBER, personDefendant.getJsonString(ARREST_SUMMONS_NUMBER));
            }

            if (personDefendant.containsKey(EMPLOYER_ORGANISATION)) {
                personDefendantBuilder.add(EMPLOYER_ORGANISATION, personDefendant.getJsonObject(EMPLOYER_ORGANISATION));
            }

            if (personDefendant.containsKey(EMPLOYER_PAYROLL_REFERENCE)) {
                personDefendantBuilder.add(EMPLOYER_PAYROLL_REFERENCE, personDefendant.getJsonString(EMPLOYER_PAYROLL_REFERENCE));
            }
            return personDefendantBuilder.build();
    }
}