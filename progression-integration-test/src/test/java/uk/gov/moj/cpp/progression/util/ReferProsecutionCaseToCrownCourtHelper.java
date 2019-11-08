package uk.gov.moj.cpp.progression.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.json.JsonObject;


public class ReferProsecutionCaseToCrownCourtHelper {

    public static void assertProsecutionCase(final JsonObject prosecutionCase, final String caseId, final String defendantId) {
        assertThat(prosecutionCase.getString("id"), equalTo(caseId));
        assertThat(prosecutionCase.getString("originatingOrganisation"), equalTo("G01FT01AB"));
        assertThat(prosecutionCase.getString("initiationCode"), equalTo("J"));
        assertThat(prosecutionCase.getString("statementOfFacts"), equalTo("You did it"));
        assertThat(prosecutionCase.getString("statementOfFactsWelsh"), equalTo("You did it in Welsh"));
        assertDefendant(prosecutionCase.getJsonArray("defendants").getJsonObject(0), caseId, defendantId);
    }

    public static void assertCaseMarkers(final JsonObject prosecutionCase, final String caseMarkerCode, final String caseMarkerDesc) {
        JsonObject caseMarkerJsonObj = prosecutionCase.getJsonArray("caseMarkers").getJsonObject(0);
        assertThat(caseMarkerJsonObj.getString("markerTypeCode"), equalTo(caseMarkerCode));
        assertThat(caseMarkerJsonObj.getString("markerTypeDescription"), equalTo(caseMarkerDesc));
    }

    public static void assertCaseMarkersIsEmpty(final JsonObject prosecutionCase) {
        assertThat(prosecutionCase.containsKey("caseMarkers"), equalTo(false));
    }

    private static void assertDefendant(final JsonObject defendant, final String caseId, final String defendantId) {
        assertThat(defendant.getString("id"), equalTo(defendantId));
        assertThat(defendant.getString("prosecutionCaseId"), equalTo(caseId));
        assertThat(defendant.getString("prosecutionAuthorityReference"), equalTo("TFL12345-ABC"));
        assertThat(defendant.getString("pncId"), equalTo("1234567"));
        assertOffence(defendant.getJsonArray("offences").getJsonObject(0));
        assertPersons(defendant.getJsonArray("associatedPersons").getJsonObject(0).getJsonObject("person"));
        assertPersonDefendant(defendant.getJsonObject("personDefendant"));
    }

    private static void assertPersonDefendant(final JsonObject personDefendant) {
        assertPersonDetails(personDefendant.getJsonObject("personDetails"));
        assertBailStatus(personDefendant.getJsonObject("bailStatus"));
        assertThat(personDefendant.getString("custodyTimeLimit"), equalTo("2018-01-01"));
        assertThat(personDefendant.getString("driverNumber"), equalTo("AACC12345"));
        assertemployerOrganisation(personDefendant.getJsonObject("employerOrganisation"));
    }

    private static void assertBailStatus(final JsonObject bailStatus) {
        assertThat(bailStatus.getString("id"), equalTo("2593cf09-ace0-4b7d-a746-0703a29f33b5"));
        assertThat(bailStatus.getString("code"), equalTo("C"));
        assertThat(bailStatus.getString("description"), equalTo("Remanded into Custody"));
    }


    private static void assertemployerOrganisation(final JsonObject employerOrganisation) {

        assertThat(employerOrganisation.getString("name"), equalTo("Disneyland Paris"));
        assertThat(employerOrganisation.getString("incorporationNumber"), equalTo("Mickeymouse1"));
    }

    private static void assertPersonDetails(final JsonObject personDetails) {
        assertThat(personDetails.getString("title"), equalTo("MR"));
        assertThat(personDetails.getString("middleName"), equalTo("Jack"));
        assertThat(personDetails.getString("lastName"), equalTo("Kane Junior"));
        assertThat(personDetails.getJsonObject("ethnicity").getString("observedEthnicityId"), equalTo("2daefec3-2f76-8109-82d9-2e60544a6c02"));
        assertThat(personDetails.getJsonObject("ethnicity").getString("selfDefinedEthnicityId"), equalTo("2daefec3-2f76-8109-82d9-2e60544a6c02"));
    }


    private static void assertOffence(final JsonObject offence) {
        assertThat(offence.getString("id"), equalTo("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"));
        assertThat(offence.getString("offenceDefinitionId"), equalTo("490dce00-8591-49af-b2d0-1e161e7d0c36"));
        assertThat(offence.getString("wording"), equalTo("No Travel Card"));
        assertThat(offence.getString("wordingWelsh"), equalTo("No Travel Card In Welsh"));
        assertThat(offence.getString("startDate"), equalTo("2018-01-01"));
        assertThat(offence.getString("endDate"), equalTo("2018-01-01"));
        assertThat(offence.getString("arrestDate"), equalTo("2018-01-01"));
        assertThat(offence.getString("chargeDate"), equalTo("2018-01-01"));
        assertThat(offence.getInt("orderIndex"), equalTo(1));
        assertThat(offence.getInt("count"), equalTo(0));
        assertThat(offence.getString("offenceCode"), equalTo("TTH105HY"));
        assertOffenceFacts(offence.getJsonObject("offenceFacts"));
        assertNotifiedPlea(offence.getJsonObject("notifiedPlea"));

    }

