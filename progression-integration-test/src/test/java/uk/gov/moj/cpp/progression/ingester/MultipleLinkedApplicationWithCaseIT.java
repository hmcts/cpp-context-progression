package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.fail;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyAddCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyEmbeddedApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyUpdateCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;

import uk.gov.moj.cpp.progression.AbstractIT;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({"squid:S1607", "squid:S2925"})
public class MultipleLinkedApplicationWithCaseIT extends AbstractIT {
    private static final String CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.create-court-application-embedded.json";
    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";
    private static final String UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.update-court-application-embedded.json";

    private String caseId;
    private String defendantId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String referralReasonId;
    private String applicantId1;
    private String applicantDefendantId1;
    private String respondentId1;
    private String respondentDefendantId1;
    private String applicantId2;
    private String applicantDefendantId2;
    private String respondentId2;
    private String respondentDefendantId2;
    private String applicationId1;
    private String applicationId2;
    private String applicationReference;

    @Before
    public void setup() {
        deleteAndCreateIndex();
    }

    @AfterClass
    public static void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    @Test
    public void shouldCreateMultipleEmbeddedCourtApplicationAndGetConfirmation() throws Exception {

        this.initializeIds();

        final String caseUrn = generateUrn();
        addProsecutionCaseToCrownCourtForIngestion(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);

        addCourtApplicationForIngestion(caseId, applicationId1, applicantId1, applicantDefendantId1, respondentId1, respondentDefendantId1, CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        addCourtApplicationForIngestion(caseId, applicationId2, applicantId2, applicantDefendantId2, respondentId2, respondentDefendantId2, CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        final Optional<JsonObject> prosecutionCaseResponseJsonObject = getPoller().pollUntilFound(() -> {
            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1 && isPartiesPopulated(jsonObject, 9)) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }

            return empty();
        });

        final String payloadStr1 = getStringFromResource(CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId1)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId1)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId1)
                .replaceAll("RANDOM_RESPONDANT_ID", respondentId1)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondentDefendantId1)
                .replaceAll("RANDOM_REFERENCE", applicationReference);
        final JsonObject inputApplication1 = jsonFromString(payloadStr1);

        final DocumentContext inputCourtApplication1 = parse(inputApplication1);
        final String payloadStr2 = getStringFromResource(CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId2)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId2)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId2)
                .replaceAll("RANDOM_RESPONDANT_ID", respondentId2)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondentDefendantId2)
                .replaceAll("RANDOM_REFERENCE", applicationReference);
        final JsonObject inputApplication2 = jsonFromString(payloadStr2);

        final DocumentContext inputCourtApplication2 = parse(inputApplication2);

        final JsonObject transformedJson = jsonFromString(getJsonArray(prosecutionCaseResponseJsonObject.get(), "index").get().getString(0));
        final DocumentContext inputProsecutionCase = documentContext(caseUrn);

        verifyCaseCreated(9l, inputProsecutionCase, transformedJson);
        final String linkedCaseId1 = ((JsonString) inputCourtApplication1.read("$.application.linkedCaseId")).getString();
        final String linkedCaseId2 = ((JsonString) inputCourtApplication2.read("$.application.linkedCaseId")).getString();
        verifyEmbeddedApplication(linkedCaseId1, transformedJson);
        verifyEmbeddedApplication(linkedCaseId2, transformedJson);
        verifyAddCourtApplication(inputCourtApplication1, transformedJson, applicationId1);
        verifyAddCourtApplication(inputCourtApplication2, transformedJson, applicationId2);
    }

    @Test
    public void shouldCreateMultipleEmbeddedCourtApplicationAndGetConfirmationAndVerifyUpdate() throws Exception {

        this.initializeIds();

        final String caseUrn = generateUrn();
        addProsecutionCaseToCrownCourtForIngestion(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);

        addCourtApplicationForIngestion(caseId, applicationId1, applicantId1, applicantDefendantId1, respondentId1, respondentDefendantId1, CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        addCourtApplicationForIngestion(caseId, applicationId2, applicantId2, applicantDefendantId2, respondentId2, respondentDefendantId2, CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        updateCourtApplicationForIngestion(caseId, applicationId1, applicantId1, applicantDefendantId1, respondentId1, respondentDefendantId1, applicationReference, UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        final Optional<JsonObject> prosecussionCaseResponseJsonObject = getPoller().pollUntilFound(() -> {
            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1 && isPartiesPopulated(jsonObject, 9)) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }

            return empty();
        });

        final String payloadStr1 = getStringFromResource(UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId1)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId1)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId1)
                .replaceAll("RANDOM_RESPONDANT_ID", respondentId1)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondentDefendantId1)
                .replaceAll("RANDOM_REFERENCE", applicationReference);
        final JsonObject inputApplication1 = jsonFromString(payloadStr1);

        final DocumentContext inputCourtApplication1 = parse(inputApplication1);
        final String payloadStr2 = getStringFromResource(CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId2)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId2)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId2)
                .replaceAll("RANDOM_RESPONDANT_ID", respondentId2)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondentDefendantId2)
                .replaceAll("RANDOM_REFERENCE", applicationReference);
        final JsonObject inputApplication2 = jsonFromString(payloadStr2);
        final DocumentContext inputCourtApplication2 = parse(inputApplication2);

        final JsonObject transformedJson = jsonFromString(getJsonArray(prosecussionCaseResponseJsonObject.get(), "index").get().getString(0));
        final DocumentContext inputProsecutionCase = documentContext(caseUrn);

        verifyCaseCreated(9l, inputProsecutionCase, transformedJson);
        final String linkedCaseId1 = ((JsonString) inputCourtApplication1.read("$.courtApplication.linkedCaseId")).getString();
        final String linkedCaseId2 = ((JsonString) inputCourtApplication2.read("$.application.linkedCaseId")).getString();
        verifyEmbeddedApplication(linkedCaseId1, transformedJson);
        verifyEmbeddedApplication(linkedCaseId2, transformedJson);
        verifyAddCourtApplication(inputCourtApplication2, transformedJson, applicationId2);
        verifyUpdateCourtApplication(inputCourtApplication1, transformedJson, applicationId1);
    }

    private boolean isPartiesPopulated(final JsonObject jsonObject, final int partySize) {
        final JsonObject indexData = jsonFromString(getJsonArray(jsonObject, "index").get().getString(0));
        return indexData.containsKey("parties") && (indexData.getJsonArray("parties").size() == partySize);
    }

    private DocumentContext documentContext(final String caseUrn) throws IOException {

        final String commandJson = getReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                courtDocumentId, randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);
        final JsonObject commandJsonInputJson = jsonFromString(commandJson);
        final DocumentContext prosecutionCase = parse(commandJsonInputJson);
        final JsonObject prosecutionCaseJO = prosecutionCase.read("$.courtReferral.prosecutionCases[0]");
        final JsonObject prosecutionCaseEvent = Json.createObjectBuilder().add("prosecutionCase", prosecutionCaseJO).build();
        return parse(prosecutionCaseEvent);
    }

    private void initializeIds(){
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        courtDocumentId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        applicantId1 = randomUUID().toString();
        applicantDefendantId1 = randomUUID().toString();
        respondentId1 = randomUUID().toString();
        respondentDefendantId1 = randomUUID().toString();
        applicantId2 = randomUUID().toString();
        applicantDefendantId2 = randomUUID().toString();
        respondentId2 = randomUUID().toString();
        respondentDefendantId2 = randomUUID().toString();
        applicationId1 = randomUUID().toString();
        applicationId2 = randomUUID().toString();
        applicationReference =  randomAlphanumeric(10).toUpperCase();
    }
}

