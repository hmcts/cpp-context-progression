package uk.gov.moj.cpp.progression.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.moj.cpp.progression.domain.PostalAddress;

public class TestUtils {

    public static final String TEST_COMPANY_EMAIL = "test@company.com";
    public static final String TEST_PERSON_EMAIL = "test@person.com";
    public static final String TEST_COMPANY_NAME = "ABC LTD";
    public static final String TEST_PROSECUTION_AUTHORITY_NAME = "XYZ LTD";
    public static final String TEST_FIRST_NAME = "firstName";
    public static final String TEST_LAST_NAME = "lastName";
    public static final String TEST_COMPANY_ADDRESS_LINE1 = "company address line 1";
    public static final String TEST_PROSECUTION_AUTHORITY_ADDRESS_LINE1 = "prosecution authority address line 1";
    public static final String TEST_COMPANY_ADDRESS_POSTCODE = "CO1 1CO";
    public static final String TEST_PROSECUTION_AUTHORITY_ADDRESS_POSTCODE = "PO1 AU1";
    public static final String TEST_PERSON_ADDRESS_LINE1 = "person address line 1";
    public static final String TEST_PERSON_ADDRESS_POSTCODE = "PE1 1PE";
    public static final String LJA_CODE = "008";
    public static final String LJA_NAME = "Manchester Courts";


    public static MasterDefendant buildDefendantWithLegalEntity() {
        return MasterDefendant.masterDefendant()
                .withLegalEntityDefendant(buildLegalEntityDefendant())
                .build();
    }

    public static MasterDefendant buildDefendantWithPersonDefendant() {
        return MasterDefendant.masterDefendant()
                .withPersonDefendant(buildPersonDefendant())
                .build();
    }


    public static PersonDefendant buildPersonDefendant() {
        return PersonDefendant.personDefendant()
                .withPersonDetails(buildPerson())
                .build();
    }

    public static LegalEntityDefendant buildLegalEntityDefendant() {
        Organisation organisation = Organisation.organisation()
                .withName(TEST_COMPANY_NAME)
                .withContact(buildContact(TEST_COMPANY_EMAIL))
                .withAddress(buildAddress(TEST_COMPANY_ADDRESS_LINE1, TEST_COMPANY_ADDRESS_POSTCODE))
                .build();
        return LegalEntityDefendant.legalEntityDefendant()
                .withOrganisation(organisation)
                .build();
    }

    public static ContactNumber buildContact(String email) {
        return ContactNumber.contactNumber()
                .withPrimaryEmail(email)
                .build();
    }

    private static Person buildPerson() {
        return Person.person()
                .withFirstName(TEST_FIRST_NAME)
                .withLastName(TEST_LAST_NAME)
                .withContact(buildContact(TEST_PERSON_EMAIL))
                .withAddress(buildAddress(TEST_PERSON_ADDRESS_LINE1, TEST_PERSON_ADDRESS_POSTCODE))
                .build();
    }

    private static Address buildAddress(String line1, String postcode) {
        return Address.address()
                .withAddress1(line1)
                .withPostcode(postcode)
                .build();
    }

    public static String getPersonName() {
        return TEST_FIRST_NAME + " " + TEST_LAST_NAME;
    }

    public static ProsecutingAuthority buildProsecutingAuthority() {
        return ProsecutingAuthority.prosecutingAuthority()
                .withName(TEST_PROSECUTION_AUTHORITY_NAME)
                .withProsecutionAuthorityCode(TEST_PROSECUTION_AUTHORITY_NAME)
                .withAddress(buildAddress(TEST_PROSECUTION_AUTHORITY_ADDRESS_LINE1, TEST_PROSECUTION_AUTHORITY_ADDRESS_POSTCODE))
                .build();
    }

    public static CourtApplicationParty buildCourtApplicationPartyWithLegalEntity() {
        return CourtApplicationParty.courtApplicationParty()
                .withMasterDefendant(buildDefendantWithLegalEntity())
                .build();
    }

    public static CourtApplicationParty buildCourtApplicationPartyWithPersonDefendant() {
        return CourtApplicationParty.courtApplicationParty()
                .withMasterDefendant(buildDefendantWithPersonDefendant())
                .build();
    }

