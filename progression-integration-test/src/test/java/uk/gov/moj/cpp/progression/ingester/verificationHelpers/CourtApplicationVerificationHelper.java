package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.PersonVerificationHelper.assertApplicantDetails;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.PersonVerificationHelper.assertOrganisationDetails;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.PersonVerificationHelper.assertRespondantDetails;

import java.util.concurrent.atomic.AtomicInteger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;

public class CourtApplicationVerificationHelper {

    public static void verifyStandaloneApplication(final String applicationid, final JsonObject transformedJson) {
        assertEquals(applicationid, transformedJson.getString("caseId"));
        assertEquals("APPLICATION", transformedJson.getString("_case_type"));
    }

    public static void verifyEmbeddedApplication(final String applicationid, final JsonObject transformedJson) {
        assertEquals(applicationid, transformedJson.getString("caseId"));
        assertEquals("PROSECUTION", transformedJson.getString("_case_type"));
    }

    public static void verifyUpdateCourtApplication(final DocumentContext inputCourtApplication,
                                                    final JsonObject transformedJson,
                                                    final String applicationId) {

        final JsonArray outputCourtApplications = transformedJson.getJsonArray("applications");
        final JsonObject outputApplication = outputCourtApplications.stream()
                .map(JsonObject.class::cast)
                .filter(application1 -> application1.getString("applicationId").equals(applicationId)).findFirst().get();

        final String id = ((JsonString) inputCourtApplication.read("$.courtApplication.id")).getString();
        final String applicationType = ((JsonString) inputCourtApplication.read("$.courtApplication.type.type")).getString();
        final String applicationReceivedDate = ((JsonString) inputCourtApplication.read("$.courtApplication.applicationReceivedDate")).getString();
        final String applicationDecisionSoughtByDate = ((JsonString) inputCourtApplication.read("$.courtApplication.applicationDecisionSoughtByDate")).getString();
        final String applicationReference = ((JsonString) inputCourtApplication.read("$.courtApplication.applicationReference")).getString();

        assertEquals(id, outputApplication.getString("applicationId"));
        assertEquals(applicationType, outputApplication.getString("applicationType"));
        assertEquals(applicationReceivedDate, outputApplication.getString("receivedDate"));
        assertEquals(applicationDecisionSoughtByDate, outputApplication.getString("decisionDate"));
        assertEquals(applicationReference, outputApplication.getString("applicationReference"));

        final JsonObject inputApplication = inputCourtApplication.read("$.courtApplication");
        final JsonObject applicant = inputApplication.getJsonObject("applicant");
        final JsonArray respondents = getRespondents(inputApplication);
        verifyApplicationParties(transformedJson, applicant, respondents);


    }

    public static void verifyAddCourtApplication(final DocumentContext inputCourtApplication,
                                                 final JsonObject transformedJson,
                                                 final String applicationId) {

        final JsonArray outputCourtApplications = transformedJson.getJsonArray("applications");
        final JsonObject outputApplication = outputCourtApplications.stream()
                .map(JsonObject.class::cast)
                .filter(application1 -> application1.getString("applicationId").equals(applicationId)).findFirst().get();
        final String id = ((JsonString) inputCourtApplication.read("$.courtApplication.id")).getString();
        final String applicationType = ((JsonString) inputCourtApplication.read("$.courtApplication.type.type")).getString();
        final String applicationReceivedDate = ((JsonString) inputCourtApplication.read("$.courtApplication.applicationReceivedDate")).getString();
        final String applicationStatus = ((JsonString) inputCourtApplication.read("$.courtApplication.applicationStatus")).getString();
        final String applicationDecisionSoughtByDate = ((JsonString) inputCourtApplication.read("$.courtApplication.applicationDecisionSoughtByDate")).getString();

        assertEquals(id, outputApplication.getString("applicationId"));
        assertEquals(applicationType, outputApplication.getString("applicationType"));
        assertEquals(applicationReceivedDate, outputApplication.getString("receivedDate"));
        assertEquals(applicationStatus, outputApplication.getString("applicationStatus"));
        assertEquals(applicationDecisionSoughtByDate, outputApplication.getString("decisionDate"));

        final JsonObject inputApplication = inputCourtApplication.read("$.courtApplication");

        final JsonObject applicant = inputApplication.getJsonObject("applicant");
        final JsonArray respondents = getRespondents(inputApplication);

        verifyApplicationParties(transformedJson, applicant, respondents);
        verifyArn(outputApplication);
    }

    private static void verifyArn(final JsonObject outputCourtApplication) {
        assertNotNull(outputCourtApplication.getString("applicationReference"));
    }


    private static void verifyApplicationParties(
            final JsonObject transformedJson,
            final JsonObject applicant,
            final JsonArray respondents) {
        final JsonArray parties = transformedJson.getJsonArray("parties");

        final JsonObject applicantParty = parties.stream()
                .map(JsonObject.class::cast)
                .filter(p -> p.getString("_party_type").equalsIgnoreCase("applicant"))
                .filter(p -> p.getString("partyId").equals(applicant.getString("id"))).findFirst().get();

        verifyApplicant(applicant, applicantParty);

        respondents.stream().
                map(JsonObject.class::cast).
                forEach(applicationRespondent -> {
                    final String respondentId = applicationRespondent.getString("id");
                    final JsonObject respondentParty = parties.stream()
                            .map(JsonObject.class::cast)
                            .filter(p -> p.getString("_party_type").equalsIgnoreCase("respondent"))
                            .filter(p -> p.getString("partyId").equals(respondentId)).findFirst().get();

                    assertEquals(respondentId, respondentParty.getString("partyId"));
                    verifyRespondent(applicationRespondent, respondentParty);
                });
    }

    private static void verifyApplicant(final JsonObject applicant, final JsonObject applicantParty) {
        final String applicantId = applicant.getString("id");
        final JsonObject organisation = applicant.getJsonObject("organisation");
        final String organisationName = organisation.getString("name");
        final JsonArray organisationPersons = applicant.getJsonArray("organisationPersons");

        final AtomicInteger organisationPersonsIndex = new AtomicInteger(0);
        final JsonObject organisationPerson = organisationPersons.getJsonObject(organisationPersonsIndex.getAndIncrement());
        assertEquals(applicantId, applicantParty.getString("partyId"));

        final JsonObject person = organisationPerson.getJsonObject("person");
        final String applicantPartyLastName = applicantParty.getString("lastName", null);
        if (applicantPartyLastName != null) {
            assertApplicantDetails(person, applicantParty, organisationName);
        } else {
            assertOrganisationDetails(organisation, applicantParty);
        }
    }

    private static void verifyRespondent(final JsonObject respondent, final JsonObject respondentParty) {
        final JsonObject organisation = respondent.getJsonObject("organisation");
        final String organisationName = organisation.getString("name");
        final JsonObject personDetails = respondent.getJsonObject("personDetails");
        final String respondentPartyLastName = respondentParty.getString("lastName", null);
        if (respondentPartyLastName != null) {
            assertRespondantDetails(personDetails, respondentParty, organisationName);
        } else {
            assertOrganisationDetails(organisation, respondentParty);
        }
    }

    private static JsonArray getRespondents(final JsonObject inputApplication) {
        return inputApplication.containsKey("respondents") ? inputApplication.getJsonArray("respondents") : createArrayBuilder().build();
    }

}
