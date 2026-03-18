
package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.*;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class CaseMaterialIT extends AbstractIT {

    private static final String USER_ID = "07e9cd55-0eff-4eb3-961f-0d83e259e415";
    public static final String USER_GROUP_NOT_PRESENT_DROOL = randomUUID().toString();
    public static final String USER_GROUP_NOT_PRESENT_RBAC = randomUUID().toString();
    public static final String CHAMBER_USER_ID = randomUUID().toString();
    private String caseId;
    private String docId;
    private String defendantId;
    private static final String MATERIAL_CASES_ENDPOINT = "/materials/cases";
    private static final String MEDIA_TYPE_MATERIAL_BULK = "application/vnd.progression.query.material-bulk+json";

    @BeforeAll
    public static void init() {
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_DROOL), "stub-data/usersgroups.get-invalid-groups-by-user.json");
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_RBAC), "stub-data/usersgroups.get-invalid-rbac-groups-by-user.json");
        setupAsAuthorisedUser(fromString(CHAMBER_USER_ID), "stub-data/usersgroups.get-chamber-groups-by-user.json");
        setupAsAuthorisedUser(fromString(USER_ID), "stub-data/usersgroups.get-specific-groups-by-user.json");
    }

    @BeforeEach
    public void setup() {
        caseId = randomUUID().toString();
        docId = randomUUID().toString();
        defendantId = randomUUID().toString();
    }

    @Test
    public void shouldReturnMaterialMappingsForMultipleMaterialIds() throws IOException, JSONException {
        //Given
        String materialId1 = randomUUID().toString();
        String materialId2 = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", CoreMatchers.is(caseId)))));

        String courtDocumentBody = prepareCourtDocumentWithMaterials(docId, materialId1, materialId2);

        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                courtDocumentBody);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String courtDocResponse = pollForResponse(format("/courtdocuments/%s", docId),
                "application/vnd.progression.query.courtdocument+json");

        JSONObject courtDocObject = new JSONObject(courtDocResponse);
        assertThat(courtDocObject.getJSONObject("courtDocument").has("materials"), Matchers.is(true));

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");

        String materialIds = materialId1 + "," + materialId2;
        final String url = MATERIAL_CASES_ENDPOINT + format("?materialIds=%s", materialIds);

        //When
        final String response = pollForResponse(url, MEDIA_TYPE_MATERIAL_BULK);

        //Then
        assertThat(response, notNullValue());

        JSONObject responseObject = new JSONObject(response);
        assertThat(responseObject.has("materialIds"), Matchers.is(true));

        JSONArray materialIdsArray = responseObject.getJSONArray("materialIds");
        assertThat(materialIdsArray.length(), Matchers.greaterThanOrEqualTo(1));

        for (int i = 0; i < materialIdsArray.length(); i++) {
            JSONObject material = materialIdsArray.getJSONObject(i);
            assertThat(material.getString("materialId"), Matchers.notNullValue());
            assertThat(material.has("courtDocumentId"), Matchers.is(true));
            assertThat(material.has("caseId"), Matchers.is(true));
            assertThat(material.has("caseUrn"), Matchers.is(true));
        }
    }

    private String prepareCourtDocumentWithMaterials(final String docId, final String... materialIds) throws JSONException {
        String body = getPayload("progression.add-court-document.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId);

        // Add materials array to the payload if not already present
        JSONObject jsonBody = new JSONObject(body);
        JSONObject courtDocument = jsonBody.getJSONObject("courtDocument");

        JSONArray materialsArray = new JSONArray();
        for (String materialId : materialIds) {
            JSONObject material = new JSONObject();
            material.put("id", materialId);
            materialsArray.put(material);
        }
        courtDocument.put("materials", materialsArray);

        return jsonBody.toString();
    }

}