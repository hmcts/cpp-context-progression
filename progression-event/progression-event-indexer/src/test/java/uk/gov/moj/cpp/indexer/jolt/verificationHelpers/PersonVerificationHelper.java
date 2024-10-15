package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertPersonDetails(personDetails, party, organisationName);

        final String defendantId = defendantJsonObject.getString("id");
        assertEquals(defendantId.toUpperCase(), party.getString("partyId").toUpperCase());

        final String pncId = defendantJsonObject.getString("pncId");
        assertEquals(pncId, party.getString("pncId"));

        final String masterDefendantId = defendantJsonObject.getString("masterDefendantId");
        assertEquals(masterDefendantId.toUpperCase(), party.getString("masterPartyId").toUpperCase());

        final String croNumber = defendantJsonObject.getString("croNumber");
        assertEquals(croNumber, party.getString("croNumber"));

        final String courtProceedingsInitiated = defendantJsonObject.getString("courtProceedingsInitiated");
        assertEquals(courtProceedingsInitiated, party.getString("courtProceedingsInitiated"));

        final String arrestSummonsNumber = personDefendant.getString("arrestSummonsNumber");
        assertEquals(arrestSummonsNumber, party.getString("arrestSummonsNumber"));


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
                party.getString("postCode"),
                party.getJsonObject("defendantAddress"));
    }

    public static void assertOrganisationDetails(final JsonObject personDetails, final JsonObject party) {

        final JsonObject address = personDetails.getJsonObject("address");
        final String addressLines = party.getString("addressLines");
        final String postCode = party.getString("postCode");
        final JsonObject defendantAdress = party.getJsonObject("defendantAddress");
        assertAddressDetails(address, addressLines, postCode, defendantAdress);
    }
}
