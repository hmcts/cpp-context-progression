package uk.gov.moj.cpp.progression.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

    public static MasterDefendant buildDefendantWithPersonDefendant(){
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

    private static Address buildAddress(String line1, String postcode){
        return Address.address()
                .withAddress1(line1)
                .withPostcode(postcode)
                .build();
    }

    public static String getPersonName(){
        return TEST_FIRST_NAME+" "+TEST_LAST_NAME;
    }

    public static ProsecutingAuthority buildProsecutingAuthority(){
        return ProsecutingAuthority.prosecutingAuthority()
                .withName(TEST_PROSECUTION_AUTHORITY_NAME)
                .withProsecutionAuthorityCode(TEST_PROSECUTION_AUTHORITY_NAME)
                .withAddress(buildAddress(TEST_PROSECUTION_AUTHORITY_ADDRESS_LINE1, TEST_PROSECUTION_AUTHORITY_ADDRESS_POSTCODE))
                .build();
    }

    public static CourtApplicationParty buildCourtApplicationPartyWithLegalEntity(){
       return CourtApplicationParty.courtApplicationParty()
               .withMasterDefendant(buildDefendantWithLegalEntity())
               .build();
    }

    public static CourtApplicationParty buildCourtApplicationPartyWithPersonDefendant(){
        return CourtApplicationParty.courtApplicationParty()
                .withMasterDefendant(buildDefendantWithPersonDefendant())
                .build();
    }

    public static CourtApplicationParty buildCourtApplicationPartyWithProsecutionAuthority(){
        return CourtApplicationParty.courtApplicationParty()
                .withProsecutingAuthority(buildProsecutingAuthority())
                .build();
    }

    public static void verifyCompanyName(String resultName){
        assertEquals("Mismatch company's name", TEST_COMPANY_NAME, resultName);
    }

    public static void verifyCompanyAddress(PostalAddress resultAddress){
        assertEquals("Mismatch company's address1", TEST_COMPANY_ADDRESS_LINE1, resultAddress.getLine1());
        assertEquals("Mismatch company's postcode", TEST_COMPANY_ADDRESS_POSTCODE, resultAddress.getPostCode());
    }

    public static void verifyCompanyAddress(Address resultAddress){
        assertEquals("Mismatch company's address1", TEST_COMPANY_ADDRESS_LINE1, resultAddress.getAddress1());
        assertEquals("Mismatch company's postcode", TEST_COMPANY_ADDRESS_POSTCODE, resultAddress.getPostcode());
    }

    public static void verifyPersonName(String resultName){
        assertEquals("Mismatch person's name",TEST_FIRST_NAME+" "+TEST_LAST_NAME, resultName);
    }

    public static void verifyMagistratesCourt(final String ljaCode, final String ljaName) {
        assertEquals("Mismatch LJA Code", LJA_CODE, ljaCode);
        assertEquals("Mismatch LJA NAME", LJA_NAME, ljaName);
    }

    public static void verifyMagistratesCourtAmended(final String ljaCode, final String ljaName, final String amendmentDate) {
        assertEquals("Mismatch LJA Code", LJA_CODE, ljaCode);
        assertEquals("Mismatch LJA NAME", LJA_NAME, ljaName);
        assertNotNull(amendmentDate);


    }

    public static void verifyCrownCourt(final String ljaCode, final String ljaName) {
        assertEquals("Mismatch LJA Code", null, ljaCode);
        assertEquals("Mismatch LJA NAME", null, ljaName);
    }

    public static void verifyCrownCourtAmended(final String ljaCode, final String ljaName, final String amendmentDate) {
        assertEquals("Mismatch LJA Code", null, ljaCode);
        assertEquals("Mismatch LJA NAME", null, ljaName);
        assertNotNull(amendmentDate);
    }

    public static void verifyPersonAddress(PostalAddress resultAddress){
        assertEquals("Mismatch person address1", TEST_PERSON_ADDRESS_LINE1, resultAddress.getLine1());
        assertEquals("Mismatch person postcode", TEST_PERSON_ADDRESS_POSTCODE, resultAddress.getPostCode());
    }

    public static void verifyPersonAddress(Address resultAddress){
        assertEquals("Mismatch person address1", TEST_PERSON_ADDRESS_LINE1, resultAddress.getAddress1());
        assertEquals("Mismatch person postcode", TEST_PERSON_ADDRESS_POSTCODE, resultAddress.getPostcode());
    }

    public static void verifyProsecutionAuthorityName(String resultName){
        assertEquals("Mismatch prosecution authority's name", TEST_PROSECUTION_AUTHORITY_NAME, resultName);
    }

    public static void verifyProsecutionAuthorityAddress(PostalAddress resultAddress){
        assertEquals("Mismatch prosecution authority's address1", TEST_PROSECUTION_AUTHORITY_ADDRESS_LINE1, resultAddress.getLine1());
        assertEquals("Mismatch prosecution authority's postcode", TEST_PROSECUTION_AUTHORITY_ADDRESS_POSTCODE, resultAddress.getPostCode());
    }

    public static void verifyCompanyEmail(String resultEmail){
        assertEquals("Mismatch company's email", TEST_COMPANY_EMAIL, resultEmail);
    }

    public static void verifyPersonEmail(String resultEmail){
        assertEquals("Mismatch person's email", TEST_PERSON_EMAIL, resultEmail);
    }

}
