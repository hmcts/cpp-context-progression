package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseDefendant;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertcourtDocuments;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.jsonpath.DocumentContext;
import org.junit.Before;
import org.junit.Test;


public class InitiateCourtProceedingsIT {

    private String caseId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;
    private static final String INITIAL_COURT_PROCEEDINGS = "ingestion/progression.command.initiate-court-proceedings.json";

    private ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        materialIdActive = UUID.randomUUID().toString();
        materialIdDeleted = UUID.randomUUID().toString();
        courtDocumentId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        referralReasonId = UUID.randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();

        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();
        final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);

    }

    @Test
    public void shouldInitiateCourtProceedingsWithCourtDocuments() throws IOException {
        createMockEndpoints();
        final String caseUrn = generateUrn();
        //given
        initiateCourtProceedings(INITIAL_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, caseUrn, listedStartDateTime, earliestStartDateTime, defendantDOB);

        //introduce delay by checking court document present first
        getCourtDocumentFor(courtDocumentId, withJsonPath("$.courtDocument.courtDocumentId", equalTo(courtDocumentId)));

        final String response = getProsecutioncasesProgressionFor(caseId);
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        //then
        assertProsecutionCase(prosecutionCasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
        assertcourtDocuments(prosecutionCasesJsonObject.getJsonArray("courtDocuments").getJsonObject(0), caseId, courtDocumentId, materialIdActive);

        final DocumentContext inputProsecutionCase = documentContext(caseUrn);

        final Optional<JsonObject> prosecutionCaseResponseJsonObject = getPoller().pollUntilFound(() -> {
            try {
                final JsonObject jsonObject1 = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject1.getInt("totalResults") == 1 && isPartiesPopulated(jsonObject1, 1)) {
                    return of(jsonObject1);
                }
            } catch (final IOException e) {
                fail();
            }

            return empty();
        });

        final JsonObject outputCase = jsonFromString(getJsonArray(prosecutionCaseResponseJsonObject.get(), "index").get().getString(0));
        verifyCaseCreated(1l, inputProsecutionCase, outputCase);
        verifyCaseDefendant(inputProsecutionCase, outputCase,false);
    }

    private boolean isPartiesPopulated(final JsonObject jsonObject, final int partySize) {
        final JsonObject indexData = jsonFromString(getJsonArray(jsonObject, "index").get().getString(0));
        return indexData.containsKey("parties") && (indexData.getJsonArray("parties").size() == partySize);
    }

    private DocumentContext documentContext(final String caseUrn) throws IOException {

        final String commandJson = Resources.toString(Resources.getResource(INITIAL_COURT_PROCEEDINGS), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replace("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdActive)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdDeleted)
                .replace("RANDOM_REFERRAL_ID", referralReasonId)
                .replace("LISTED_START_DATE_TIME", listedStartDateTime)
                .replace("EARLIEST_START_DATE_TIME", earliestStartDateTime)
                .replace("DOB", defendantDOB);

        final JsonObject commandJsonInputJson = jsonFromString(commandJson);
        final DocumentContext prosecutionCase = parse(commandJsonInputJson);
        final JsonObject prosecutionCaseJO = prosecutionCase.read("$.initiateCourtProceedings.prosecutionCases[0]");
        final JsonObject prosecutionCaseEvent = Json.createObjectBuilder().add("prosecutionCase", prosecutionCaseJO).build();
        return parse(prosecutionCaseEvent);
    }
}


