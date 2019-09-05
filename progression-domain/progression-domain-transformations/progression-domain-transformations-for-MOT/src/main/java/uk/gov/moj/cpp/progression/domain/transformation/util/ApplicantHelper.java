package uk.gov.moj.cpp.progression.domain.transformation.util;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.DefendantHelper.transformDefendant;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ApplicantHelper {

    public static final String SYNONYM = "synonym";
    public static final String ORGANISATION_PERSONS = "organisationPersons";
    public static final String PERSON_DETAILS = "personDetails";
    public static final String ORGANISATION = "organisation";
    public static final String PROSECUTING_AUTHORITY = "prosecutingAuthority";
    public static final String DEFENDANT = "defendant";
    public static final String REPRESENTATION_ORGANISATION = "representationOrganisation";

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
