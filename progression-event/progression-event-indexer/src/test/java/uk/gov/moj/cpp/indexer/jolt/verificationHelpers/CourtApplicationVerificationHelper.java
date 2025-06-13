package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.AddressVerificationHelper.assertAddressDetails;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.PersonVerificationHelper.assertApplicantDetails;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.PersonVerificationHelper.assertDefendantDetails;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.PersonVerificationHelper.assertOrganisationDetails;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.PersonVerificationHelper.assertPersonDetails;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.PersonVerificationHelper.assertRespondantDetails;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import org.hamcrest.Matchers;

public class CourtApplicationVerificationHelper {

    public static void verifyStandaloneApplication(final DocumentContext inputCourtApplication, final JsonObject transformedJson) {
        final String applicationid = ((JsonString) inputCourtApplication.read("$.courtApplication.id")).getString();
        assertThat(applicationid, is(getCaseDetails(transformedJson).getString("caseId")));
        assertThat("APPLICATION", is(getCaseDetails(transformedJson).getString("_case_type")));
    }

    public static void verifyCourtApplication(final DocumentContext inputCourtApplication, final JsonObject transformedJson) {
        final String applicationid = ((JsonString) inputCourtApplication.read("$.application.id")).getString();
        assertThat(applicationid, is(getCaseDetails(transformedJson).getString("caseId")));
    }

    public static void verifyEmbeddedApplication(final DocumentContext inputCourtApplication, final JsonObject transformedJson) {
        final JsonObject courtApplicationJson = inputCourtApplication.read("$.courtApplication");
        String linkedCaseId;

        if (courtApplicationJson.containsKey("courtApplicationCases")) {
            linkedCaseId = courtApplicationJson.getJsonArray("courtApplicationCases").getJsonObject(0).getString("prosecutionCaseId");
        } else {
            linkedCaseId = courtApplicationJson.getJsonObject("courtOrder").getJsonArray("courtOrderOffences").getJsonObject(0).getString("prosecutionCaseId");
        }

        assertThat(linkedCaseId, is(getCaseDetails(transformedJson).getString("caseId")));
        assertThat("PROSECUTION", is(getCaseDetails(transformedJson).getString("_case_type")));
    }

    public static void verifyAddApplication(final DocumentContext inputCourtApplication, final JsonObject transformedJson, final int expectedPartCount, final int expectedDefendantCount) {

        final JsonArray outputCourtApplications = getCaseDetails(transformedJson).getJsonArray("applications");
        final JsonArray parties = getCaseDetails(transformedJson).getJsonArray("parties");
        final JsonObject applicant = inputCourtApplication.read("$.courtApplication.applicant");
        final JsonArray respondents = inputCourtApplication.read("$.courtApplication.respondents");
        verifyArn(inputCourtApplication, outputCourtApplications);
        verifyApplication(inputCourtApplication, outputCourtApplications, parties, applicant, respondents, expectedPartCount, expectedDefendantCount);
    }

    public static void verifyAddApplicationWhenNoOrganisationApplicant(final DocumentContext inputCourtApplication, final JsonObject transformedJson, final int expectedPartCount, final int expectedDefendantCount) {

        final JsonArray outputCourtApplications = getCaseDetails(transformedJson).getJsonArray("applications");
        final JsonArray parties = getCaseDetails(transformedJson).getJsonArray("parties");
        final JsonObject applicant = inputCourtApplication.read("$.courtApplication.applicant");
        final JsonArray respondents = inputCourtApplication.read("$.courtApplication.respondents");
        verifyArn(inputCourtApplication, outputCourtApplications);
        verifyApplicationWithNoApplicantOrganisation(inputCourtApplication, outputCourtApplications, parties, applicant, respondents, expectedPartCount, expectedDefendantCount);
    }

