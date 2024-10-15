package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.createReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutionCaseDefendantUpdatedEvent;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyDefendantAliases;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyDefendantUpdate;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.io.IOException;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProsecutionCaseDefendantUpdatedIngesterIT extends AbstractIT {
    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";
    private static final String UPDATE_PROSECUTION_DEFENDANT_COMMAND_RESOURCE_LOCATION = "ingestion/progression.update-defendant-for-prosecution-case.json";
    private String caseId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private ProsecutionCaseUpdateDefendantHelper helper;
    private String caseUrn;

    @BeforeEach
    public void setup() {
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        courtDocumentId = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
        caseUrn = PreAndPostConditionHelper.generateUrn();
        deleteAndCreateIndex();
    }

    @AfterAll
    public static void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    @Test
    public void shouldUpdateDefendant() throws IOException, JSONException {

        final String commandJson = createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                courtDocumentId, randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);

        setUpCaseAndDefendants();
        final String inputDefendant = getPayload(UPDATE_PROSECUTION_DEFENDANT_COMMAND_RESOURCE_LOCATION);
        helper.updateDefendant(inputDefendant);

        final Optional<JsonObject> defendantUpdatedResponseJsonObject = pollAfterDefendantUpdated();

        assertTrue(defendantUpdatedResponseJsonObject.isPresent());

        final String indexedContent = getJsonArray(defendantUpdatedResponseJsonObject.get(), "index").get().getString(0);
        final JsonObject outputIndexedJson = jsonFromString(indexedContent);

        final String defendantUpdatedCommand = getProsecutionCaseDefendantUpdatedEvent(caseId, defendantId, caseUrn, UPDATE_PROSECUTION_DEFENDANT_COMMAND_RESOURCE_LOCATION);
        final JsonObject defendantUpdated = jsonFromString(defendantUpdatedCommand);

        final DocumentContext inputCaseDocument = documentContextForProsecutionCase();
        verifyCaseCreated(1l, inputCaseDocument, outputIndexedJson);

        final JsonObject party = outputIndexedJson.getJsonArray("parties").getJsonObject(0);
        verifyDefendantUpdate(parse(defendantUpdated.getJsonObject("defendant")), party);
        final JsonObject defendant = inputCaseDocument.read("$.prosecutionCase.defendants[0]");
        final DocumentContext parsedInputDefendant = parse(defendant);
        verifyDefendantAliases(parsedInputDefendant, party);

    }

    private Optional<JsonObject> pollAfterDefendantUpdated() {
        return getPoller().pollUntilFound(() -> {
            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                final JsonObject outputCase = jsonFromString(getJsonArray(jsonObject, "index").get().getString(0));

                if (jsonObject.getInt("totalResults") == 1
                        && defendantPopulated(outputCase)
                        && defendantUpdated(outputCase)) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }
            return empty();
        });
    }

    private void setUpCaseAndDefendants() throws IOException {
        addProsecutionCaseToCrownCourtForIngestion(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId,
                referralReasonId, caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);

        final Optional<JsonObject> prosecussionCaseResponseJsonObject = getPoller().pollUntilFound(() -> {

            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1) {
                    final JsonObject outputCase = jsonFromString(getJsonArray(jsonObject, "index").get().getString(0));
                    if (defendantPopulated(outputCase)) {
                        return of(jsonObject);
                    }
                }
            } catch (final IOException e) {
                fail();
            }

            return empty();
        });

        assertTrue(prosecussionCaseResponseJsonObject.isPresent());

        final JsonObject outputCase = jsonFromString(getJsonArray(prosecussionCaseResponseJsonObject.get(), "index").get().getString(0));
        final DocumentContext prosecutionCase = documentContextForProsecutionCase();
        verifyCaseCreated(1l, prosecutionCase, outputCase);
    }

    private DocumentContext documentContextForProsecutionCase() throws IOException {

        final String commandJson = createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                courtDocumentId, randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);
        final JsonObject commandJsonInputJson = jsonFromString(commandJson);
        final DocumentContext prosecutionCase = parse(commandJsonInputJson);
        final JsonObject prosecutionCaseJO = prosecutionCase.read("$.courtReferral.prosecutionCases[0]");
        final JsonObject prosecutionCaseEvent = Json.createObjectBuilder().add("prosecutionCase", prosecutionCaseJO).build();
        return parse(prosecutionCaseEvent);
    }

    private boolean defendantPopulated(final JsonObject outputCase) {
        return outputCase.containsKey("parties");
    }

    private boolean defendantUpdated(final JsonObject outputCase) {
        return outputCase
                .getJsonArray("parties").getJsonObject(0)
                .getString("firstName")
                .equals("UpdatedHarry");
    }
}
