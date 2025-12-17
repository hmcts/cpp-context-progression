package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.JsonPath.parse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.AddressVerificationHelper.addressLines;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJsonViaPath;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.VerificationUtil.initializeJolt;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HearingResultedCaseUpdatedTransformationTest {


    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformHearingResultedJson() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.hearing-resulted-case-updated-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.hearing-resulted-case-updated.json");
        final DocumentContext inputProsecutionCase = parse(inputJson);

        final JsonObject outputCase = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        with(outputCase.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.id")).getString()))
                .assertThat("$.caseStatus", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.caseStatus")).getString()))
                .assertThat("$._case_type", equalTo("PROSECUTION"))
                .assertThat("$.prosecutingAuthority", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityCode")).getString()));

        assertThat(outputCase.getJsonArray("parties").size(), is(1));

        with(outputCase.toString())
                .assertThat("$.parties[0].partyId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].id")).getString()))
                .assertThat("$.parties[0].proceedingsConcluded", equalTo(Boolean.parseBoolean(inputProsecutionCase.read("$.prosecutionCase.defendants[0].proceedingsConcluded").toString())))
                .assertThat("$.parties[0].firstName", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName")).getString()))
                .assertThat("$.parties[0].middleName", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.middleName")).getString()))
                .assertThat("$.parties[0].lastName", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.lastName")).getString()))
                .assertThat("$.parties[0].title", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.title")).getString()))
                .assertThat( "$.parties[0].dateOfBirth", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.dateOfBirth")).getString()))
                .assertThat("$.parties[0].gender", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.gender")).getString()))
                .assertThat("$.parties[0].postCode", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.address.postcode")).getString()))
                .assertThat("$.parties[0].addressLines", equalTo(addressLines(inputProsecutionCase, "$.prosecutionCase.defendants[0].personDefendant.personDetails.address")))
                .assertThat("$.parties[0].pncId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].pncId")).getString()))
                .assertThat("$.parties[0].arrestSummonsNumber", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.arrestSummonsNumber")).getString()))

                .assertThat("$.parties[0].nationalInsuranceNumber", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.nationalInsuranceNumber")).getString()))

                .assertThat("$.parties[0].offences[0].offenceId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].id")).getString()))
                .assertThat("$.parties[0].offences[0].startDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].startDate")).getString()))
                .assertThat("$.parties[0].offences[0].endDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].endDate")).getString()))
                .assertThat("$.parties[0].offences[0].arrestDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].arrestDate")).getString()))
                .assertThat("$.parties[0].offences[0].chargeDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].chargeDate")).getString()))
                .assertThat("$.parties[0].offences[0].orderIndex", equalTo(((JsonNumber) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].orderIndex")).intValue()));
    }

    @Test
    public void shouldTransformHearingResultedWhenProsecutorIsNotNull() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.hearing-resulted-case-updated-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.hearing-resulted-case-updated-with-prosecutor.json");
        final DocumentContext inputProsecutionCase = parse(inputJson);

        final JsonObject outputCase = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        with(outputCase.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.id")).getString()))
                .assertThat("$.caseStatus", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.caseStatus")).getString()))
                .assertThat("$._case_type", equalTo("PROSECUTION"))
                .assertThat("$.prosecutingAuthority", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.prosecutor.prosecutorCode")).getString()));

    }

    @Test
    public void shouldTransformHearingResultedJsonWithMultipleDefendantsAndOffences() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/progression.event.hearing-resulted-case-updated-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/progression.event.hearing-resulted-case-updated-multiple-defendant-offence.json");
        final DocumentContext inputProsecutionCase = parse(inputJson);

        final JsonObject outputCase = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        with(outputCase.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.id")).getString()))
                .assertThat("$.caseStatus", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.caseStatus")).getString()))
                .assertThat("$._case_type", equalTo("PROSECUTION"));

        assertThat(outputCase.getJsonArray("parties").size(), is(2));
        assertThat(outputCase.getJsonArray("parties").getJsonObject(1).getJsonArray("offences").size(), is(2));

        with(outputCase.toString())
                //first defendant details
                .assertThat("$.parties[0].partyId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].id")).getString()))
                .assertThat("$.parties[0].proceedingsConcluded", equalTo(Boolean.parseBoolean(inputProsecutionCase.read("$.prosecutionCase.defendants[0].proceedingsConcluded").toString())))
                .assertThat("$.parties[0].firstName", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName")).getString()))
                .assertThat("$.parties[0].middleName", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.middleName")).getString()))
                .assertThat("$.parties[0].lastName", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.lastName")).getString()))
                .assertThat("$.parties[0].title", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.title")).getString()))
                .assertThat( "$.parties[0].dateOfBirth", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.dateOfBirth")).getString()))
                .assertThat("$.parties[0].gender", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.gender")).getString()))
                .assertThat("$.parties[0].postCode", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.address.postcode")).getString()))
                .assertThat("$.parties[0].addressLines", equalTo(addressLines(inputProsecutionCase, "$.prosecutionCase.defendants[0].personDefendant.personDetails.address")))
                .assertThat("$.parties[0].pncId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].pncId")).getString()))
                .assertThat("$.parties[0].arrestSummonsNumber", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.arrestSummonsNumber")).getString()))

                .assertThat("$.parties[0].nationalInsuranceNumber", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.nationalInsuranceNumber")).getString()))

                .assertThat("$.parties[0].offences[0].offenceId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].id")).getString()))
                .assertThat("$.parties[0].offences[0].startDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].startDate")).getString()))
                .assertThat("$.parties[0].offences[0].endDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].endDate")).getString()))
                .assertThat("$.parties[0].offences[0].arrestDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].arrestDate")).getString()))
                .assertThat("$.parties[0].offences[0].chargeDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].chargeDate")).getString()))
                .assertThat("$.parties[0].offences[0].orderIndex", equalTo(((JsonNumber) inputProsecutionCase.read("$.prosecutionCase.defendants[0].offences[0].orderIndex")).intValue()))

                //Second defendant details
                .assertThat("$.parties[1].partyId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].id")).getString()))
                .assertThat("$.parties[1].proceedingsConcluded", equalTo(Boolean.parseBoolean(inputProsecutionCase.read("$.prosecutionCase.defendants[1].proceedingsConcluded").toString())))
                .assertThat("$.parties[1].firstName", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[1].personDefendant.personDetails.firstName")).getString()))
                .assertThat("$.parties[1].middleName", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[1].personDefendant.personDetails.middleName")).getString()))
                .assertThat("$.parties[1].lastName", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[1].personDefendant.personDetails.lastName")).getString()))
                .assertThat("$.parties[1].title", equalTo(((JsonString) inputProsecutionCase.read( "$.prosecutionCase.defendants[1].personDefendant.personDetails.title")).getString()))
                .assertThat( "$.parties[1].dateOfBirth", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].personDefendant.personDetails.dateOfBirth")).getString()))
                .assertThat("$.parties[1].gender", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].personDefendant.personDetails.gender")).getString()))
                .assertThat("$.parties[1].postCode", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].personDefendant.personDetails.address.postcode")).getString()))
                .assertThat("$.parties[1].addressLines", equalTo(addressLines(inputProsecutionCase, "$.prosecutionCase.defendants[1].personDefendant.personDetails.address")))
                .assertThat("$.parties[1].pncId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].pncId")).getString()))
                .assertThat("$.parties[1].arrestSummonsNumber", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].personDefendant.arrestSummonsNumber")).getString()))

                .assertThat("$.parties[1].nationalInsuranceNumber", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].personDefendant.personDetails.nationalInsuranceNumber")).getString()))

                //Multiple offences
                .assertThat("$.parties[1].offences[0].offenceId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[0].id")).getString()))
                .assertThat("$.parties[1].offences[0].startDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[0].startDate")).getString()))
                .assertThat("$.parties[1].offences[0].endDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[0].endDate")).getString()))
                .assertThat("$.parties[1].offences[0].arrestDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[0].arrestDate")).getString()))
                .assertThat("$.parties[1].offences[0].chargeDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[0].chargeDate")).getString()))
                .assertThat("$.parties[1].offences[0].orderIndex", equalTo(((JsonNumber) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[0].orderIndex")).intValue()))

                .assertThat("$.parties[1].offences[1].offenceId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[1].id")).getString()))
                .assertThat("$.parties[1].offences[1].startDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[1].startDate")).getString()))
                .assertThat("$.parties[1].offences[1].endDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[1].endDate")).getString()))
                .assertThat("$.parties[1].offences[1].arrestDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[1].arrestDate")).getString()))
                .assertThat("$.parties[1].offences[1].chargeDate", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[1].chargeDate")).getString()))
                .assertThat("$.parties[1].offences[1].orderIndex", equalTo(((JsonNumber) inputProsecutionCase.read("$.prosecutionCase.defendants[1].offences[1].orderIndex")).intValue()));
    }
}
