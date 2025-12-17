package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static junit.framework.TestCase.assertEquals;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.AddressVerificationHelper.assertAddressDetails;

import javax.json.JsonObject;

public class PersonVerificationHelper {

    public static void assertApplicantDetails(final JsonObject personDetails, final JsonObject party, final String organisationName) {
        assertPersonDetails(personDetails, party, organisationName);
    }

    public static void assertRespondantDetails(final JsonObject personDetails, final JsonObject party, final String organisationName) {
        assertPersonDetails(personDetails, party, organisationName);
        assertAddressDetailsForSpecificTypeOfPerson(personDetails, party);
    }

    private static void assertPersonDetails(final JsonObject personDetails, final JsonObject party, final String organisationName) {

        final String applicantFirstName = personDetails.getString("firstName", null);
        final String applicantMiddleName = personDetails.getString("middleName", null);
        final String applicantLastName = personDetails.getString("lastName", null);
        final String applicantTitle = personDetails.getString("title", null);
        final String applicantDateOfBirth = personDetails.getString("dateOfBirth", null);
        final String applicantGender = personDetails.getString("gender", null);

        assertEquals(applicantFirstName, party.getString("firstName", null));
        assertEquals(applicantMiddleName, party.getString("middleName", null));
        assertEquals(applicantLastName, party.getString("lastName", null));

        assertEquals(applicantTitle, party.getString("title", null));
        assertEquals(applicantDateOfBirth, party.getString("dateOfBirth", null));
        assertEquals(applicantGender, party.getString("gender", null));
    }

    private static void assertAddressDetailsForSpecificTypeOfPerson(final JsonObject personDetails, final JsonObject party) {
        assertAddressDetails(personDetails.getJsonObject("address"), party.getString("addressLines")
                , party.getString("postCode"), party.getJsonObject("defendantAddress"));
    }

    public static void assertOrganisationDetails(final JsonObject personDetails, final JsonObject party) {
        assertAddressDetails(personDetails.getJsonObject("address"), party.getString("addressLines")
                , party.getString("postCode"),party.getJsonObject("defendantAddress"));
    }
}
