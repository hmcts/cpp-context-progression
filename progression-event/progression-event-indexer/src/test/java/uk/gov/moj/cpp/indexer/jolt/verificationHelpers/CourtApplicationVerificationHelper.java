package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.AddressVerificationHelper.assertAddressDetails;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.PersonVerificationHelper.assertApplicantDetails;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.PersonVerificationHelper.assertDefendantDetails;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.PersonVerificationHelper.assertOrganisationDetails;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.PersonVerificationHelper.assertPersonDetails;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.PersonVerificationHelper.assertRespondantDetails;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.jayway.jsonpath.DocumentContext;

public class CourtApplicationVerificationHelper {

    public static void verifyStandaloneApplication(final DocumentContext inputCourtApplication, final JsonObject transformedJson) {
        final String applicationid = ((JsonString) inputCourtApplication.read("$.courtApplication.id")).getString();
        assertEquals(applicationid, transformedJson.getString("caseId"));
        assertEquals("APPLICATION", transformedJson.getString("_case_type"));
    }

    public static void verifyEmbeddedApplication(final DocumentContext inputCourtApplication, final JsonObject transformedJson) {
        final String linkedCaseId = ((JsonString) inputCourtApplication.read("$.courtApplication.linkedCaseId")).getString();
        assertEquals(linkedCaseId, transformedJson.getString("caseId"));
        assertEquals("PROSECUTION", transformedJson.getString("_case_type"));
    }

    public static void verifyAddApplication(final DocumentContext inputCourtApplication, final JsonObject transformedJson) {

        final JsonArray outputCourtApplications = transformedJson.getJsonArray("applications");
        final JsonArray parties = transformedJson.getJsonArray("parties");
        final JsonObject applicant = inputCourtApplication.read("$.courtApplication.applicant");
        final JsonArray respondents = inputCourtApplication.read("$.courtApplication.respondents");
        verifyArn(inputCourtApplication, outputCourtApplications);
        verifyApplication(inputCourtApplication, outputCourtApplications, parties, applicant, respondents);
    }

    public static void verifyAddApplicationWhenNoOrganisationApplicant(final DocumentContext inputCourtApplication, final JsonObject transformedJson) {

        final JsonArray outputCourtApplications = transformedJson.getJsonArray("applications");
        final JsonArray parties = transformedJson.getJsonArray("parties");
        final JsonObject applicant = inputCourtApplication.read("$.courtApplication.applicant");
        final JsonArray respondents = inputCourtApplication.read("$.courtApplication.respondents");
        verifyArn(inputCourtApplication, outputCourtApplications);
        verifyApplicationWithNoApplicantOrganisation(inputCourtApplication, outputCourtApplications, parties, applicant, respondents);
    }

    public static void verifyUpdateApplication(final DocumentContext inputCourtApplication, final JsonObject transformedJson) {

        final JsonArray outputCourtApplications = transformedJson.getJsonArray("applications");
        final JsonArray parties = transformedJson.getJsonArray("parties");
        final JsonObject applicant = inputCourtApplication.read("$.courtApplication.applicant");
        final JsonArray respondents = inputCourtApplication.read("$.courtApplication.respondents");
        verifyApplication(inputCourtApplication, outputCourtApplications, parties, applicant, respondents);

        final String sourceApplicationReference = ((JsonString) inputCourtApplication.read("$.courtApplication.applicationReference")).getString();
        final JsonObject transformedApplication = transformedJson.getJsonArray("applications").getJsonObject(0);
        assertNotNull(transformedApplication);

        final JsonString applicationReferenceValue = transformedApplication.getJsonString("applicationReference");
        assertNotNull(applicationReferenceValue);

        assertEquals(sourceApplicationReference, applicationReferenceValue.getString());

    }

    public static void verifyExtendHearing(final DocumentContext inputCourtApplication, final JsonObject transformedJson) {

        final JsonArray outputCourtApplications = transformedJson.getJsonArray("applications");
        final JsonArray parties = transformedJson.getJsonArray("parties");
        final JsonObject applicant = inputCourtApplication.read("$.courtApplication.applicant");
        final JsonArray respondents = inputCourtApplication.read("$.courtApplication.respondents");
        verifyApplication(inputCourtApplication, outputCourtApplications, parties, applicant, respondents);
    }

