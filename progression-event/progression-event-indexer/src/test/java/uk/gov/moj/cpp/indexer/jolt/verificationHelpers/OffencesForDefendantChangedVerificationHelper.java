package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.Boolean.parseBoolean;
import static org.hamcrest.core.IsEqual.equalTo;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;

public class OffencesForDefendantChangedVerificationHelper {

    public static void verifyUpdatedOffences(final DocumentContext inputOffence, final JsonObject outputOffence) {

        with(outputOffence.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].prosecutionCaseId")).getString()))
                .assertThat("$.parties[0]._party_type", equalTo("DEFENDANT"))
                .assertThat("$.parties[0].offences[0].offenceId", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].id")).getString()))
                .assertThat("$.parties[0].offences[0].offenceCode", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].offenceCode")).getString()))
                .assertThat("$.parties[0].offences[0].offenceTitle", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].offenceTitle")).getString()))
                .assertThat("$.parties[0].offences[0].offenceLegislation", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].offenceLegislation")).getString()))
                .assertThat("$.parties[0].offences[0].dateOfInformation", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].dateOfInformation")).getString()))
                .assertThat("$.parties[0].offences[0].startDate", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].startDate")).getString()))
                .assertThat("$.parties[0].offences[0].endDate", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].endDate")).getString()))
                .assertThat("$.parties[0].offences[0].arrestDate", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].arrestDate")).getString()))
                .assertThat("$.parties[0].offences[0].chargeDate", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].chargeDate")).getString()))
                .assertThat("$.parties[0].offences[0].modeOfTrial", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].modeOfTrial")).getString()))
                .assertThat("$.parties[0].offences[0].orderIndex", equalTo(((JsonNumber) inputOffence.read("$.updatedOffences[0].offences[0].orderIndex")).intValue()))
                .assertThat("$.parties[0].offences[0].wording", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].wording")).getString()))
                .assertThat("$.parties[0].offences[0].proceedingsConcluded", equalTo( parseBoolean(inputOffence.read("$.updatedOffences[0].offences[0].proceedingsConcluded").toString())))
                .assertThat("$.parties[0].offences[0].laaReference.applicationReference", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].laaApplnReference.applicationReference")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusId", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].laaApplnReference.statusId")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusCode", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].laaApplnReference.statusCode")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusDescription", equalTo(((JsonString) inputOffence.read("$.updatedOffences[0].offences[0].laaApplnReference.statusDescription")).getString()));
    }

    public static void verifyAddedOffences(final DocumentContext inputOffence, final JsonObject outputOffence) {

        with(outputOffence.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].prosecutionCaseId")).getString()))
                .assertThat("$.parties[0]._party_type", equalTo("DEFENDANT"))
                .assertThat("$.parties[0].offences[0].offenceId", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].id")).getString()))
                .assertThat("$.parties[0].offences[0].offenceCode", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].offenceCode")).getString()))
                .assertThat("$.parties[0].offences[0].offenceTitle", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].offenceTitle")).getString()))
                .assertThat("$.parties[0].offences[0].offenceLegislation", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].offenceLegislation")).getString()))
                .assertThat("$.parties[0].offences[0].dateOfInformation", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].dateOfInformation")).getString()))
                .assertThat("$.parties[0].offences[0].startDate", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].startDate")).getString()))
                .assertThat("$.parties[0].offences[0].endDate", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].endDate")).getString()))
                .assertThat("$.parties[0].offences[0].arrestDate", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].arrestDate")).getString()))
                .assertThat("$.parties[0].offences[0].chargeDate", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].chargeDate")).getString()))
                .assertThat("$.parties[0].offences[0].modeOfTrial", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].modeOfTrial")).getString()))
                .assertThat("$.parties[0].offences[0].orderIndex", equalTo(((JsonNumber) inputOffence.read("$.addedOffences[0].offences[0].orderIndex")).intValue()))
                .assertThat("$.parties[0].offences[0].wording", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].wording")).getString()))
                .assertThat("$.parties[0].offences[0].proceedingsConcluded", equalTo( parseBoolean(inputOffence.read("$.addedOffences[0].offences[0].proceedingsConcluded").toString())))
                .assertThat("$.parties[0].offences[0].laaReference.applicationReference", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].laaApplnReference.applicationReference")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusId", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].laaApplnReference.statusId")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusCode", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].laaApplnReference.statusCode")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusDescription", equalTo(((JsonString) inputOffence.read("$.addedOffences[0].offences[0].laaApplnReference.statusDescription")).getString()));
    }

    public static void verifyDeletedOffences(final DocumentContext inputOffence, final JsonObject outputOffence) {

        with(outputOffence.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputOffence.read("$.deletedOffences[0].prosecutionCaseId")).getString()))
                .assertNotNull("$.parties[*].offences[*]");


    }
}