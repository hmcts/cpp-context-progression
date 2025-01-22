package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyAddCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyStandaloneApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyUpdateCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.AbstractIT;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CourtApplicationUpdatedIngesterIT extends AbstractIT {
    private static final String CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.create-court-application.json";
    private static final String UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.update-court-application.json";
    private static final String CREATE_COURT_APPLICATION_WITHOUT_RESPONDENT_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.create-court-application-without-respondent.json";
    private static final String UPDATE_COURT_APPLICATION_WITHOUT_RESPONDENT_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.update-court-application-without-respondent.json";

    private String applicationId;
    private String caseId;
    private String applicantId;
    private String applicantDefendantId;
    private String respondentId;
    private String respondentDefendantId;
    private String applicationReference;
    private String applicationStatus;

    @BeforeEach
    public void setup() {
        applicationId = randomUUID().toString();
        caseId = randomUUID().toString();
        applicantId = randomUUID().toString();
        applicantDefendantId = randomUUID().toString();
        respondentId = randomUUID().toString();
        respondentDefendantId = randomUUID().toString();
        applicationReference = randomAlphanumeric(10).toUpperCase();
        applicationStatus = ApplicationStatus.DRAFT.toString();
        deleteAndCreateIndex();

    }

    @AfterEach
    public void tearDown() {
        cleanEventStoreTables();
    }

    private void verifyMessageReceived(final JmsMessageConsumerClient messageConsumer) {
        assertTrue(retrieveMessageBody(messageConsumer).isPresent());
    }

    @Test
    public void shouldUpdateCourtApplication() throws Exception {

        setUpCourtApplication(CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        final JmsMessageConsumerClient messageConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-application-proceedings-edited").getMessageConsumerClient();

        updateCourtApplicationForIngestion(applicationId, applicationId, applicantId, applicantDefendantId, respondentId, respondentDefendantId, applicationReference, UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);
        verifyMessageReceived(messageConsumer);

        final Matcher[] matchers = {withJsonPath("$.parties[?(@._party_type=='RESPONDENT')].firstName", hasItem(equalTo("updatedA")))};

        final Optional<JsonObject> courApplicationUpdatesResponseJsonObject = findBy(matchers);

        assertTrue(courApplicationUpdatesResponseJsonObject.isPresent());

        final DocumentContext updatedInputCourtApplication = loadInputUpdatedApplicationPayload(UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        verifyStandaloneApplication(applicationId, courApplicationUpdatesResponseJsonObject.get());
        verifyUpdateCourtApplication(updatedInputCourtApplication, courApplicationUpdatesResponseJsonObject.get(), applicationId);
    }

    @Test
    public void shouldUpdateCourtApplicationWithoutRespondent() throws Exception {

        setUpCourtApplication(CREATE_COURT_APPLICATION_WITHOUT_RESPONDENT_COMMAND_RESOURCE_LOCATION);

        final JmsMessageConsumerClient messageConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-application-proceedings-edited").getMessageConsumerClient();

        updateCourtApplicationForIngestion(applicationId, applicationId, applicantId, applicantDefendantId, respondentId, respondentDefendantId, applicationReference, UPDATE_COURT_APPLICATION_WITHOUT_RESPONDENT_COMMAND_RESOURCE_LOCATION);
        verifyMessageReceived(messageConsumer);

        final Matcher[] matchers = {withJsonPath("$.parties[?(@._party_type=='APPLICANT')].organisationName", hasItem(equalTo("updatedA")))};

        final Optional<JsonObject> courApplicationUpdatesResponseJsonObject = findBy(matchers);

        assertTrue(courApplicationUpdatesResponseJsonObject.isPresent());

        final DocumentContext updatedInputCourtApplication = loadInputUpdatedApplicationPayload(UPDATE_COURT_APPLICATION_WITHOUT_RESPONDENT_COMMAND_RESOURCE_LOCATION);

        verifyStandaloneApplication(applicationId, courApplicationUpdatesResponseJsonObject.get());
        verifyUpdateCourtApplication(updatedInputCourtApplication, courApplicationUpdatesResponseJsonObject.get(), applicationId);
    }

    private void setUpCourtApplication(final String payloadPath) throws IOException {
        addCourtApplicationForIngestion(applicationId, applicationId, applicantId, applicantDefendantId, respondentId, respondentDefendantId, applicationStatus, payloadPath);

        final Matcher[] matchers = {withJsonPath("$.caseId", equalTo(applicationId))};

        final Optional<JsonObject> courApplicationCreatedResponseJsonObject = findBy(matchers);

        assertTrue(courApplicationCreatedResponseJsonObject.isPresent());

        final String payloadStr = getStringFromResource(payloadPath)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId)
                .replaceAll("RANDOM_RESPONDANT_ID", respondentId)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondentDefendantId)
                .replaceAll("APPLICATION_STATUS", applicationStatus)
                .replaceAll("RANDOM_REFERENCE", applicationReference);

        final JsonObject inputApplication = jsonFromString(payloadStr);

        final DocumentContext inputCourtApplication = parse(inputApplication);

        verifyAddCourtApplication(inputCourtApplication, courApplicationCreatedResponseJsonObject.get(), applicationId);
    }

    private DocumentContext loadInputUpdatedApplicationPayload(final String updateCourtApplicationCommandResourceLocation) {
        final String payloadUpdatedStr = getStringFromResource(updateCourtApplicationCommandResourceLocation)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId)
                .replaceAll("RANDOM_RESPONDANT_ID", respondentId)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondentDefendantId)
                .replaceAll("RANDOM_REFERENCE", applicationReference);

        final JsonObject updateJson = jsonFromString(payloadUpdatedStr);
        return parse(updateJson);
    }
}