    private static void verifyArn(final DocumentContext courtApplication, final JsonArray outputCourtApplications) {

        final Object arnValue = courtApplication.read("$.arn");
        if (arnValue != null) {
            final String arn = ((JsonString) arnValue).getString();
            final JsonObject outputApplication = outputCourtApplications.getJsonObject(0);
            assertEquals(arn, outputApplication.getString("applicationReference"));
        }
    }

    private static void verifyApplication(final DocumentContext courtApplication, final JsonArray outputCourtApplications) {

        final String id = ((JsonString) courtApplication.read("$.courtApplication.id")).getString();
        final String applicationType = ((JsonString) courtApplication.read("$.courtApplication.type.applicationType")).getString();
        final String applicationReceivedDate = ((JsonString) courtApplication.read("$.courtApplication.applicationReceivedDate")).getString();
        final String applicationDecisionSoughtByDate = ((JsonString) courtApplication.read("$.courtApplication.applicationDecisionSoughtByDate")).getString();
        final JsonObject outputApplication = outputCourtApplications.getJsonObject(0);
        assertEquals(id, outputApplication.getString("applicationId"));
        assertEquals(applicationType, outputApplication.getString("applicationType"));
        assertEquals(applicationReceivedDate, outputApplication.getString("receivedDate"));
        assertEquals(applicationDecisionSoughtByDate, outputApplication.getString("decisionDate"));
    }

    private static void verifyApplicantTransformationWhenNoOrganisation(final JsonObject applicant, final JsonArray parties) {

        final String applicantId = applicant.getString("id");

        final JsonObject personDetails = applicant.getJsonObject("personDetails");
        verifyApplicantPersonDetails(personDetails, parties, applicantId);

        final JsonObject applicantDefendant = applicant.getJsonObject("defendant");

        final Stream<JsonObject> defendantPartyStream = parties.stream().map(JsonObject.class::cast)
                .filter(p -> p.getString("_party_type").equalsIgnoreCase("defendant"));

        final long defendantCount = defendantPartyStream.count();
        assertThat(defendantCount, is(2l));

        verifyDefendant(applicantDefendant, parties);


    }


    private static void verifyApplicantTransformation(final JsonObject applicant, final JsonArray parties) {
        final String applicantId = applicant.getString("id");

        final JsonObject personDetails = applicant.getJsonObject("personDetails");
        if (personDetails != null) {
            verifyApplicantPersonDetails(personDetails, parties, applicantId);
        }

        final JsonArray organisationPersons = applicant.getJsonArray("organisationPersons");
        final AtomicInteger organisationPersonsIndex = new AtomicInteger(0);
        final JsonObject applicantDefendant = applicant.getJsonObject("defendant");

        final Stream<JsonObject> defendantPartyStream = parties.stream().map(JsonObject.class::cast)
                .filter(p -> p.getString("_party_type").equalsIgnoreCase("defendant"));

        final long defendantCount = defendantPartyStream.count();
        assertThat(defendantCount, is(2l));

        verifyDefendant(applicantDefendant, parties);

        assertThat(organisationPersons.size(), greaterThan(0));

        final long applicantCount = parties.stream()
                .map(JsonObject.class::cast)
                .filter(p -> p.getString("_party_type").equalsIgnoreCase("applicant"))
                .count();

        assertThat(applicantCount, greaterThan(0l));

        final JsonObject firstOrganisationPerson = organisationPersons.getJsonObject(0);

        parties.stream()
                .map(JsonObject.class::cast)
                .filter(p -> p.getString("_party_type").equalsIgnoreCase("applicant"))
                .forEach(applicantParty -> {

                    assertEquals(applicantId, applicantParty.getString("partyId"));

                    final JsonObject organisationPerson = organisationPersons.getJsonObject(organisationPersonsIndex.getAndIncrement());
                    final JsonObject person = organisationPerson.getJsonObject("person");

                    final JsonObject organisation = applicant.getJsonObject("organisation");
                    if (organisation != null) {
                        final String organisationName = organisation.getString("name");
                        final JsonString applicantLastName = applicantParty.getJsonString("lastName");
                        if (applicantLastName != null) {
                            assertApplicantDetails(person, applicantParty, organisationName);
                        }
                        else {
                            assertOrganisationDetails(person, applicantParty);
                        }
                    }

                    final String addressLines = applicantParty.getString("addressLines");
                    assertAddressDetails(person.getJsonObject("address"), addressLines, applicantParty.getString("postCode"));
            });

    }