    private static void assertNotifiedPlea(final JsonObject notifiedPlea) {
        assertThat(notifiedPlea.getString("offenceId"), equalTo("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"));
        assertThat(notifiedPlea.getString("notifiedPleaDate"), equalTo("2018-04-01"));
        assertThat(notifiedPlea.getString("notifiedPleaValue"), equalTo("NOTIFIED_GUILTY"));
    }

    private static void assertOffenceFacts(final JsonObject offenceFact) {
        assertThat(offenceFact.getString("vehicleRegistration"), equalTo("AA12345"));
        assertThat(offenceFact.getInt("alcoholReadingAmount"), equalTo(111));
        assertThat(offenceFact.getString("alcoholReadingMethodCode"), equalTo("2222"));
    }

    public static void assertcourtDocuments(final JsonObject courtDocument, final String caseId, final String courtDocumentId, final String materialIdActive) {
        assertThat(courtDocument.getString("courtDocumentId"), equalTo(courtDocumentId));
        assertThat(courtDocument.getJsonObject("documentCategory").getJsonObject("defendantDocument").getString("prosecutionCaseId"), equalTo(caseId));
        assertThat(courtDocument.getString("name"), equalTo("Bank Statement"));
        assertThat(courtDocument.getString("documentTypeId"), equalTo("1b7e6a7a-a571-4ab9-8895-4b1a58424d78"));
        assertThat(courtDocument.getJsonArray("materials").size(), equalTo(2));
        assertMaterials(courtDocument.getJsonArray("materials").getJsonObject(0), materialIdActive);


    }

    private static void assertMaterials(final JsonObject material, final String docId) {
        assertThat(material.getString("id"), equalTo(docId));
        assertThat(material.getString("name"), equalTo("BankStatment.pdf"));
        assertThat(material.getString("uploadDateTime"), equalTo("2018-03-20T16:14:29.000Z"));
    }

    private static void assertPersons(final JsonObject person) {
        assertThat(person.getString("title"), equalTo("MR"));
        assertThat(person.getString("firstName"), equalTo("Harry"));
        assertThat(person.getString("middleName"), equalTo("Jack"));
        assertThat(person.getString("lastName"), equalTo("Kane"));
        assertThat(person.getString("dateOfBirth"), equalTo("1995-01-01"));
        assertThat(person.getString("nationalityId"), equalTo("2daefec3-2f76-8109-82d9-2e60544a6c02"));
        assertThat(person.getString("additionalNationalityId"), equalTo("2daefec3-2f76-8109-82d9-2e60544a6c02"));
        assertThat(person.getString("disabilityStatus"), equalTo("a"));
        assertThat(person.getString("gender"), equalTo("MALE"));
        assertThat(person.getString("interpreterLanguageNeeds"), equalTo("Hindi"));
        assertThat(person.getString("documentationLanguageNeeds"), equalTo("WELSH"));
        assertThat(person.getString("nationalInsuranceNumber"), equalTo("NH222222B"));
        assertThat(person.getString("occupation"), equalTo("Footballer"));
        assertThat(person.getString("occupationCode"), equalTo("F"));
        assertAddress(person.getJsonObject("address"));
        assertContact(person.getJsonObject("contact"));
    }

    private static void assertAddress(final JsonObject address) {
        assertThat(address.getString("address1"), equalTo("22"));
        assertThat(address.getString("address2"), equalTo("Acacia Avenue"));
        assertThat(address.getString("address3"), equalTo("Acacia Town"));
        assertThat(address.getString("address4"), equalTo("Acacia City"));
        assertThat(address.getString("address5"), equalTo("Acacia Country"));
        assertThat(address.getString("postcode"), equalTo("CR7 0AA"));
    }

    private static void assertContact(final JsonObject contact) {
        assertThat(contact.getString("home"), equalTo("123456"));
        assertThat(contact.getString("work"), equalTo("7891011"));
        assertThat(contact.getString("mobile"), equalTo("+45678910"));
        assertThat(contact.getString("primaryEmail"), equalTo("harry.kane@spurs.co.uk"));
        assertThat(contact.getString("secondaryEmail"), equalTo("harry.kane@hotmail.com"));
        assertThat(contact.getString("fax"), equalTo("3425678"));
    }

    public static void assertDefendantRequest(final JsonObject defendantRequest, final String caseId, final String defendantId,
                                              final String referralReasonId) {
        assertThat(defendantRequest.getString("defendantId"), equalTo(defendantId));
        assertThat(defendantRequest.getString("prosecutionCaseId"), equalTo(caseId));
        assertThat(defendantRequest.getString("referralReasonId"), equalTo(referralReasonId));
        assertThat(defendantRequest.getString("summonsType"), equalTo("SJP_REFERRAL"));
    }

}

