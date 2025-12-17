package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.createReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyAddCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyEmbeddedApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyUpdateCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.moj.cpp.progression.AbstractIT;

import java.io.IOException;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607", "squid:S2925"})
public class EmbeddedCourtApplicationUpdatedIT extends AbstractIT {

    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";
    private static final String CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.create-court-application-embedded.json";
    private static final String UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.update-court-application-embedded.json";

    private String caseId;
    private String defendantId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String referralReasonId;
    private String applicantId;
    private String applicantDefendantId;
    private String respondantId;
    private String respondantDefendantId;
    private String applicationId;
    private String applicationReference;
    private String applicationStatus;

    @BeforeEach
    public void setup() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        courtDocumentId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        applicantId = randomUUID().toString();
        applicantDefendantId = randomUUID().toString();
        respondantId = randomUUID().toString();
        respondantDefendantId = randomUUID().toString();
        applicationId = randomUUID().toString();
        applicationStatus = ApplicationStatus.DRAFT.toString();
        applicationReference = randomAlphanumeric(10).toUpperCase();
        deleteAndCreateIndex();
    }

    @Test
    public void shouldUpdateCourtApplicationAndGetConfirmation() throws Exception {
        setUpCourtApplication();

        updateCourtApplicationForIngestion(caseId, applicationId, applicantId, applicantDefendantId, respondantId, respondantDefendantId, applicationReference, UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        final Matcher[] updateApplicationMatcher = {allOf(
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.applications[*].applicationId", hasItem(applicationId)),
                withJsonPath("$.parties[?(@._party_type=='RESPONDENT')].firstName", hasItem(equalTo("respondantA"))))};
        Optional<JsonObject> updateApplicationResponseJsonObject = findBy(updateApplicationMatcher);

        assertThat(updateApplicationResponseJsonObject.isPresent(), CoreMatchers.is(true));

        final String payloadUpdatedStr = getStringFromResource(UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId)
                .replaceAll("RANDOM_RESPONDANT_ID", respondantId)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondantDefendantId)
                .replaceAll("RANDOM_REFERENCE", applicationReference);

        final JsonObject updateJson = jsonFromString(payloadUpdatedStr);
        final DocumentContext updatedInputCourtApplication = parse(updateJson);

        final JsonObject outputUpdatedJson = updateApplicationResponseJsonObject.get();

        verifyUpdateCourtApplication(updatedInputCourtApplication, outputUpdatedJson, applicationId);
    }

    private String setUpCourtApplication() throws Exception {
        final String caseUrn = generateUrn();
        addProsecutionCaseToCrownCourtForIngestion(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION)
                .then().assertThat().statusCode(is(HttpStatus.SC_ACCEPTED));

        final Matcher[] caseMatcher = {allOf(
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.parties[?(@._party_type=='DEFENDANT')]", hasSize(1)))};
        Optional<JsonObject> addCaseResponseJsonObject = findBy(caseMatcher);
        assertThat(addCaseResponseJsonObject.isPresent(), CoreMatchers.is(true));

        addCourtApplicationForIngestion(caseId, applicationId, applicantId, applicantDefendantId, respondantId, respondantDefendantId, applicationStatus, CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION)
                .then().assertThat().statusCode(is(HttpStatus.SC_ACCEPTED));

        final Matcher[] addApplicationMatcher = {allOf(
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.applications[*].applicationId", hasItem(applicationId)),
                withJsonPath("$.parties[?(@._party_type=='RESPONDENT')].firstName", hasItem(equalTo("a"))))};
        Optional<JsonObject> addApplicationResponseJsonObject = findBy(addApplicationMatcher);
        assertThat(addApplicationResponseJsonObject.isPresent(), CoreMatchers.is(true));

        final Optional<JsonObject> prosecutionCaseResponseJsonObject = getPoller().pollUntilFound(() -> {
            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1 && isPartiesPopulated(jsonObject, 3)) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }
            return empty();
        });

        assertTrue(prosecutionCaseResponseJsonObject.isPresent());

        final String applicationCreatedIndex = getJsonArray(prosecutionCaseResponseJsonObject.get(), "index").get().getString(0);
        final String payloadStr = getStringFromResource(CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId)
                .replaceAll("RANDOM_RESPONDANT_ID", respondantId)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondantDefendantId)
                .replaceAll("RANDOM_REFERENCE", applicationReference);


        final JsonObject inputApplication = jsonFromString(payloadStr);
        final DocumentContext inputCourtApplication = parse(inputApplication);
        final JsonObject transformedJson = jsonFromString(getJsonArray(prosecutionCaseResponseJsonObject.get(), "index").get().getString(0));
        final DocumentContext inputProsecutionCase = documentContext(caseUrn);
        verifyCaseCreated(3, inputProsecutionCase, transformedJson);
        final String linkedCaseId = ((JsonString) inputCourtApplication.read("$.courtApplication.courtApplicationCases[0].prosecutionCaseId")).getString();
        verifyEmbeddedApplication(linkedCaseId, transformedJson);
        verifyAddCourtApplication(inputCourtApplication, transformedJson, applicationId);
        return applicationCreatedIndex;
    }

    private boolean isPartiesPopulated(final JsonObject jsonObject, final int partySize) {
        final JsonObject indexData = jsonFromString(getJsonArray(jsonObject, "index").get().getString(0));
        return indexData.containsKey("parties") && (indexData.getJsonArray("parties").size() == partySize);
    }

    private DocumentContext documentContext(final String caseUrn) {
        final String commandJson = createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                courtDocumentId, randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);
        final JsonObject commandJsonInputJson = jsonFromString(commandJson);
        final DocumentContext prosecutionCase = parse(commandJsonInputJson);
        final JsonObject prosecutionCaseJO = prosecutionCase.read("$.courtReferral.prosecutionCases[0]");
        final JsonObject prosecutionCaseEvent = Json.createObjectBuilder().add("prosecutionCase", prosecutionCaseJO).build();
        return parse(prosecutionCaseEvent);
    }
}