    private static void verifyRespondentsTransformation(final JsonArray respondents, final JsonArray parties) {

        assertThat(respondents.size(), greaterThan(0));

        respondents.forEach(respondent -> {

            final JsonObject respondentsJsonObject = (JsonObject) respondent;
            final JsonObject partyDetails = respondentsJsonObject.getJsonObject("partyDetails");
            final String respondentId = partyDetails.getString("id");
            final JsonObject respondentDetails = respondentsJsonObject.getJsonObject("partyDetails").getJsonObject("personDetails");

            final long applicantCount = parties.stream()
                    .filter(p -> ((JsonObject) p).getString("_party_type").equalsIgnoreCase("respondent"))
                    .count();
            assertThat(applicantCount, greaterThan(0l));

            parties.stream()
                    .map(JsonObject.class::cast)
                    .filter(p -> p.getString("_party_type").equalsIgnoreCase("respondent"))
                    .forEach(respondentParty -> {

                        assertEquals(respondentId, respondentParty.getString("partyId"));
                        final JsonObject organisation = partyDetails.getJsonObject("organisation");
                        if (respondentDetails != null && organisation != null) {
                            final String organisationName = organisation.getString("name");
                            assertRespondantDetails(respondentDetails, respondentParty, organisationName);
                        }

                        final JsonObject defendant = partyDetails.getJsonObject("defendant");
                        assertNotNull(defendant);
                        final long defendantCount = parties.stream()
                                .filter(p -> ((JsonObject) p).getString("_party_type").equalsIgnoreCase("defendant"))
                                .count();
                        assertThat(defendantCount, is(2l));
                        verifyDefendant(defendant, parties);
            });
        });

    }

    private static void verifyDefendant(final JsonObject defendant, final JsonArray parties) {

        parties.stream().map(JsonObject.class::cast)
                .filter(p -> p.getString("_party_type").equalsIgnoreCase("defendant")
                        && (p.getString("partyId").equalsIgnoreCase(defendant.getString("id"))))
                .forEach(defendantParty -> {
                    final JsonObject legalEntityDefendant = defendant.getJsonObject("legalEntityDefendant");
                    final JsonObject organisation = legalEntityDefendant.getJsonObject("organisation");
                    final String organisationName = organisation.getString("name");
                    assertDefendantDetails(defendant, defendantParty, organisationName);
                });
    }


    private static void verifyApplicantPersonDetails(final JsonObject personDetails, final JsonArray parties, final String applicantId) {

        final Optional<JsonObject> applicant = parties.stream()
                .map(JsonObject.class::cast)
                .filter(p -> p.getString("_party_type").equalsIgnoreCase("applicant")
                        && (p.getString("partyId").equalsIgnoreCase(applicantId)))
                .findFirst();

        assertTrue(applicant.isPresent());
        assertPersonDetails(personDetails, applicant.get());

    }

    private static void verifyApplication(final DocumentContext inputCourtApplication,
                                          final JsonArray outputCourtApplications,
                                          final JsonArray parties,
                                          final JsonObject applicant,
                                          final JsonArray respondents) {
        assertNotNull(inputCourtApplication);
        assertEquals(4, parties.size());

        verifyApplication(inputCourtApplication, outputCourtApplications);

        verifyApplicantTransformation(applicant, parties);

        verifyRespondentsTransformation(respondents, parties);
    }

    private static void verifyApplicationWithNoApplicantOrganisation(final DocumentContext inputCourtApplication,
                                          final JsonArray outputCourtApplications,
                                          final JsonArray parties,
                                          final JsonObject applicant,
                                          final JsonArray respondents) {
        assertNotNull(inputCourtApplication);
        assertEquals(4, parties.size());

        verifyApplication(inputCourtApplication, outputCourtApplications);

        verifyApplicantTransformationWhenNoOrganisation(applicant, parties);

        verifyRespondentsTransformation(respondents, parties);
    }
}