    public static void verifyUpdateApplication(final DocumentContext inputCourtApplication, final JsonObject transformedJson, final int expectedPartyCount, final int expectedDefendantCount) {

        final JsonArray outputCourtApplications = getCaseDetails(transformedJson).getJsonArray("applications");
        final JsonArray parties = getCaseDetails(transformedJson).getJsonArray("parties");

        final JsonObject inputApplication = inputCourtApplication.read("$.courtApplication");

        final JsonObject applicant = inputApplication.getJsonObject("applicant");
        final JsonArray respondents = inputApplication.containsKey("respondents") ? inputApplication.getJsonArray("respondents") : createArrayBuilder().build();

        verifyApplication(inputCourtApplication, outputCourtApplications, parties, applicant, respondents, expectedPartyCount, expectedDefendantCount);

        final String sourceApplicationReference = ((JsonString) inputCourtApplication.read("$.courtApplication.applicationReference")).getString();
        final JsonObject transformedApplication = getCaseDetails(transformedJson).getJsonArray("applications").getJsonObject(0);
        assertThat(transformedApplication, is(notNullValue()));

        final JsonString applicationReferenceValue = transformedApplication.getJsonString("applicationReference");
        assertThat(applicationReferenceValue, is(notNullValue()));

        assertThat(sourceApplicationReference, is(applicationReferenceValue.getString()));

    }

    public static void verifyExtendHearing(final DocumentContext inputCourtApplication, final JsonObject transformedJson, final int expectedPartCount, final int expectedDefendantCount) {

        final JsonArray outputCourtApplications = transformedJson.getJsonArray("applications");
        final JsonArray parties = transformedJson.getJsonArray("parties");
        final JsonObject applicant = inputCourtApplication.read("$.courtApplication.applicant");
        final JsonArray respondents = inputCourtApplication.read("$.courtApplication.respondents");
        verifyApplication(inputCourtApplication, outputCourtApplications, parties, applicant, respondents, expectedPartCount, expectedDefendantCount);
    }

    private static void verifyArn(final DocumentContext courtApplication, final JsonArray outputCourtApplications) {

        final Object arnValue = courtApplication.read("$.arn");
        if (arnValue != null) {
            final String arn = ((JsonString) arnValue).getString();
            final JsonObject outputApplication = outputCourtApplications.getJsonObject(0);
            assertThat(arn, is(outputApplication.getString("applicationReference")));
        }
    }

