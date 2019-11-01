package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyAddCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyStandaloneApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyUpdateCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import com.jayway.jsonpath.DocumentContext;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;

import javax.json.JsonObject;
import java.io.IOException;
import java.util.Optional;

public class CourtApplicationUpdatedIngesterIT {
    private static final String CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.create-court-application.json";
    private static final String UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.update-court-application.json";
    private String applicationId;
    private String caseId;
    private String applicantId;
    private String applicantDefendantId;
    private String respondantId;
    private String respondantDefendantId;
    private String applicationReference;

    @BeforeClass
    public static void beforeClass() {
        createMockEndpoints();
    }

    @Before
    public void setUp() throws IOException {
        applicationId = randomUUID().toString();
        caseId = randomUUID().toString();
        applicantId = randomUUID().toString();
        applicantDefendantId = randomUUID().toString();
        respondantId = randomUUID().toString();
        respondantDefendantId = randomUUID().toString();
        applicationReference =  randomAlphanumeric(10).toUpperCase();

        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();
    }

    @AfterClass
    public static void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    @Test
    public void shouldUpdateCourtApplication() throws Exception {

        setUpCourtApplication();

        updateCourtApplicationForIngestion(applicationId, applicationId, applicantId, applicantDefendantId, respondantId, respondantDefendantId,
                                           applicationReference, UPDATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        final Matcher[] matchers = {withJsonPath("$.parties[*].firstName", hasItem(equalTo("updatedA")))};

         final Optional<JsonObject> courApplicationUpdatesResponseJsonObject = findBy(matchers);

        assertTrue(courApplicationUpdatesResponseJsonObject.isPresent());

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

        verifyStandaloneApplication(applicationId, courApplicationUpdatesResponseJsonObject.get());
        verifyUpdateCourtApplication(updatedInputCourtApplication, courApplicationUpdatesResponseJsonObject.get(), applicationId);
    }

    private void setUpCourtApplication() throws IOException {
        addCourtApplicationForIngestion(applicationId, applicationId, applicantId, applicantDefendantId, respondantId, respondantDefendantId, CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        final Matcher[] matchers = {withJsonPath("$.caseId", equalTo(applicationId))};

        final Optional<JsonObject> courApplicationCreatedResponseJsonObject = findBy(matchers);

        assertTrue(courApplicationCreatedResponseJsonObject.isPresent());

        final String payloadStr = getStringFromResource(CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId)
                .replaceAll("RANDOM_RESPONDANT_ID", respondantId)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondantDefendantId)
                .replaceAll("RANDOM_REFERENCE", applicationReference);

        final JsonObject inputApplication = jsonFromString(payloadStr);

        final DocumentContext inputCourtApplication = parse(inputApplication);

        verifyAddCourtApplication(inputCourtApplication, courApplicationCreatedResponseJsonObject.get(), applicationId);
    }
}
