package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.JsonPath.parse;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.AddressVerificationHelper.addressLines;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import org.hamcrest.core.IsEqual;

public class HearingResultedCaseUpdatedVerificationHelper {

    public static void verifyInitialElasticSearchCase(final DocumentContext inputProsecutionCase, final JsonObject outputCase, final String caseStatus) {
        with(outputCase.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.id")).getString()))
                .assertThat("$.caseStatus", equalTo(caseStatus));
    }

    public static void verifyUpdatedElasticSearchCase(final JsonObject caseUpdatedEvent, final JsonObject outputCase) {
        final DocumentContext hearingResultedCaseUpdatedEvent = parse(caseUpdatedEvent);
        with(outputCase.toString())
                .assertThat("$.caseId", equalTo(caseUpdatedEvent.getJsonObject("prosecutionCase").getString("id")))
                .assertThat("$.caseStatus", equalTo(caseUpdatedEvent.getJsonObject("prosecutionCase").getString("caseStatus")))
                .assertThat("$.parties[0].partyId", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].id")).getString()))
                .assertThat("$.parties[0].proceedingsConcluded", IsEqual.equalTo(Boolean.valueOf(hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].proceedingsConcluded").toString())))
                .assertThat("$.parties[0].firstName", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName")).getString()))
                .assertThat("$.parties[0].middleName", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.middleName")).getString()))
                .assertThat("$.parties[0].lastName", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.lastName")).getString()))
                .assertThat("$.parties[0].title", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read( "$.prosecutionCase.defendants[0].personDefendant.personDetails.title")).getString()))
                .assertThat( "$.parties[0].dateOfBirth", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.dateOfBirth")).getString()))
                .assertThat("$.parties[0].gender", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.gender")).getString()))
                .assertThat("$.parties[0].postCode", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.address.postcode")).getString()))
                .assertThat("$.parties[0].addressLines", IsEqual.equalTo(addressLines(hearingResultedCaseUpdatedEvent, "$.prosecutionCase.defendants[0].personDefendant.personDetails.address")))
                .assertThat("$.parties[0].pncId", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].pncId")).getString()))
                .assertThat("$.parties[0].arrestSummonsNumber", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].personDefendant.arrestSummonsNumber")).getString()))

                .assertThat("$.parties[0].nationalInsuranceNumber", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].personDefendant.personDetails.nationalInsuranceNumber")).getString()))

                .assertThat("$.parties[0].offences[0].offenceId", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].offences[0].id")).getString()))
                .assertThat("$.parties[0].offences[0].startDate", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].offences[0].startDate")).getString()))
                .assertThat("$.parties[0].offences[0].endDate", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].offences[0].endDate")).getString()))
                .assertThat("$.parties[0].offences[0].arrestDate", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].offences[0].arrestDate")).getString()))
                .assertThat("$.parties[0].offences[0].chargeDate", IsEqual.equalTo(((JsonString) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].offences[0].chargeDate")).getString()))
                .assertThat("$.parties[0].offences[0].orderIndex", IsEqual.equalTo(((JsonNumber) hearingResultedCaseUpdatedEvent.read("$.prosecutionCase.defendants[0].offences[0].orderIndex")).intValue()));

    }

}
