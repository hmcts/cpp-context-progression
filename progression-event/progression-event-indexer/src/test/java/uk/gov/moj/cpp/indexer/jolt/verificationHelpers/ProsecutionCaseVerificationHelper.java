package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.stream.IntStream.range;
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
                .assertThat("$._case_type", equalTo("PROSECUTION"))
                .assertThat("$._is_crown", equalTo(false));
    }

    public static void verifyDefendants(final DocumentContext inputProsecutionCase, final JsonObject outputCase, final int count, final boolean includeAliasAndOrganisation) {

        range(0, count)
                .forEach(index -> {

            final String partiesIndexPath = String.format("$.parties[%d]", index);
            final String defendantIndexPath = String.format("$.prosecutionCase.defendants[%d]", index);

            with(outputCase.toString())
                    .assertThat("$.parties[*]", hasSize(count))
                    .assertThat(partiesIndexPath + ".partyId", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".id")).getString()))
                    .assertThat(partiesIndexPath + ".title", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.title")).getString()))
                    .assertThat(partiesIndexPath + ".firstName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.firstName")).getString()))
                    .assertThat(partiesIndexPath + ".middleName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.middleName")).getString()))
                    .assertThat(partiesIndexPath + ".lastName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.lastName")).getString()))
                    .assertThat(partiesIndexPath + ".dateOfBirth", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.dateOfBirth")).getString()))
                    .assertThat(partiesIndexPath + ".gender", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.gender")).getString()))
                    .assertThat(partiesIndexPath + ".postCode", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.address.postcode")).getString()))
                    .assertThat(partiesIndexPath + ".addressLines", equalTo(addressLines(inputProsecutionCase, defendantIndexPath + ".personDefendant.personDetails.address")))
                    .assertThat(partiesIndexPath + ".pncId", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".pncId")).getString()))
                    .assertThat(partiesIndexPath + ".arrestSummonsNumber", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.arrestSummonsNumber")).getString()))
                    .assertThat(partiesIndexPath + "._party_type", equalTo("DEFENDANT"));
            if (includeAliasAndOrganisation) {
                with(outputCase.toString())
                        .assertThat(partiesIndexPath + ".organisationName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".legalEntityDefendant.organisation.name")).getString()))
                        .assertThat(partiesIndexPath + ".aliases[0].firstName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".aliases[0].firstName")).getString()))
                        .assertThat(partiesIndexPath + ".aliases[0].middleName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".aliases[0].middleName")).getString()))
                        .assertThat(partiesIndexPath + ".aliases[0].lastName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".aliases[0].lastName")).getString()))
                        .assertThat(partiesIndexPath + ".aliases[0].title", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".aliases[0].title")).getString()))
                        .assertThat(partiesIndexPath + ".aliases[0].organisationName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".aliases[0].legalEntityName")).getString()));
            }

        });



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

    public static void verifyCaseCreated(final DocumentContext inputProsecutionCase, final JsonObject outputCase, final int defendantCount, final boolean includeAliasAndOrganisation) {
        final JsonArray parties = outputCase.getJsonArray("parties");
        assertNotNull(parties);
        assertThat(parties.size(), is(defendantCount));
        verifyProsecutionCase(inputProsecutionCase, outputCase);
        verifyDefendants(inputProsecutionCase, outputCase, defendantCount, includeAliasAndOrganisation);
    }

    public static void verifyCaseDefendantUpdated(final DocumentContext inputProsecutionCase, final JsonObject outputCase) {

        final JsonArray parties = outputCase.getJsonArray("parties");
        assertNotNull(parties);
        assertThat(parties.size(), is(1));
        verifyDefendantsForDefendantUpdated(inputProsecutionCase, outputCase);
        verifyAdditionalAliasesOnOnDefendantUpdate(outputCase);
    }
}
