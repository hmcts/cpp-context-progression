package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.AddressVerificationHelper.addressLines;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;

public class ProsecutionCaseVerificationHelper {

    public static void verifyProsecutionCase(final DocumentContext inputProsecutionCase, final JsonObject outputCase) {

        with(outputCase.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.id")).getString()))
                .assertThat("$.caseReference", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityReference")).getString()))
                .assertThat("$.prosecutingAuthority", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityCode")).getString()))
                .assertThat("$.caseStatus", equalTo("ACTIVE"))
                .assertThat("$._case_type", equalTo("PROSECUTION"));
    }

    public static void verifyDefendants(final DocumentContext inputProsecutionCase, final JsonObject outputCase) {

        with(outputCase.toString())
                .assertThat("$.parties[*]", hasSize(1))
                .assertThat("$.parties[0].partyId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].id")).getString()))
                .assertThat("$.parties[0].title", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.title")).getString()))
                .assertThat("$.parties[0].firstName", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName")).getString()))
                .assertThat("$.parties[0].middleName", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.middleName")).getString()))
                .assertThat("$.parties[0].lastName", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.lastName")).getString()))
                .assertThat("$.parties[0].dateOfBirth", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.dateOfBirth")).getString()))
                .assertThat("$.parties[0].gender", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.gender")).getString()))
                .assertThat("$.parties[0].postCode", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.address.postcode")).getString()))
                .assertThat("$.parties[0].addressLines", equalTo(addressLines(inputProsecutionCase, "$.prosecutionCase.defendants[0].personDefendant.personDetails.address")))
                .assertThat("$.parties[0].pncId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].pncId")).getString()))
                .assertThat("$.parties[0].arrestSummonsNumber", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.arrestSummonsNumber")).getString()))
                .assertThat("$.parties[0].organisationName", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].legalEntityDefendant.organisation.name")).getString()))
                .assertThat("$.parties[0]._party_type", equalTo("DEFENDANT"))
                .assertThat("$.parties[0].aliases[0].firstName", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].aliases[0].firstName")).getString()))
                .assertThat("$.parties[0].aliases[0].middleName", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].aliases[0].middleName")).getString()))
                .assertThat("$.parties[0].aliases[0].lastName", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].aliases[0].lastName")).getString()))
                .assertThat("$.parties[0].aliases[0].title", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].aliases[0].title")).getString()))
                .assertThat("$.parties[0].aliases[0].organisationName", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].aliases[0].legalEntityName")).getString()));
    }

    public static void verifyDefendantsForDefendantUpdated(final DocumentContext inputProsecutionCase, final JsonObject outputCase) {

        with(outputCase.toString())
                .assertThat("$.parties[*]", hasSize(1))
                .assertThat("$.parties[0].partyId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.id")).getString()))
                .assertThat("$.parties[0].title", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.title")).getString()))
                .assertThat("$.parties[0].firstName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.firstName")).getString()))
                .assertThat("$.parties[0].middleName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.middleName")).getString()))
                .assertThat("$.parties[0].lastName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.lastName")).getString()))
                .assertThat("$.parties[0].dateOfBirth", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.dateOfBirth")).getString()))
                .assertThat("$.parties[0].gender", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.gender")).getString()))
                .assertThat("$.parties[0].postCode", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.address.postcode")).getString()))
                .assertThat("$.parties[0].addressLines", equalTo(addressLines(inputProsecutionCase, "$.defendant.personDefendant.personDetails.address")))
                .assertThat("$.parties[0].pncId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.pncId")).getString()))
                .assertThat("$.parties[0].arrestSummonsNumber", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.arrestSummonsNumber")).getString()))
                .assertThat("$.parties[0].organisationName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.legalEntityDefendant.organisation.name")).getString()))
                .assertThat("$.parties[0]._party_type", equalTo("DEFENDANT"))
                .assertThat("$.parties[0].aliases[0].firstName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.aliases[0].firstName")).getString()))
                .assertThat("$.parties[0].aliases[0].middleName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.aliases[0].middleName")).getString()))
                .assertThat("$.parties[0].aliases[0].lastName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.aliases[0].lastName")).getString()))
                .assertThat("$.parties[0].aliases[0].title", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.aliases[0].title")).getString()))
                .assertThat("$.parties[0].aliases[0].organisationName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.aliases[0].legalEntityName")).getString()));
    }

    public static void verifyAdditionalAliasesOnOnDefendantUpdate(final JsonObject outputCase) {
        with(outputCase.toString())
                .assertThat("$.parties[0].aliases[0].firstName", equalTo("Jim"))
                .assertThat("$.parties[0].aliases[0].middleName", equalTo("John"))
                .assertThat("$.parties[0].aliases[0].lastName", equalTo("Jefferies"))
                .assertThat("$.parties[0].aliases[0].title", equalTo("MR"))
                .assertThat("$.parties[0].aliases[0].organisationName", equalTo("Chicken & Curry Best In Town LTD"));
    }


    public static void verifyCaseCreated(final DocumentContext inputProsectionCase, final JsonObject outputCase) {
        final JsonArray parties = outputCase.getJsonArray("parties");
        assertNotNull(parties);
        assertThat(parties.size(), is(1));
        verifyProsecutionCase(inputProsectionCase, outputCase);
        verifyDefendants(inputProsectionCase, outputCase);
    }

    public static void verifyCaseDefendantUpdated(final DocumentContext inputProsectionCase, final JsonObject outputCase) {

        final JsonArray parties = outputCase.getJsonArray("parties");
        assertNotNull(parties);
        assertThat(parties.size(), is(1));
        verifyDefendantsForDefendantUpdated(inputProsectionCase, outputCase);
        verifyAdditionalAliasesOnOnDefendantUpdate(outputCase);
    }
}
