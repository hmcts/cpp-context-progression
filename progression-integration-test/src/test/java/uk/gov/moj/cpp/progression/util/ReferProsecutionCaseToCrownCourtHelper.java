package uk.gov.moj.cpp.progression.util;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class ReferProsecutionCaseToCrownCourtHelper {
    private static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

    public static Matcher<? super ReadContext>[] getProsecutionCaseMatchers(final String caseId, final String defendantId) {
        return getProsecutionCaseMatchers(caseId, defendantId, emptyList());

    }

    public static Matcher<? super ReadContext>[] getCotrTrialHearingsMatchers(final String hearingId, List<Matcher<? super ReadContext>> additionalMatchers) {
        List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.trialHearings[0].id", is(hearingId))
        );
        matchers.addAll(additionalMatchers);
        return matchers.toArray(new Matcher[0]);
    }

    public static Matcher<? super ReadContext>[] getCotrDetailsMatchers(final String cotrId, final String hearingId, List<Matcher<? super ReadContext>> additionalMatchers) {
        List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.cotrDetails[0].id", is(cotrId)),
                withJsonPath("$.cotrDetails[0].hearingId", is(hearingId))
        );
        matchers.addAll(additionalMatchers);
        return matchers.toArray(new Matcher[0]);
    }

    public static List<Matcher<? super ReadContext>> getYouthReportingRestrictionsMatchers(final LocalDate orderedDate, final LocalDate dateOfBirth, final int expectedRestrictionsCount) {
        List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.add(withJsonPath("$.prosecutionCase.defendants[0].offences[*].reportingRestrictions", hasSize(equalTo(expectedRestrictionsCount))));
        matchers.add(withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is(YOUTH_RESTRICTION)));
        matchers.add(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.dateOfBirth", is(dateOfBirth.toString())));
        matchers.add(withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", is(orderedDate.toString())));
        return matchers;
    }

    public static Matcher<? super ReadContext>[] getProsecutionCaseMatchersWithOffence(final String caseId, final String defendantId, final List<Matcher<? super ReadContext>> additionalMatchers) {
        final List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.originatingOrganisation", is("G01FT01AB")),
                withJsonPath("$.prosecutionCase.initiationCode", is("J")),
                withJsonPath("$.prosecutionCase.statementOfFacts", is("You did it")),
                withJsonPath("$.prosecutionCase.statementOfFactsWelsh", is("You did it in Welsh"))
        );

        matchers.addAll(getDefendantMatchers(caseId, defendantId));
        matchers.addAll(getDefendantOffenceMatchersWithOffenceDateCode());
        matchers.addAll(getOffenceFactMatchers());
        matchers.addAll(getNotifyPleatMatchers());
        matchers.addAll(getPersonMatchers());
        matchers.addAll(getPersonAddressMatchers());
        matchers.addAll(getPersonContactDetailsMatchers());
        matchers.addAll(getPersonDefendantMatchers());

        matchers.addAll(additionalMatchers);

        return matchers.toArray(new Matcher[0]);

    }

    public static List<Matcher<? super ReadContext>> getDefendantOffenceMatchersWithOffenceDateCode() {
        return newArrayList(
                // defendant offence assertion
                withJsonPath("$.prosecutionCase.defendants[*].offences[*].id", hasItem(equalTo("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"))),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceDefinitionId", is("490dce00-8591-49af-b2d0-1e161e7d0c36")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].wording", is("No Travel Card")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].wordingWelsh", is("No Travel Card In Welsh")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].startDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].endDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].arrestDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].chargeDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].orderIndex", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(0)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceDateCode", is(4)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY"))
        );
    }

    public static Matcher<? super ReadContext>[] getProsecutionCaseMatchers(final String caseId, final String defendantId, List<Matcher<? super ReadContext>> additionalMatchers) {
        List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.originatingOrganisation", is("G01FT01AB")),
                withJsonPath("$.prosecutionCase.initiationCode", is("J")),
                withJsonPath("$.prosecutionCase.statementOfFacts", is("You did it")),
                withJsonPath("$.prosecutionCase.statementOfFactsWelsh", is("You did it in Welsh"))
        );

        matchers.addAll(getDefendantMatchers(caseId, defendantId));
        matchers.addAll(getDefendantOffenceMatchers());
        matchers.addAll(getOffenceFactMatchers());
        matchers.addAll(getNotifyPleatMatchers());
        matchers.addAll(getPersonMatchers());
        matchers.addAll(getPersonAddressMatchers());
        matchers.addAll(getPersonContactDetailsMatchers());
        matchers.addAll(getPersonDefendantMatchers());

        matchers.addAll(additionalMatchers);

        return matchers.toArray(new Matcher[0]);

    }

    public static Matcher<? super ReadContext>[] getCivilProsecutionCaseMatchers(final String caseId, final String defendantId, List<Matcher<? super ReadContext>> additionalMatchers) {
        List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.originatingOrganisation", is("G01FT01AB")),
                withJsonPath("$.prosecutionCase.initiationCode", is("O")),
                withJsonPath("$.prosecutionCase.statementOfFacts", is("You did it")),
                withJsonPath("$.prosecutionCase.statementOfFactsWelsh", is("You did it in Welsh"))
        );

        matchers.addAll(getDefendantMatchers(caseId, defendantId));
        matchers.addAll(getDefendantOffenceMatchers());
        matchers.addAll(getOffenceFactMatchers());
        matchers.addAll(getNotifyPleatMatchers());
        matchers.addAll(getPersonMatchers());
        matchers.addAll(getPersonAddressMatchers());
        matchers.addAll(getPersonContactDetailsMatchers());
        matchers.addAll(getPersonDefendantMatchers());

        matchers.addAll(additionalMatchers);

        return matchers.toArray(new Matcher[0]);

    }

    public static Matcher<? super ReadContext>[] getProsecutionCaseMatchersForLegalEntity(final String caseId, final String defendantId, List<Matcher<? super ReadContext>> additionalMatchers) {
        List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.originatingOrganisation", is("G01FT01AB")),
                withJsonPath("$.prosecutionCase.initiationCode", is("J")),
                withJsonPath("$.prosecutionCase.statementOfFacts", is("You did it")),
                withJsonPath("$.prosecutionCase.statementOfFactsWelsh", is("You did it in Welsh"))
        );

        matchers.addAll(getDefendantMatchers(caseId, defendantId));
        matchers.addAll(getDefendantOffenceMatchers());
        matchers.addAll(getOffenceFactMatchers());
        matchers.addAll(getLegalEntityMatchers());
        matchers.addAll(getNotifyPleatMatchers());
        matchers.addAll(additionalMatchers);

        return matchers.toArray(new Matcher[0]);

    }

    public static List<Matcher<? super ReadContext>> getDefendantMatchers(final String caseId, final String defendantId) {
        return newArrayList(
                // defendant assertion
                withJsonPath("$.prosecutionCase.defendants[*].id", hasItem(equalTo(defendantId))),
                withJsonPath("$.prosecutionCase.defendants[*].prosecutionCaseId", hasItem(equalTo(caseId))),
                withJsonPath("$.prosecutionCase.defendants[*].prosecutionAuthorityReference", hasItem(equalTo("TFL12345-ABC"))),
                withJsonPath("$.prosecutionCase.defendants[*].pncId", hasItem(equalTo("1234567")))
        );
    }

    public static List<Matcher<? super ReadContext>> getDefendantOffenceMatchers() {
        return newArrayList(
                // defendant offence assertion
                withJsonPath("$.prosecutionCase.defendants[*].offences[*].id", hasItem(equalTo("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"))),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceDefinitionId", is("490dce00-8591-49af-b2d0-1e161e7d0c36")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].wording", is("No Travel Card")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].wordingWelsh", is("No Travel Card In Welsh")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].startDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].endDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].arrestDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].chargeDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].orderIndex", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(0)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getOffenceFactMatchers() {
        return newArrayList(
                // offence facts
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceFacts.vehicleRegistration", is("AA12345")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceFacts.alcoholReadingAmount", is(111)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceFacts.alcoholReadingMethodCode", is("2222"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getLegalEntityMatchers() {
        return newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].legalEntityDefendant.organisation.name", is("Organisation Name"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getNotifyPleatMatchers() {
        return newArrayList(
                // notified plea
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].notifiedPlea.offenceId", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].notifiedPlea.notifiedPleaDate", is("2018-04-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].notifiedPlea.notifiedPleaValue", is("NOTIFIED_GUILTY"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getPersonMatchers() {
        return newArrayList(
                // assert person
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.title", is("DR")),
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
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.occupationCode", is("F"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getPersonAddressMatchers() {
        return newArrayList(
                // person address
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address1", is("22")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address2", is("Acacia Avenue")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address3", is("Acacia Town")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address4", is("Acacia City")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.address5", is("Acacia Country")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.address.postcode", is("CR7 0AA"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getPersonContactDetailsMatchers() {
        return newArrayList(
                // person contact details
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.home", is("123456")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.work", is("7891011")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.mobile", is("+45678910")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.primaryEmail", is("harry.kane@spurs.co.uk")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.secondaryEmail", is("harry.kane@hotmail.com")),
                withJsonPath("$.prosecutionCase.defendants[0].associatedPersons[0].person.contact.fax", is("3425678"))
        );
    }

    public static ArrayList<Matcher<? super ReadContext>> getPersonDefendantMatchers() {
        return newArrayList(
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
    }

}

