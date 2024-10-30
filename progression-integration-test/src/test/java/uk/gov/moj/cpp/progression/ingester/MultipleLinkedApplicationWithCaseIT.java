package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.createReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyAddCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyEmbeddedApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyUpdateCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.AbstractIT;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private String applicationStatus;

    @BeforeEach
    public void setup() {
        deleteAndCreateIndex();
        initializeIds();
    }

    @AfterEach
    public void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    private void verifyMessageReceived(final JmsMessageConsumerClient messageConsumer) {
        assertThat(retrieveMessageBody(messageConsumer).isPresent(), is(true));
    }

    @Test
    public void shouldCreateMultipleEmbeddedCourtApplicationAndGetConfirmationAndVerifyUpdate() throws Exception {

        final String caseUrn = applicationReference;
        addProsecutionCaseToCrownCourtForIngestion(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);

        final JmsMessageConsumerClient messageConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-application-proceedings-initiated").getMessageConsumerClient();

        addCourtApplicationForIngestion(caseId, applicationId1, applicantId1, applicantDefendantId1,
                respondentId1,
                respondentDefendantId1,
                applicationReference, applicationStatus, CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);
        verifyMessageReceived(messageConsumer);

        final Matcher[] addApplication1Matcher = {allOf(
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.applications[*].applicationId", hasItem(applicationId1)),
                withJsonPath("$.parties[?(@._party_type=='RESPONDENT')].firstName", hasItem(equalTo("a"))))};
        Optional<JsonObject> addApplication1ResponseJsonObject = findBy(addApplication1Matcher);
        assertThat(addApplication1ResponseJsonObject.isPresent(), is(true));

        final JmsMessageConsumerClient initiatedMessageConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-application-proceedings-initiated").getMessageConsumerClient();
        addCourtApplicationForIngestion(caseId, applicationId2, applicantId2, applicantDefendantId2, respondentId2, respondentDefendantId2, applicationReference, applicationStatus, CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);
        verifyMessageReceived(initiatedMessageConsumer);

        final Matcher[] addApplication2Matcher = {allOf(
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.applications[*].applicationId", hasItem(applicationId2)),
                withJsonPath("$.parties[?(@._party_type=='RESPONDENT')].firstName", hasItem(equalTo("a"))))};
        Optional<JsonObject> addApplication2ResponseJsonObject = findBy(addApplication2Matcher);
        assertThat(addApplication2ResponseJsonObject.isPresent(), is(true));

        final JmsMessageConsumerClient editedMessageConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-application-proceedings-edited").getMessageConsumerClient();
        updateCourtApplicationForIngestion(caseId, applicationId1, applicantId1, applicantDefendantId1, respondentId1, respondentDefendantId1, applicationReference, UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);
        verifyMessageReceived(editedMessageConsumer);

        final Matcher[] updateApplication1Matcher = {allOf(
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.applications[*].applicationId", hasItem(applicationId1)),
                withJsonPath("$.parties[?(@._party_type=='RESPONDENT')].firstName", hasItem(equalTo("respondantA"))))};
        Optional<JsonObject> updateApplication1ResponseJsonObject = findBy(updateApplication1Matcher);
        assertThat(updateApplication1ResponseJsonObject.isPresent(), is(true));

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

        final Matcher[] caseMatchers = {allOf(
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.caseReference", equalTo(caseUrn)),
                withJsonPath("$.parties.length()", equalTo(5)),
                withJsonPath("$.applications.length()", equalTo(2)),
                withJsonPath("$.applications[*].applicationReference", hasItem(applicationReference)))};

        Optional<JsonObject> prosecutionCaseResponseJsonObject = findBy(caseMatchers);
        assertThat(prosecutionCaseResponseJsonObject.isPresent(), is(true));
        final JsonObject transformedJson = prosecutionCaseResponseJsonObject.get();
        final DocumentContext inputProsecutionCase = documentContext(caseUrn);

        verifyCaseCreated(5l, inputProsecutionCase, transformedJson);
        final String linkedCaseId1 = ((JsonString) inputCourtApplication1.read("$.courtApplication.courtApplicationCases[0].prosecutionCaseId")).getString();
        final String linkedCaseId2 = ((JsonString) inputCourtApplication2.read("$.courtApplication.courtApplicationCases[0].prosecutionCaseId")).getString();
        verifyEmbeddedApplication(linkedCaseId1, transformedJson);
        verifyEmbeddedApplication(linkedCaseId2, transformedJson);
        verifyAddCourtApplication(inputCourtApplication1, transformedJson, applicationId1);
        verifyAddCourtApplication(inputCourtApplication2, transformedJson, applicationId2);
        verifyUpdateCourtApplication(inputCourtApplication1, transformedJson, applicationId1);
    }

    private DocumentContext documentContext(final String caseUrn) throws IOException {

        final String commandJson = createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                courtDocumentId, randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);
        final JsonObject commandJsonInputJson = jsonFromString(commandJson);
        final DocumentContext prosecutionCase = parse(commandJsonInputJson);
        final JsonObject prosecutionCaseJO = prosecutionCase.read("$.courtReferral.prosecutionCases[0]");
        final JsonObject prosecutionCaseEvent = Json.createObjectBuilder().add("prosecutionCase", prosecutionCaseJO).build();
        return parse(prosecutionCaseEvent);
    }

    private void initializeIds() {
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
        applicationReference = generateUrn();
    }
}

