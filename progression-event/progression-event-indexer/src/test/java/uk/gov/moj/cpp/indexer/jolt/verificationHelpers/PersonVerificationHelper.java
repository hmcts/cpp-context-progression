package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.AddressVerificationHelper.assertAddressDetails;

import javax.json.JsonObject;

public class PersonVerificationHelper {

    public static void assertApplicantDetails(final JsonObject personDetails, final JsonObject party, final String organisationName) {
        assertPersonDetails(personDetails, party, organisationName);
    }

    public static void assertRespondantDetails(final JsonObject personDetails, final JsonObject party, final String organisationName) {
        assertPersonDetails(personDetails, party, organisationName);
    }

    public static void assertDefendantDetails(final JsonObject defendantJsonObject, final JsonObject party, final String organisationName) {
        assertNotNull(party);
        assertNotNull(defendantJsonObject);

        final JsonObject personDefendant = defendantJsonObject.getJsonObject("personDefendant");
        final JsonObject personDetails = personDefendant.getJsonObject("personDetails");
        final String defendantId = defendantJsonObject.getString("id");
        assertEquals(defendantId.toUpperCase(), party.getString("partyId").toUpperCase());
        assertPersonDetails(personDetails, party, organisationName);
        final String arrestSummonsNumber = personDefendant.getString("arrestSummonsNumber");
        final String pncId = defendantJsonObject.getString("pncId");
        assertEquals(arrestSummonsNumber, party.getString("arrestSummonsNumber"));
        assertEquals(pncId, party.getString("pncId"));
    }

    private static void assertPersonDetails(final JsonObject personDetails, final JsonObject party, final String organisationName) {
        assertPersonDetails(personDetails, party);
        assertEquals(organisationName, party.getString("organisationName"));
    }

    public static void assertPersonDetails(final JsonObject personDetails, final JsonObject party) {

        final String applicantFirstName = personDetails.getString("firstName");
        final String applicantMiddleName = personDetails.getString("middleName");
        final String applicantLastName = personDetails.getString("lastName");
        final String applicantTitle = personDetails.getString("title");
        final String applicantDateOfBirth = personDetails.getString("dateOfBirth");
        final String applicantGender = personDetails.getString("gender");

        assertEquals(applicantFirstName, party.getString("firstName"));
        assertEquals(applicantMiddleName, party.getString("middleName"));
        assertEquals(applicantLastName, party.getString("lastName"));
        assertEquals(applicantTitle, party.getString("title"));
        assertEquals(applicantDateOfBirth, party.getString("dateOfBirth"));
        assertEquals(applicantGender, party.getString("gender"));
        assertAddressDetails(personDetails.getJsonObject("address"),
                party.getString("addressLines"),
                party.getString("postCode"));
    }

    public static void assertOrganisationDetails(final JsonObject personDetails, final JsonObject party) {

        assertAddressDetails(personDetails.getJsonObject("address"), party.getString("addressLines")
                , party.getString("postCode"));
    }
}
