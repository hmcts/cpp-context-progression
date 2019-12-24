package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.lang.String.format;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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

    public static void verifyPncOnDefendantLevel(final DocumentContext inputDefendant, final JsonObject party) {
        with(party.toString())
                .assertThat("$.pncId", equalTo(((JsonString) inputDefendant.read("$.pncId")).getString()));
    }

    public static void verifyPncOnPersonDefendantLevel(final DocumentContext inputDefendant, final JsonObject party) {
        with(party.toString())
                .assertThat("$.pncId", equalTo(((JsonString) inputDefendant.read("$.personDefendant.pncId")).getString()));
    }

    public static void verifyDefendant(final DocumentContext defendant, final JsonObject party) {

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


    public static void verifyDefendant(final JsonObject inputDefendant, final JsonObject outputCase, boolean isReferCaseToCourt) {
        final JsonString defendantId = inputDefendant.getJsonString("id");
        final DocumentContext parsedInputDefendant = parse(inputDefendant);
        final Optional<JsonObject> outputPartyDefendant = outputParty(defendantId, outputCase);
        verifyDefendant(parsedInputDefendant, outputPartyDefendant.get());
        if (isReferCaseToCourt) {
            verifyPncOnPersonDefendantLevel(parsedInputDefendant, outputPartyDefendant.get());
        } else {
            verifyPncOnDefendantLevel(parsedInputDefendant, outputPartyDefendant.get());

        }
    }

    public static Optional<JsonObject> outputParty(JsonString defendantId, final JsonObject outputCase) {
        final DocumentContext indexData = parse(outputCase);
        final JSONArray outputPartyDefendants = indexData.read("$.parties[*]");
        return outputPartyDefendants.stream()
                    .map(JsonObject.class::cast)
                    .filter(j -> j.getString("partyId").equals(defendantId.getString()))
                    .findFirst();
    }

    public static void verifyAliases(final int partyIndex, final DocumentContext inputPartyDefendant, final JsonObject outputPartyDefendant) {
        with(outputPartyDefendant.toString())
                .assertThat(format("$.aliases[%d].firstName", partyIndex), equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].firstName")).getString()))
                .assertThat(format("$.aliases[%d].middleName", partyIndex), equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].middleName")).getString()))
                .assertThat(format("$.aliases[%d].lastName", partyIndex), equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].lastName")).getString()))
                .assertThat(format("$.aliases[%d].title", partyIndex), equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].title")).getString()))
                .assertThat(format("$.aliases[%d].organisationName", partyIndex), equalTo(((JsonString) inputPartyDefendant.read("$.aliases[0].legalEntityName")).getString()));
    }
}
