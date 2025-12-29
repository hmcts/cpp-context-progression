package uk.gov.moj.cpp.progression.ingester;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.anyOf;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCaseCivilFeesFor;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseDefendant;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.progression.AbstractIT;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InitiateCourtProceedingsIT extends AbstractIT {

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
    @BeforeEach
    public void setup() {
        caseId = UUID.randomUUID().toString();
        materialIdActive = UUID.randomUUID().toString();
        materialIdDeleted = UUID.randomUUID().toString();
        courtDocumentId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        referralReasonId = UUID.randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
        deleteAndCreateIndex();
    }

    @Test
    public void shouldInitiateCourtProceedingsWithCourtDocuments() throws IOException {

        final String caseUrn = generateUrn();
        //given
        initiateCourtProceedings(INITIAL_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, caseUrn, listedStartDateTime, earliestStartDateTime, defendantDOB);

        final String feeIds = "3034e172-99d3-4970-bc5e-fd95dd62c9d7"+','+ "3034e172-99d3-4970-bc5e-fd95dd62c9d6";

        List<Matcher<? super ReadContext>> matchers = getCivilFeeMatchers();
        pollProsecutionCaseCivilFeesFor(feeIds, matchers.toArray(new Matcher[7]));

        final DocumentContext inputProsecutionCase = documentContext(caseUrn);

        final Optional<JsonObject> prosecutionCaseResponseJsonObject = getPoller().pollUntilFound(() -> {
            try {
                final JsonObject jsonObject1 = elasticSearchIndexFinderUtil.findByCaseIds("crime_case_index", caseId);
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

    private static List<Matcher<? super ReadContext>> getCivilFeeMatchers() {
        List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.civilFees.size()", is(2)),
                withJsonPath("$.civilFees.[0].feeId", anyOf(is("3034e172-99d3-4970-bc5e-fd95dd62c9d6"), is("3034e172-99d3-4970-bc5e-fd95dd62c9d7"))),
                withJsonPath("$.civilFees.[0].feeType", anyOf(is("INITIAL"), is("CONTESTED"))),
                withJsonPath("$.civilFees.[0].feeStatus", is("OUTSTANDING")),
                withJsonPath("$.civilFees.[1].feeId", anyOf(is("3034e172-99d3-4970-bc5e-fd95dd62c9d6"), is("3034e172-99d3-4970-bc5e-fd95dd62c9d7"))),
                withJsonPath("$.civilFees.[1].feeType", anyOf(is("CONTESTED"), is("INITIAL"))),
                withJsonPath("$.civilFees.[1].feeStatus", is("OUTSTANDING"))
        );
        return matchers;
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
        final JsonObject prosecutionCaseEvent = JsonObjects.createObjectBuilder().add("prosecutionCase", prosecutionCaseJO).build();
        return parse(prosecutionCaseEvent);
    }
}


