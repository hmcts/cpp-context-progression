package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyAddCourtApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.CourtApplicationVerificationHelper.verifyEmbeddedApplication;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getStringFromResource;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;

import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;

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
public class EmbeddedCourtApplicationCreatedIT {
    private static final String CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.create-court-application-embedded.json";
    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";

    private String caseId;
    private String defendantId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String referralReasonId;
    private ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil;
    private String applicantId;
    private String applicantDefendantId;
    private String respondantId;
    private String respondantDefendantId;
    private String applicationId;


    @Before
    public void setUp() throws IOException {
        createMockEndpoints();
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        courtDocumentId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        applicantId = randomUUID().toString();
        applicantDefendantId = randomUUID().toString();
        respondantId = randomUUID().toString();
        respondantDefendantId = randomUUID().toString();
        applicationId = randomUUID().toString();

        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();
        final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);
    }

    @AfterClass
    public static void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    @Test
    public void shouldCreateCourtApplicationAndGetConfirmation() throws Exception {

        final String caseUrn = generateUrn();
        addProsecutionCaseToCrownCourtForIngestion(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);

        addCourtApplicationForIngestion(caseId, applicationId, applicantId, applicantDefendantId, respondantId, respondantDefendantId, CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);


        final Optional<JsonObject> prosecussionCaseResponseJsonObject = getPoller().pollUntilFound(() -> {
            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1 && isPartiesPopulated(jsonObject, 5)) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }

            return empty();
        });

        assertTrue(prosecussionCaseResponseJsonObject.isPresent());

        final String payloadStr = getStringFromResource(CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId)
                .replaceAll("RANDOM_RESPONDANT_ID", respondantId)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondantDefendantId)
                .replaceAll("RANDOM_REFERENCE", UUID.randomUUID().toString());


        final JsonObject inputApplication = jsonFromString(payloadStr);

        final DocumentContext inputCourtApplication = parse(inputApplication);

        final JsonObject transformedJson = jsonFromString(getJsonArray(prosecussionCaseResponseJsonObject.get(), "index").get().getString(0));
        final DocumentContext inputProsecutionCase = documentContext(caseUrn);
        verifyCaseCreated(5l, inputProsecutionCase, transformedJson);
        final String linkedCaseId = ((JsonString) inputCourtApplication.read("$.application.linkedCaseId")).getString();
        verifyEmbeddedApplication(linkedCaseId, transformedJson);
        verifyAddCourtApplication(inputCourtApplication, transformedJson, applicationId);
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
}

