package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class AddCourtDocumentIT extends AbstractIT {

    private String caseId;
    private String docId;
    private String defendantId;

    @Before
    public void setup() {
        super.setUp();
        caseId = UUID.randomUUID().toString();
        docId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        createMockEndpoints();
    }

    @Test
    public void shouldAddCourtDocument() throws IOException, InterruptedException {
        //Given
        String body = prepareAddCourtDocumentPayload();
        //When
        final Response writeResponse = postCommand(getCommandUri("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        //Then
        getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true))
        ));
    }

    private String prepareAddCourtDocumentPayload() throws IOException {
        String body = Resources.toString(Resources.getResource("progression.add-court-document.json"),
                Charset.defaultCharset());
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId.toString())
                .replaceAll("%RANDOM_CASE_ID%", caseId.toString())
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId.toString());
        return body;
    }

}
