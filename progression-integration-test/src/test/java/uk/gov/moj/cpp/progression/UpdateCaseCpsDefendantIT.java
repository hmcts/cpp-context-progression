package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCpsProsecutorData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import java.io.IOException;
import java.util.Arrays;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class UpdateCaseCpsDefendantIT extends AbstractIT {

    private static final String docId = randomUUID().toString();
    private static final String caseId = randomUUID().toString();
    private static final String defendantId = randomUUID().toString();
    private static final String cpsDefendantId = randomUUID().toString();

    @Test
    public void shouldUpdateProsecutionCaseCpsDefendantId() throws Exception {
        stubQueryCpsProsecutorData("/restResource/referencedata.query.prosecutor.by.oucode.json", randomUUID(), HttpStatus.SC_OK);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")))));

        verifyAddCourtDocumentForCase();
    }

    private void verifyAddCourtDocumentForCase() throws IOException {
        final String body = prepareAddCourtDocumentPayload();
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document-v2+json", body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                Arrays.asList(allOf(withJsonPath("$.prosecutionCase.defendants[0].cpsDefendantId", is(cpsDefendantId)),
                        withJsonPath("$.prosecutionCase.prosecutor.prosecutorCode", is("TFL"))))));
    }

    private String prepareAddCourtDocumentPayload() {
        String body = getPayload("progression.add-court-document-for-case-v2.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId)
                .replaceAll("%RANDOM_CPS_DEFENDANT_ID%", cpsDefendantId);

        return body;
    }

}