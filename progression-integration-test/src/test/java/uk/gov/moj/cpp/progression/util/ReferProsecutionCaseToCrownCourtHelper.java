package uk.gov.moj.cpp.progression.util;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;

import java.util.Collections;
import java.util.List;

import org.hamcrest.Matcher;
import org.hamcrest.collection.IsCollectionWithSize;

public class ReferProsecutionCaseToCrownCourtHelper {

    public static Matcher[] getProsecutionCaseMatchers(final String caseId, final String defendantId) {
        return getProsecutionCaseMatchers(caseId, defendantId, Collections.emptyList());

    }

    public static Matcher[] getProsecutionCaseMatchers(final String caseId, final String defendantId, List<Matcher> additionalMatchers) {
        List<Matcher> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.originatingOrganisation", is("G01FT01AB")),
                withJsonPath("$.prosecutionCase.initiationCode", is("J")),
                withJsonPath("$.prosecutionCase.statementOfFacts", is("You did it")),
                withJsonPath("$.prosecutionCase.statementOfFactsWelsh", is("You did it in Welsh")),

                // defendant assertion
                withJsonPath("$.prosecutionCase.defendants[0].id", is(defendantId)),
                withJsonPath("$.prosecutionCase.defendants[0].prosecutionCaseId", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].prosecutionAuthorityReference", is("TFL12345-ABC")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),

                // defendant offence assertion
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceDefinitionId", is("490dce00-8591-49af-b2d0-1e161e7d0c36")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].wording", is("No Travel Card")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].wordingWelsh", is("No Travel Card In Welsh")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].startDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].endDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].arrestDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].chargeDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].orderIndex", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(0)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")),

                // offence facts
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceFacts.vehicleRegistration", is("AA12345")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceFacts.alcoholReadingAmount", is(111)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceFacts.alcoholReadingMethodCode", is("2222")),

                // notified plea
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].notifiedPlea.offenceId", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].notifiedPlea.notifiedPleaDate", is("2018-04-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].notifiedPlea.notifiedPleaValue", is("NOTIFIED_GUILTY")),

                // assert person
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.title", is("DR")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.firstName", is("Harry")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.middleName", is("Jack")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.lastName", is("Kane")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.dateOfBirth", is("1995-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.nationalityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.additionalNationalityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.disabilityStatus", is("a")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.gender", is("MALE")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.interpreterLanguageNeeds", is("Hindi")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.documentationLanguageNeeds", is("WELSH")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.nationalInsuranceNumber", is("NH222222B")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.occupation", is("Footballer")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.occupationCode", is("F")),

                // person address
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address1", is("22")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address2", is("Acacia Avenue")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address3", is("Acacia Town")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address4", is("Acacia City")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address5", is("Acacia Country")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.postcode", is("CR7 0AA")),

                // person contact details
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.home", is("123456")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.work", is("7891011")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.mobile", is("+45678910")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.primaryEmail", is("harry.kane@spurs.co.uk")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.secondaryEmail", is("harry.kane@hotmail.com")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.fax", is("3425678")),

                // person defendant details
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.title", is("DR")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.middleName", is("Jack")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.lastName", is("Kane Junior")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.ethnicity.observedEthnicityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.ethnicity.selfDefinedEthnicityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),


                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.id", is("2593cf09-ace0-4b7d-a746-0703a29f33b5")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.code", is("C")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.description", is("Remanded into Custody")),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.driverNumber", is("AACC12345")),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.employerOrganisation.name", is("Disneyland Paris")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.employerOrganisation.incorporationNumber", is("Mickeymouse1"))
        );

        matchers.addAll(additionalMatchers);

        return matchers.toArray(new Matcher[0]);

    }


    public static List<Matcher> getCourtDocumentMatchers(final String caseId, final String courtDocumentId, final String materialIdActive, final int position) {
        return newArrayList(
                withJsonPath("$.courtDocuments[" + position + "].courtDocumentId", is(courtDocumentId)),
                withJsonPath("$.courtDocuments[" + position + "].documentCategory.defendantDocument.prosecutionCaseId", is(caseId)),
                withJsonPath("$.courtDocuments[" + position + "].name", is("Bank Statement")),
                withJsonPath("$.courtDocuments[" + position + "].documentTypeId", is("1b7e6a7a-a571-4ab9-8895-4b1a58424d78")),
                withJsonPath("$.courtDocuments[" + position + "].materials", IsCollectionWithSize.hasSize(2)),
                withJsonPath("$.courtDocuments[" + position + "].materials[0].id", is(materialIdActive)),
                withJsonPath("$.courtDocuments[" + position + "].materials[0].name", is("BankStatment.pdf")),
                withJsonPath("$.courtDocuments[" + position + "].materials[0].uploadDateTime", is("2018-03-20T16:14:29.000Z"))

        );
    }
}

