package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyAliases;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.outputParty;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyDefendant;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;

import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;

import java.io.IOException;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class ProsecutionCaseCreatedIT extends AbstractIT {
    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";
    private String caseId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;

    @Before
    public void setup() throws IOException {
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        courtDocumentId = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        deleteAndCreateIndex();
    }

    @AfterClass
    public static void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    @Test
    public void shouldIndexProgressionCaseCreatedEvent() throws Exception {

        final String caseUrn = PreAndPostConditionHelper.generateUrn();
        addProsecutionCaseToCrownCourtForIngestion(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);

        final Optional<JsonObject> prosecussionCaseResponseJsonObject = getPoller().pollUntilFound(() -> {
            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1 && isPartiesPopulated(jsonObject)) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }

            return empty();
        });

        assertTrue(prosecussionCaseResponseJsonObject.isPresent());

        final JsonObject outputCase = jsonFromString(getJsonArray(prosecussionCaseResponseJsonObject.get(), "index").get().getString(0));
        final DocumentContext inputProsecutionCase = documentContext(caseUrn);
        verifyCaseCreated(1l, inputProsecutionCase, outputCase);
        final JsonObject inputDefendant = inputProsecutionCase.read("$.prosecutionCase.defendants[0]");
        verifyDefendant(inputDefendant, outputCase, true);
        verifyAliases(0, parse(inputDefendant), outputParty(inputDefendant.getJsonString("id"), outputCase).get());

    }

    private boolean isPartiesPopulated(final JsonObject jsonObject) {
        return jsonFromString(getJsonArray(jsonObject, "index").get().getString(0))
                .containsKey("parties");
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