    private static void verifyApplication(final DocumentContext courtApplication, final JsonArray outputCourtApplications) {

        final String id = ((JsonString) courtApplication.read("$.courtApplication.id")).getString();
        final String applicationType = ((JsonString) courtApplication.read("$.courtApplication.type.type")).getString();
        final String applicationReceivedDate = ((JsonString) courtApplication.read("$.courtApplication.applicationReceivedDate")).getString();
        final String applicationDecisionSoughtByDate = ((JsonString) courtApplication.read("$.courtApplication.applicationDecisionSoughtByDate")).getString();
        final String dueDate = ((JsonString) courtApplication.read("$.courtApplication.dueDate")).getString();
        final JsonObject outputApplication = outputCourtApplications.getJsonObject(0);
        assertThat(id, is(outputApplication.getString("applicationId")));
        assertThat(applicationType, is(outputApplication.getString("applicationType")));
        assertThat(applicationReceivedDate, is(outputApplication.getString("receivedDate")));
        assertThat(applicationDecisionSoughtByDate, is(outputApplication.getString("decisionDate")));
        final JsonObject inputApplication = courtApplication.read("$.courtApplication");
        if(inputApplication.containsKey("subject")){
            if(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").containsKey("personDefendant")){
                assertThat(outputApplication.getJsonObject("subjectSummary").getString("firstName"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("personDefendant").getJsonObject("personDetails").getString("firstName")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getString("lastName"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("personDefendant").getJsonObject("personDetails").getString("lastName")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getString("dateOfBirth"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("personDefendant").getJsonObject("personDetails").getString("dateOfBirth")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getString("masterDefendantId"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getString("masterDefendantId")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getString("subjectId"), is(inputApplication.getJsonObject("subject").getString("id")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("address1"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("personDefendant").getJsonObject("personDetails").getJsonObject("address").getString("address1")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("address2"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("personDefendant").getJsonObject("personDetails").getJsonObject("address").getString("address2")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("address3"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("personDefendant").getJsonObject("personDetails").getJsonObject("address").getString("address3")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("address4"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("personDefendant").getJsonObject("personDetails").getJsonObject("address").getString("address4")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("address5"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("personDefendant").getJsonObject("personDetails").getJsonObject("address").getString("address5")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("postCode"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("personDefendant").getJsonObject("personDetails").getJsonObject("address").getString("postcode")));
            } else {
                assertThat(outputApplication.getJsonObject("subjectSummary").getString("organisationName"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("legalEntityDefendant").getJsonObject("organisation").getString("name")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getString("masterDefendantId"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getString("masterDefendantId")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getString("subjectId"), is(inputApplication.getJsonObject("subject").getString("id")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("address1"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("legalEntityDefendant").getJsonObject("organisation").getJsonObject("address").getString("address1")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("address2"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("legalEntityDefendant").getJsonObject("organisation").getJsonObject("address").getString("address2")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("address3"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("legalEntityDefendant").getJsonObject("organisation").getJsonObject("address").getString("address3")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("address4"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("legalEntityDefendant").getJsonObject("organisation").getJsonObject("address").getString("address4")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("address5"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("legalEntityDefendant").getJsonObject("organisation").getJsonObject("address").getString("address5")));
                assertThat(outputApplication.getJsonObject("subjectSummary").getJsonObject("address").getString("postCode"), is(inputApplication.getJsonObject("subject").getJsonObject("masterDefendant").getJsonObject("legalEntityDefendant").getJsonObject("organisation").getJsonObject("address").getString("postcode")));
            }

        }
    }

    private static void verifyApplicantTransformationWhenNoOrganisation(final JsonObject applicant, final JsonArray parties, final long expectedDefendantCount) {

        final String applicantId = applicant.getString("id");

        final JsonObject applicantDefendant = applicant.getJsonObject("masterDefendant");
        final JsonObject personDetails = applicant.getJsonObject("personDetails");

        if (applicantDefendant != null) {
            verifyDefendant(applicantDefendant, parties);
        } else if (personDetails != null) {
            verifyApplicantPersonDetails(personDetails, parties, applicantId);
        }

        final Stream<JsonObject> defendantPartyStream = parties.stream().map(JsonObject.class::cast)
                .filter(p -> p.getString("_party_type").equalsIgnoreCase("defendant"));

        final long defendantCount = defendantPartyStream.count();
        assertThat(defendantCount, is(expectedDefendantCount));

    }


    private static void verifyApplicantTransformation(final JsonObject applicant, final JsonArray parties, final long expectedDefendantCount) {
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
        assertThat(defendantCount, is(expectedDefendantCount));

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
                .filter(organisationPersonPredicate()) //we are looking for orgPersons only
                .forEach(applicantParty -> {

                    assertThat(applicantId, is(applicantParty.getString("partyId")));

                    final JsonObject organisationPerson = organisationPersons.getJsonObject(organisationPersonsIndex.getAndIncrement());
                    final JsonObject person = organisationPerson.getJsonObject("person");

                    final JsonObject organisation = applicant.getJsonObject("organisation");
                    if (organisation != null) {
                        final String organisationName = organisation.getString("name");
                        final JsonString applicantLastName = applicantParty.getJsonString("lastName");
                        if (applicantLastName != null) {
                            assertApplicantDetails(person, applicantParty, organisationName);
                        } else {
                            assertOrganisationDetails(person, applicantParty);
                        }
                    }

                    final String addressLines = applicantParty.getString("addressLines");
                    assertAddressDetails(person.getJsonObject("address"), addressLines, applicantParty.getString("postCode"),applicantParty.getJsonObject("defendantAddress"));
                });

    }

    private static void verifyRespondentsTransformation(final JsonArray respondents, final JsonArray parties, final long expectedDefendantCount) {

        assertThat(respondents.size(), greaterThan(0));

        respondents.forEach(respondent -> {

            final JsonObject respondentsJsonObject = (JsonObject) respondent;
            final String respondentId = respondentsJsonObject.getString("id");
            final JsonObject respondentDetails = respondentsJsonObject.getJsonObject("personDetails");

            final long applicantCount = parties.stream()
                    .filter(p -> ((JsonObject) p).getString("_party_type").equalsIgnoreCase("respondent"))
                    .count();
            assertThat(applicantCount, greaterThan(0l));

            parties.stream()
                    .map(JsonObject.class::cast)
                    .filter(p -> p.getString("_party_type").equalsIgnoreCase("respondent"))
                    .forEach(respondentParty -> {

                        assertThat(respondentId, is(respondentParty.getString("partyId")));
                        final JsonObject organisation = respondentsJsonObject.getJsonObject("organisation");
                        if (respondentDetails != null && organisation != null) {
                            final String organisationName = organisation.getString("name");
                            assertRespondantDetails(respondentDetails, respondentParty, organisationName);
                        }

                        final JsonObject defendant = respondentsJsonObject.getJsonObject("masterDefendant");
                        assertThat(defendant, is(notNullValue()));
                        final long defendantCount = parties.stream()
                                .filter(p -> ((JsonObject) p).getString("_party_type").equalsIgnoreCase("defendant"))
                                .count();
                        assertThat(defendantCount, is(expectedDefendantCount));
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

        assertThat(applicant.isPresent(), is(Boolean.TRUE));
        assertPersonDetails(personDetails, applicant.get());

    }

    private static void verifyApplication(final DocumentContext inputCourtApplication,
                                          final JsonArray outputCourtApplications,
                                          final JsonArray parties,
                                          final JsonObject applicant,
                                          final JsonArray respondents, final int expectedPartyCount, final int expectedDefendantCount) {
        assertThat(inputCourtApplication, is(notNullValue()));

        assertThat(parties, hasSize(expectedPartyCount));

        verifyApplication(inputCourtApplication, outputCourtApplications);

        verifyApplicantTransformation(applicant, parties, expectedDefendantCount);

        if (!respondents.isEmpty()) {
            verifyRespondentsTransformation(respondents, parties, expectedDefendantCount);
        }
    }

    private static void verifyApplicationWithNoApplicantOrganisation(final DocumentContext inputCourtApplication,
                                                                     final JsonArray outputCourtApplications,
                                                                     final JsonArray parties,
                                                                     final JsonObject applicant,
                                                                     final JsonArray respondents, final int expectedPartyCount, final int expectedDefendantCount) {
        assertThat(inputCourtApplication, is(notNullValue()));
        assertThat(parties, hasSize(expectedPartyCount));

        verifyApplication(inputCourtApplication, outputCourtApplications);

        verifyApplicantTransformationWhenNoOrganisation(applicant, parties, expectedDefendantCount);

        verifyRespondentsTransformation(respondents, parties, expectedDefendantCount);
    }

    public static void verifyAddApplicationWithoutRespondent(final DocumentContext inputCourtApplication, final JsonObject transformedJson, final int expectedPartyCount, final int expectedDefendantCount) {

        final JsonArray outputCourtApplications = transformedJson.getJsonArray("caseDocuments").getJsonObject(0).getJsonArray("applications");
        final JsonArray parties = transformedJson.getJsonArray("caseDocuments").getJsonObject(0).getJsonArray("parties");
        final JsonObject applicant = inputCourtApplication.read("$.courtApplication.applicant");

        assertThat(inputCourtApplication, is(notNullValue()));
        assertThat(outputCourtApplications, is(notNullValue()));
        assertThat(parties, hasSize(expectedPartyCount));

        verifyArn(inputCourtApplication, outputCourtApplications);
        verifyApplication(inputCourtApplication, outputCourtApplications);
        verifyApplicantTransformationWithoutRespondent(applicant, parties, expectedDefendantCount);
    }

    private static void verifyApplicantTransformationWithoutRespondent(final JsonObject applicant, final JsonArray parties, final long expectedDefendantCount) {
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
        assertThat(defendantCount, is(expectedDefendantCount));

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
                .filter(organisationPersonPredicate()) // we are looking for person Organisations only
                .forEach(applicantParty -> {

                    assertThat(applicantId, is(applicantParty.getString("partyId")));

                    final JsonObject organisationPerson = organisationPersons.getJsonObject(organisationPersonsIndex.getAndIncrement());
                    final JsonObject person = organisationPerson.getJsonObject("person");

                    final JsonObject organisation = applicant.getJsonObject("organisation");
                    if (organisation != null) {
                        final String organisationName = organisation.getString("name");
                        final JsonString applicantLastName = applicantParty.getJsonString("lastName");
                        if (applicantLastName != null) {
                            assertApplicantDetails(person, applicantParty, organisationName);
                        } else {
                            assertOrganisationDetails(person, applicantParty);
                        }
                    }

                    final String addressLines = applicantParty.getString("addressLines");
                    assertAddressDetails(person.getJsonObject("address"), addressLines, applicantParty.getString("postCode"), applicantParty.getJsonObject("defendantAddress"));
                });
    }

    private static Predicate<JsonObject> organisationPersonPredicate() {
        return p -> p.getString("lastName", null) != null;
    }

    private static JsonObject getCaseDetails(final JsonObject transformedJson) {
        return transformedJson.getJsonArray("caseDocuments").getJsonObject(0);
    }
}
