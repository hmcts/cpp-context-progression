package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCpsProsecutorData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.AbstractIT;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class ProsecutorUpdatedIT extends AbstractIT {
    private String caseId;
    private String defendantId;
    private String documentId;

    @AfterAll
    public static void tearDown() {
        cleanEventStoreTables();
    }

    @BeforeEach
    public void setup() {
        caseId = randomUUID().toString();
        documentId = randomUUID().toString();
        defendantId = randomUUID().toString();
        deleteAndCreateIndex();
        stubQueryCpsProsecutorData("/restResource/referencedata.query.cps.prosecutor.by.oucode.json", randomUUID(), HttpStatus.SC_OK);
    }

    @Test
    public void shouldIndexProsecutorUpdatedEvent() throws Exception {
        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        addCourtDocumentAndVerify("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");
        // when document is added, then the prosecutor is updated to CPS
        final Matcher[] initialMatchers = {withJsonPath("$.prosecutingAuthority", equalTo("CPS_NE"))};

        final Optional<JsonObject> initialElasticSearchCaseResponseJsonObject = findBy(initialMatchers);
        assertTrue(initialElasticSearchCaseResponseJsonObject.isPresent());
        final JsonObject outputCase = initialElasticSearchCaseResponseJsonObject.get();
        with(outputCase.toString())
                .assertThat("$.caseId", equalTo(caseId))
                .assertThat("$.prosecutingAuthority", equalTo("CPS_NE"))
                .assertThat("$.caseStatus", equalTo("ACTIVE"))
                .assertThat("$._case_type", equalTo("PROSECUTION"));
    }

    private void addCourtDocumentAndVerify(final String documentTypeId) throws IOException, JSONException {
        //Given
        final String body = prepareAddCourtDocumentPayload();
        //When
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + documentId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        //Then
        final String actualDocument = getCourtDocumentFor(documentId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(documentId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true)),
                withJsonPath("$.courtDocument.sendToCps", equalTo(true)))
        );

        final String expectedPayload = getPayload("expected/expected.progression.add-court-document.json")
                .replace("COURT-DOCUMENT-ID", documentId)
                .replace("DEFENDENT-ID", defendantId)
                .replace("DOCUMENT-TYPE-ID", documentTypeId)
                .replace("CASE-ID", caseId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("courtDocument.materials[0].uploadDateTime", (o1, o2) -> true)
        );
    }

    private String prepareAddCourtDocumentPayload() {
        return getPayload("progression.add-court-document-with-cpscase.json")
                .replaceAll("%RANDOM_DOCUMENT_ID%", documentId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId)
                .replaceAll("\"isCpsCase\": false", "\"isCpsCase\": true");
    }


}
