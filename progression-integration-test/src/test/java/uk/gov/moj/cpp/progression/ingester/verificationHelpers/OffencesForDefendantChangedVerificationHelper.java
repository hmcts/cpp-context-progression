package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.String.valueOf;
import static org.hamcrest.core.IsEqual.equalTo;

import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;

public class OffencesForDefendantChangedVerificationHelper {

    public static void verifyInitialOffence(final DocumentContext inputOffence, final JsonObject outputOffence) {

        with(outputOffence.toString())
                .assertThat("$.offenceId", equalTo(((JsonString) inputOffence.read("$.id")).getString()))
                .assertThat("$.startDate", equalTo(((JsonString) inputOffence.read("$.startDate")).getString()))
                .assertThat("$.endDate", equalTo(((JsonString) inputOffence.read("$.endDate")).getString()))
                .assertThat("$.arrestDate", equalTo(((JsonString) inputOffence.read("$.arrestDate")).getString()))
                .assertThat("$.chargeDate", equalTo(((JsonString) inputOffence.read("$.chargeDate")).getString()))
                .assertThat("$.wording", equalTo(((JsonString) inputOffence.read("$.wording")).getString()));
    }

    public static void verifyUpdatedOffence(final JsonObject defendantChangedEvent, final JsonObject updatedOffenceResponseJsonObject) {
        with(updatedOffenceResponseJsonObject.toString())
                .assertThat("$.offenceId", equalTo(defendantChangedEvent.getString("id")))
                .assertThat("$.offenceLegislation", equalTo(defendantChangedEvent.getString("offenceLegislation")))
                .assertThat("$.offenceCode", equalTo(defendantChangedEvent.getString("offenceCode")))
                .assertThat("$.offenceTitle", equalTo(defendantChangedEvent.getString("offenceTitle")))
                .assertThat("$.dateOfInformation", equalTo(defendantChangedEvent.getString("dateOfInformation")))
                .assertThat("$.startDate", equalTo(defendantChangedEvent.getString("startDate")))
                .assertThat("$.endDate", equalTo(defendantChangedEvent.getString("endDate")))
                .assertThat("$.modeOfTrial", equalTo(valueOf(defendantChangedEvent.getString("modeOfTrial"))))
                .assertThat("$.orderIndex", equalTo(defendantChangedEvent.getInt("orderIndex")))
                .assertThat("$.arrestDate", equalTo(defendantChangedEvent.getString("arrestDate")))
                .assertThat("$.wording", equalTo(defendantChangedEvent.getString("wording")))
                .assertThat("$.chargeDate", equalTo(defendantChangedEvent.getString("chargeDate")))
                .assertThat("$.proceedingsConcluded", equalTo(defendantChangedEvent.getBoolean("proceedingsConcluded")))
                .assertThat("$.laaReference.statusDescription", equalTo(defendantChangedEvent.getJsonObject("laaApplnReference").getString("statusDescription")))
                .assertThat("$.laaReference.statusId", equalTo(defendantChangedEvent.getJsonObject("laaApplnReference").getString("statusId")))
                .assertThat("$.laaReference.applicationReference", equalTo(defendantChangedEvent.getJsonObject("laaApplnReference").getString("applicationReference")))
                .assertThat("$.laaReference.statusCode", equalTo(defendantChangedEvent.getJsonObject("laaApplnReference").getString("statusCode")));
    }
}