    public static CourtApplicationParty buildCourtApplicationPartyWithProsecutionAuthority() {
        return CourtApplicationParty.courtApplicationParty()
                .withProsecutingAuthority(buildProsecutingAuthority())
                .build();
    }

    public static void verifyCompanyName(String resultName) {
        assertEquals(TEST_COMPANY_NAME, resultName, "Mismatch company's name");
    }

    public static void verifyCompanyAddress(PostalAddress resultAddress) {
        assertEquals(TEST_COMPANY_ADDRESS_LINE1, resultAddress.getLine1(), "Mismatch company's address1");
        assertEquals(TEST_COMPANY_ADDRESS_POSTCODE, resultAddress.getPostCode(), "Mismatch company's postcode");
    }

    public static void verifyCompanyAddress(Address resultAddress) {
        assertEquals(TEST_COMPANY_ADDRESS_LINE1, resultAddress.getAddress1(), "Mismatch company's address1");
        assertEquals(TEST_COMPANY_ADDRESS_POSTCODE, resultAddress.getPostcode(), "Mismatch company's postcode");
    }

    public static void verifyPersonName(String resultName) {
        assertEquals(TEST_FIRST_NAME + " " + TEST_LAST_NAME, resultName, "Mismatch person's name");
    }

    public static void verifyMagistratesCourt(final String ljaCode, final String ljaName) {
        assertEquals(LJA_CODE, ljaCode, "Mismatch LJA Code");
        assertEquals(LJA_NAME, ljaName, "Mismatch LJA NAME");
    }

    public static void verifyMagistratesCourtAmended(final String ljaCode, final String ljaName, final String amendmentDate) {
        assertEquals(LJA_CODE, ljaCode, "Mismatch LJA Code");
        assertEquals(LJA_NAME, ljaName, "Mismatch LJA NAME");
        assertNotNull(amendmentDate);


    }

    public static void verifyCrownCourt(final String ljaCode, final String ljaName) {
        assertEquals(null, ljaCode, "Mismatch LJA Code");
        assertEquals(null, ljaName, "Mismatch LJA NAME");
    }

    public static void verifyCrownCourtAmended(final String ljaCode, final String ljaName, final String amendmentDate) {
        assertEquals(null, ljaCode, "Mismatch LJA Code");
        assertEquals(null, ljaName, "Mismatch LJA NAME");
        assertNotNull(amendmentDate);
    }

    public static void verifyPersonAddress(PostalAddress resultAddress) {
        assertEquals(TEST_PERSON_ADDRESS_LINE1, resultAddress.getLine1(), "Mismatch person address1");
        assertEquals(TEST_PERSON_ADDRESS_POSTCODE, resultAddress.getPostCode(), "Mismatch person postcode");
    }

    public static void verifyPersonAddress(Address resultAddress) {
        assertEquals(TEST_PERSON_ADDRESS_LINE1, resultAddress.getAddress1(), "Mismatch person address1");
        assertEquals(TEST_PERSON_ADDRESS_POSTCODE, resultAddress.getPostcode(), "Mismatch person postcode");
    }

    public static void verifyProsecutionAuthorityName(String resultName) {
        assertEquals(TEST_PROSECUTION_AUTHORITY_NAME, resultName, "Mismatch prosecution authority's name");
    }

    public static void verifyProsecutionAuthorityAddress(PostalAddress resultAddress) {
        assertEquals(TEST_PROSECUTION_AUTHORITY_ADDRESS_LINE1, resultAddress.getLine1(), "Mismatch prosecution authority's address1");
        assertEquals(TEST_PROSECUTION_AUTHORITY_ADDRESS_POSTCODE, resultAddress.getPostCode(), "Mismatch prosecution authority's postcode");
    }

    public static void verifyCompanyEmail(String resultEmail) {
        assertEquals(TEST_COMPANY_EMAIL, resultEmail, "Mismatch company's email");
    }

    public static void verifyPersonEmail(String resultEmail) {
        assertEquals(TEST_PERSON_EMAIL, resultEmail, "Mismatch person's email");
    }

}
