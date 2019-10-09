package uk.gov.moj.cpp.progression.domain.transformation.util;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DEFENDANT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ORGANISATION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ORGANISATION_PERSONS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PERSON_DETAILS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.REPRESENTATION_ORGANISATION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.SYNONYM;
import static uk.gov.moj.cpp.progression.domain.transformation.util.DefendantHelper.transformDefendant;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ApplicantHelper {


    private ApplicantHelper() {
    }

    public static JsonObject transformApplicant(final JsonObject applicant) {
        //Add Mandatory Fields
        final JsonObjectBuilder applicantBuilder = createObjectBuilder()
                .add(ID, applicant.getString(ID));



        if (applicant.containsKey(SYNONYM)) {
            applicantBuilder.add(SYNONYM, applicant.getString(SYNONYM));
        }

        if (applicant.containsKey(ORGANISATION_PERSONS)) {
            applicantBuilder.add(ORGANISATION_PERSONS, applicant.getJsonArray(ORGANISATION_PERSONS));
        }

        if (applicant.containsKey(PERSON_DETAILS)) {
            applicantBuilder.add(PERSON_DETAILS, applicant.getJsonObject(PERSON_DETAILS));
        }

        if (applicant.containsKey(ORGANISATION)) {
            applicantBuilder.add(ORGANISATION, applicant.getJsonObject(ORGANISATION));
        }

        if (applicant.containsKey(PROSECUTING_AUTHORITY)) {
            applicantBuilder.add(PROSECUTING_AUTHORITY, applicant.getJsonObject(PROSECUTING_AUTHORITY));
        }

        if (applicant.containsKey(DEFENDANT)) {
            applicantBuilder.add(DEFENDANT, transformDefendant(applicant.getJsonObject(DEFENDANT)));
        }

        if (applicant.containsKey(REPRESENTATION_ORGANISATION)) {
            applicantBuilder.add(REPRESENTATION_ORGANISATION, applicant.getJsonObject(REPRESENTATION_ORGANISATION));
        }


        return applicantBuilder.build();
    }
}
