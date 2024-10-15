package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.JsonPath.parse;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.AddressVerificationHelper.addressLines;

import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import net.minidev.json.JSONArray;

public class ProsecutionCaseVerificationHelper {
    private static final String PARTIES = "parties";
    private static final String DEFENDANT = "DEFENDANT";

    public static void verifyProsecutionCase(final DocumentContext inputProsecutionCase, final JsonObject outputCase) {

        with(outputCase.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.id")).getString()))
                .assertThat("$.caseReference", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityReference")).getString()))
                .assertThat("$.prosecutingAuthority", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityCode")).getString()))
                .assertThat("$.caseStatus", equalTo("ACTIVE"))
                .assertThat("$._case_type", equalTo("PROSECUTION"));
    }

    public static void verifyDefendant(final JsonObject inputDefendant, final JsonObject outputCase) {
        final JsonString defendantId = inputDefendant.getJsonString("id");

        final DocumentContext indexData = parse(outputCase);
        final JSONArray outputPartyDefendants = indexData.read("$.parties[*]");
        final Optional<JsonObject> outputPartyDefendant = outputPartyDefendants.stream()
                .map(JsonObject.class::cast)
                .filter(j -> j.getString("partyId").equals(defendantId.getString()))
                .findFirst();
        final DocumentContext parsedInputDefendant = parse(inputDefendant);
        verifyDefendant(parsedInputDefendant, outputPartyDefendant.get());
        verifyDefendantAliases(parsedInputDefendant, outputPartyDefendant.get());
    }

    public static void verifyDefendantUpdate(final DocumentContext defendant, final JsonObject party) {

        with(party.toString())
                .assertThat("$.partyId", equalTo(((JsonString) defendant.read("$.id")).getString()))
                .assertThat("$.title", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.title")).getString()))
                .assertThat("$.firstName", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.firstName")).getString()))
                .assertThat("$.middleName", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.middleName")).getString()))
                .assertThat("$.lastName", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.lastName")).getString()))
                .assertThat("$.dateOfBirth", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.dateOfBirth")).getString()))
                .assertThat("$.gender", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.gender")).getString()))
                .assertThat("$.postCode", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.address.postcode")).getString()))
                .assertThat("$.addressLines", equalTo(addressLines(defendant, "$.personDefendant.personDetails.address")))
                .assertThat("$.organisationName", equalTo(((JsonString) defendant.read("$.legalEntityDefendant.organisation.name")).getString()))
                .assertThat("$.arrestSummonsNumber", equalTo(((JsonString) defendant.read("$.personDefendant.arrestSummonsNumber")).getString()))
                .assertThat("$._party_type", equalTo(DEFENDANT))
                .assertThat("$.masterPartyId", equalTo(((JsonString) defendant.read("$.masterDefendantId")).getString()))
                .assertThat("$.croNumber", equalTo(((JsonString) defendant.read("$.croNumber")).getString()))
                .assertThat("$.pncId", equalTo(((JsonString) defendant.read("$.pncId")).getString()))

                //GPE-11009: LAA updates
                .assertThat("$.nationalInsuranceNumber", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.nationalInsuranceNumber")).getString()))
                .assertThat("$.proceedingsConcluded", equalTo(Boolean.valueOf(defendant.read("$.proceedingsConcluded").toString())))

                //Offence 1
                .assertThat("$.offences[0].offenceId", equalTo(((JsonString) defendant.read("$.offences[0].id")).getString()))
                .assertThat("$.offences[0].endDate", equalTo(((JsonString) defendant.read("$.offences[0].endDate")).getString()))
                .assertThat("$.offences[0].startDate", equalTo(((JsonString) defendant.read("$.offences[0].startDate")).getString()))
                .assertThat("$.offences[0].arrestDate", equalTo(((JsonString) defendant.read("$.offences[0].arrestDate")).getString()))
                .assertThat("$.offences[0].chargeDate", equalTo(((JsonString) defendant.read("$.offences[0].chargeDate")).getString()))
                .assertThat("$.offences[0].wording", equalTo(((JsonString) defendant.read("$.offences[0].wording")).getString()))
                .assertThat("$.offences[0].proceedingsConcluded", equalTo(Boolean.valueOf(defendant.read("$.offences[0].proceedingsConcluded").toString())))
                .assertThat("$.offences[0].orderIndex", equalTo(Integer.valueOf(defendant.read("$.offences[0].orderIndex").toString())))
                .assertThat("$.offences[0].offenceCode", equalTo(((JsonString) defendant.read("$.offences[0].offenceCode")).getString()))
                .assertThat("$.offences[0].offenceTitle", equalTo(((JsonString) defendant.read("$.offences[0].offenceTitle")).getString()))
                .assertThat("$.offences[0].offenceLegislation", equalTo(((JsonString) defendant.read("$.offences[0].offenceLegislation")).getString()))
                .assertThat("$.offences[0].dateOfInformation", equalTo(((JsonString) defendant.read("$.offences[0].dateOfInformation")).getString()))
                .assertThat("$.offences[0].modeOfTrial", equalTo(((JsonString) defendant.read("$.offences[0].modeOfTrial")).getString()))
                .assertThat("$.offences[0].laaReference.applicationReference", equalTo(((JsonString) defendant.read("$.offences[0].laaApplnReference.applicationReference")).getString()))
                .assertThat("$.offences[0].laaReference.statusId", equalTo(((JsonString) defendant.read("$.offences[0].laaApplnReference.statusId")).getString()))
                .assertThat("$.offences[0].laaReference.statusCode", equalTo(((JsonString) defendant.read("$.offences[0].laaApplnReference.statusCode")).getString()))
                .assertThat("$.offences[0].laaReference.statusDescription", equalTo(((JsonString) defendant.read("$.offences[0].laaApplnReference.statusDescription")).getString()))

                //Offence 2
                .assertThat("$.offences[1].offenceId", equalTo(((JsonString) defendant.read("$.offences[1].id")).getString()))
                .assertThat("$.offences[1].endDate", equalTo(((JsonString) defendant.read("$.offences[1].endDate")).getString()))
                .assertThat("$.offences[1].startDate", equalTo(((JsonString) defendant.read("$.offences[1].startDate")).getString()))
                .assertThat("$.offences[1].arrestDate", equalTo(((JsonString) defendant.read("$.offences[1].arrestDate")).getString()))
                .assertThat("$.offences[1].chargeDate", equalTo(((JsonString) defendant.read("$.offences[1].chargeDate")).getString()))
                .assertThat("$.offences[1].wording", equalTo(((JsonString) defendant.read("$.offences[1].wording")).getString()))
                .assertThat("$.offences[1].proceedingsConcluded", equalTo(Boolean.valueOf(defendant.read("$.offences[1].proceedingsConcluded").toString())))
                .assertThat("$.offences[1].orderIndex", equalTo(Integer.valueOf(defendant.read("$.offences[1].orderIndex").toString())))
                .assertThat("$.offences[1].offenceCode", equalTo(((JsonString) defendant.read("$.offences[1].offenceCode")).getString()))
                .assertThat("$.offences[1].offenceTitle", equalTo(((JsonString) defendant.read("$.offences[1].offenceTitle")).getString()))
                .assertThat("$.offences[1].offenceLegislation", equalTo(((JsonString) defendant.read("$.offences[1].offenceLegislation")).getString()))
                .assertThat("$.offences[1].dateOfInformation", equalTo(((JsonString) defendant.read("$.offences[1].dateOfInformation")).getString()))
                .assertThat("$.offences[1].modeOfTrial", equalTo(((JsonString) defendant.read("$.offences[1].modeOfTrial")).getString()))
                .assertThat("$.offences[1].laaReference.applicationReference", equalTo(((JsonString) defendant.read("$.offences[1].laaApplnReference.applicationReference")).getString()))
                .assertThat("$.offences[1].laaReference.statusId", equalTo(((JsonString) defendant.read("$.offences[1].laaApplnReference.statusId")).getString()))
                .assertThat("$.offences[1].laaReference.statusCode", equalTo(((JsonString) defendant.read("$.offences[1].laaApplnReference.statusCode")).getString()))
                .assertThat("$.offences[1].laaReference.statusDescription", equalTo(((JsonString) defendant.read("$.offences[1].laaApplnReference.statusDescription")).getString()))

                //RepresentationOrder
                .assertThat("$.representationOrder.applicationReference", equalTo(((JsonString) defendant.read("$.associatedDefenceOrganisation.applicationReference")).getString()))
                .assertThat("$.representationOrder.effectiveFromDate", equalTo(((JsonString) defendant.read("$.associatedDefenceOrganisation.associationStartDate")).getString()))
                .assertThat("$.representationOrder.effectiveToDate", equalTo(((JsonString) defendant.read("$.associatedDefenceOrganisation.associationEndDate")).getString()))
                .assertThat("$.representationOrder.laaContractNumber", equalTo(((JsonString) defendant.read("$.associatedDefenceOrganisation.defenceOrganisation.laaContractNumber")).getString()));
    }


    public static void verifyCaseType(final JsonObject transformedJson) {
        assertEquals("PROSECUTION", transformedJson.getString("_case_type"));
    }

    public static void verifyCaseCreated(final long partySize,
                                         final DocumentContext inputProsecutionCase,
                                         final JsonObject outputCase) {
        final String caseId = ((JsonString) inputProsecutionCase.read("$.prosecutionCase.id")).getString();
        assertEquals(caseId, outputCase.getString("caseId"));

        final JsonArray parties = outputCase.getJsonArray(PARTIES);
        assertNotNull(parties);
        assertThat((long) (parties.size()), is(partySize));
        verifyCaseType(outputCase);
        verifyProsecutionCase(inputProsecutionCase, outputCase);
    }

    public static void verifyCaseDefendant(final DocumentContext inputProsecutionCase, final JsonObject outputCase, boolean isReferCaseToCourt) {
        final JsonObject inputDefendant = inputProsecutionCase.read("$.prosecutionCase.defendants[0]");
        verifyDefendant(inputDefendant, outputCase);
        final DocumentContext parsedInputDefendant = parse(inputDefendant);
        final JsonString defendantId = inputDefendant.getJsonString("id");

        final Optional<JsonObject> outputPartyDefendant = outputParty(defendantId, outputCase);

        if (isReferCaseToCourt) {
            verifyPncOnPersonDefendantLevel(parsedInputDefendant, outputPartyDefendant.get());
        } else {
            verifyPncOnDefendantLevel(parsedInputDefendant, outputPartyDefendant.get());

        }
    }


    public static void verifyDefendantAliases(final DocumentContext inputPartyDefendant, final JsonObject outputPartyDefendant) {
        with(outputPartyDefendant.toString())
                .assertThat("$.aliases[0].firstName", equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].firstName")).getString()))
                .assertThat("$.aliases[0].middleName", equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].middleName")).getString()))
                .assertThat("$.aliases[0].lastName", equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].lastName")).getString()))
                .assertThat("$.aliases[0].title", equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].title")).getString()))
                .assertThat("$.aliases[0].organisationName", equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].legalEntityName")).getString()));
    }

    private static void verifyPncOnDefendantLevel(final DocumentContext inputDefendant, final JsonObject party) {
        with(party.toString())
                .assertThat("$.pncId", equalTo(((JsonString) inputDefendant.read("$.pncId")).getString()));
    }

    private static void verifyPncOnPersonDefendantLevel(final DocumentContext inputDefendant, final JsonObject party) {
        with(party.toString())
                .assertThat("$.pncId", equalTo(((JsonString) inputDefendant.read("$.personDefendant.pncId")).getString()));
    }

    private static Optional<JsonObject> outputParty(JsonString defendantId, final JsonObject outputCase) {
        final DocumentContext indexData = parse(outputCase);
        final JSONArray outputPartyDefendants = indexData.read("$.parties[*]");
        return outputPartyDefendants.stream()
                .map(JsonObject.class::cast)
                .filter(j -> j.getString("partyId").equals(defendantId.getString()))
                .findFirst();
    }

    private static void verifyDefendant(final DocumentContext defendant, final JsonObject party) {
        with(party.toString())
                .assertThat("$.partyId", equalTo(((JsonString) defendant.read("$.id")).getString()))
                .assertThat("$.title", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.title")).getString()))
                .assertThat("$.firstName", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.firstName")).getString()))
                .assertThat("$.middleName", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.middleName")).getString()))
                .assertThat("$.lastName", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.lastName")).getString()))
                .assertThat("$.dateOfBirth", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.dateOfBirth")).getString()))
                .assertThat("$.gender", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.gender")).getString()))
                .assertThat("$.postCode", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.address.postcode")).getString()))
                .assertThat("$.addressLines", equalTo(addressLines(defendant, "$.personDefendant.personDetails.address")))
                .assertThat("$.organisationName", equalTo(((JsonString) defendant.read("$.legalEntityDefendant.organisation.name")).getString()))
                .assertThat("$.arrestSummonsNumber", equalTo(((JsonString) defendant.read("$.personDefendant.arrestSummonsNumber")).getString()))
                .assertThat("$._party_type", equalTo(DEFENDANT))
                //GPE-11010: LAA updates
                .assertThat("$.nationalInsuranceNumber", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.nationalInsuranceNumber")).getString()))
                .assertThat("$.offences[0].offenceId", equalTo(((JsonString) defendant.read("$.offences[0].id")).getString()))
                .assertThat("$.offences[0].endDate", equalTo(((JsonString) defendant.read("$.offences[0].endDate")).getString()))
                .assertThat("$.offences[0].startDate", equalTo(((JsonString) defendant.read("$.offences[0].startDate")).getString()))
                .assertThat("$.offences[0].arrestDate", equalTo(((JsonString) defendant.read("$.offences[0].arrestDate")).getString()))
                .assertThat("$.offences[0].chargeDate", equalTo(((JsonString) defendant.read("$.offences[0].chargeDate")).getString()))
                .assertThat("$.offences[0].wording", equalTo(((JsonString) defendant.read("$.offences[0].wording")).getString()));
//        TODO: need to raise a bug to fix this orderIndex assertion
//        final String orderIndex = defendant.read("$.offences[0].orderIndex").toString();
//        final JsonObject offence = (JsonObject) party.getJsonArray("offences").get(0);
//        assertThat(offence.getJsonString("orderIndex").getString(), equalTo(orderIndex));
//           .assertThat("$.offences[0].orderIndex", equalTo(orderIndex));

    }

}
