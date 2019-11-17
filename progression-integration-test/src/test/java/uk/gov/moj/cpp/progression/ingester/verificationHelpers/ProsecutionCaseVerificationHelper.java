package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.valueOf;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.AddressVerificationHelper.addressLines;

import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonNumber;
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
                .assertThat("$.pncId", equalTo(((JsonString) defendant.read("$.personDefendant.pncId")).getString()))
                .assertThat("$.arrestSummonsNumber", equalTo(((JsonString) defendant.read("$.personDefendant.arrestSummonsNumber")).getString()))
                .assertThat("$._party_type", equalTo(DEFENDANT))

                //GPE-11010: LAA updates
                .assertThat("$.nationalInsuranceNumber", equalTo(((JsonString) defendant.read("$.personDefendant.personDetails.nationalInsuranceNumber")).getString()))

                .assertThat("$.offences[0].offenceId", equalTo(((JsonString) defendant.read("$.offences[0].id")).getString()))
                .assertThat("$.offences[0].endDate", equalTo(((JsonString) defendant.read("$.offences[0].endDate")).getString()))
                .assertThat("$.offences[0].startDate", equalTo(((JsonString) defendant.read("$.offences[0].startDate")).getString()))
                .assertThat("$.offences[0].arrestDate", equalTo(((JsonString) defendant.read("$.offences[0].arrestDate")).getString()))
                .assertThat("$.offences[0].chargeDate", equalTo(((JsonString) defendant.read("$.offences[0].chargeDate")).getString()))
                .assertThat("$.offences[0].wording", equalTo(((JsonString) defendant.read("$.offences[0].wording")).getString()))
                .assertThat("$.offences[0].orderIndex", equalTo(valueOf(((JsonNumber) defendant.read("$.offences[0].orderIndex")).intValue())));

                if(includedLAAOffenceDetails(party)){
                    with(party.toString())
                            .assertThat("$.offences[0].offenceCode", equalTo(((JsonString) defendant.read("$.offences[0].offenceCode")).getString()))
                            .assertThat("$.offences[0].offenceTitle", equalTo(((JsonString) defendant.read("$.offences[0].offenceTitle")).getString()))
                            .assertThat("$.offences[0].offenceLegislation", equalTo(((JsonString) defendant.read("$.offences[0].offenceLegislation")).getString()))
                            .assertThat("$.offences[0].proceedingsConcluded", equalTo(parseBoolean((defendant.read("$.offences[0].proceedingsConcluded")).toString())))
                            .assertThat("$.offences[0].dateOfInformation", equalTo(((JsonString) defendant.read("$.offences[0].dateOfInformation")).getString()))
                            .assertThat("$.offences[0].modeOfTrial", equalTo(((JsonString) defendant.read("$.offences[0].modeOfTrial")).getString()))

                            .assertThat("$.offences[0].laaReference.laaApplicationReference", equalTo(((JsonString) defendant.read("$.offences[0].laaReference.laaApplicationReference")).getString()))
                            .assertThat("$.offences[0].laaReference.statusId", equalTo(((JsonString) defendant.read("$.offences[0].laaReference.statusId")).getString()))
                            .assertThat("$.offences[0].laaReference.statusCode", equalTo(((JsonString) defendant.read("$.offences[0].laaReference.statusCode")).getString()))
                            .assertThat("$.offences[0].laaReference.statusDescription", equalTo(((JsonString) defendant.read("$.offences[0].laaReference.statusDescription")).getString()))
                            .assertThat("$.offences[0].laaReference.effectiveFromDate", equalTo(((JsonString) defendant.read("$.offences[0].laaReference.effectiveFromDate")).getString()))
                            .assertThat("$.offences[0].laaReference.effectiveToDate", equalTo(((JsonString) defendant.read("$.offences[0].laaReference.effectiveToDate")).getString()))
                            .assertThat("$.offences[0].laaReference.laaRepresentativeAccountNumber", equalTo(((JsonString) defendant.read("$.offences[0].laaReference.laaRepresentativeAccountNumber")).getString()));
                }

    }

    private static boolean includedLAAOffenceDetails(final JsonObject party) {
        return party.getJsonArray("offences").getJsonObject(0).getString("offenceCode").isEmpty() ||
                party.getJsonArray("offences").getJsonObject(0).getString("offenceTitle").isEmpty() ||
                party.getJsonArray("offences").getJsonObject(0).getString("offenceLegislation").isEmpty() ||
                party.getJsonArray("offences").getJsonObject(0).getString("modeOfTrial").isEmpty();

        //The attributes below need be updated on the schemas of the commands and queries by Gajanan's team/legalaid implementations
        /*
        1. referredOffence schema in core domain team/legaliaid branch does not have LAAReference yet.
         */

//                party.getJsonArray("offences").getJsonObject(0).getString("proceedingsConcluded") != null||
//                party.getJsonArray("offences").getJsonObject(0).getString("dateOfInformation").isEmpty() ||
//                party.getJsonArray("offences").getJsonObject(0).getString("laaReference.laaApplicationReference").isEmpty() ||
//                party.getJsonArray("offences").getJsonObject(0).getString("laaReference.statusId").isEmpty() ||
//                party.getJsonArray("offences").getJsonObject(0).getString("laaReference.statusCode").isEmpty() ||
//                party.getJsonArray("offences").getJsonObject(0).getString("laaReference.statusDescription").isEmpty() ||
//                party.getJsonArray("offences").getJsonObject(0).getString("laaReference.effectiveFromDate").isEmpty() ||
//                party.getJsonArray("offences").getJsonObject(0).getString("laaReference.effectiveToDate").isEmpty() ||
//                party.getJsonArray("offences").getJsonObject(0).getString("laaReference.laaRepresentativeAccountNumber").isEmpty();
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
                .assertThat("$.pncId", equalTo(((JsonString) defendant.read("$.pncId")).getString()))
                .assertThat("$.arrestSummonsNumber", equalTo(((JsonString) defendant.read("$.personDefendant.arrestSummonsNumber")).getString()))
                .assertThat("$._party_type", equalTo(DEFENDANT));
    }

    public static void verifyCaseType(final JsonObject transformedJson) {
        assertEquals("PROSECUTION", transformedJson.getString("_case_type"));
    }

    public static void verifyCaseCreated(final long partySize,
                                         final DocumentContext inputProsectionCase,
                                         final JsonObject outputCase) {
        final String caseId = ((JsonString) inputProsectionCase.read("$.prosecutionCase.id")).getString();
        assertEquals(caseId, outputCase.getString("caseId"));

        final JsonArray parties = outputCase.getJsonArray(PARTIES);
        assertNotNull(parties);
        assertThat((long) (parties.size()), is(partySize));
        verifyCaseType(outputCase);
        verifyProsecutionCase(inputProsectionCase, outputCase);
    }

    public static void verifyCaseDefendant(final DocumentContext inputProsectionCase, final JsonObject outputCase) {
        final JsonObject inputDefendant = inputProsectionCase.read("$.prosecutionCase.defendants[0]");
        verifyDefendant(inputDefendant, outputCase);
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

    public static void verifyDefendantAliases(final DocumentContext inputPartyDefendant, final JsonObject outputPartyDefendant) {
        with(outputPartyDefendant.toString())
                .assertThat("$.aliases[0].firstName", equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].firstName")).getString()))
                .assertThat("$.aliases[0].middleName", equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].middleName")).getString()))
                .assertThat("$.aliases[0].lastName", equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].lastName")).getString()))
                .assertThat("$.aliases[0].title", equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].title")).getString()))
                .assertThat("$.aliases[0].organisationName", equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].legalEntityName")).getString()));
    }

}
