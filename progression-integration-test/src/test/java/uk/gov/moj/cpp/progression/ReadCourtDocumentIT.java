package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getMaterialContent;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getMaterialMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getQueryUri;

import uk.gov.moj.cpp.progression.helper.RestHelper;
import uk.gov.moj.cpp.progression.helper.StubUtil;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;

public class ReadCourtDocumentIT {

    private final String mimeType = "application/pdf";
    private final String MaterialContent = "Material content for uploaded material";
    private String caseId;
    private UUID materialId;
    private String defendantId;
    private static final String QUERY_USERGROUPS_BY_MATERIAL_ID_JSON = "application/vnd.progression.query.usergroups-by-material-id+json";


    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        materialId = UUID.randomUUID();
        defendantId = UUID.randomUUID().toString();
        createMockEndpoints();
        StubUtil.setupMaterialStub(materialId.toString());
        StubUtil.stubMaterialContent(materialId, MaterialContent.getBytes(), mimeType);
    }


    @Test
    public void shouldGetMaterialMetadataAndContent() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialId.toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());


        poll(requestParams(getQueryUri("/search?q=" + materialId.toString()), QUERY_USERGROUPS_BY_MATERIAL_ID_JSON).withHeader(USER_ID, UUID.randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(RestHelper.POLL_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.allowedUserGroups[0]",
                                        IsEqual.equalTo("defence"))

                        )));
        // and
        final JsonObject responseObject = getJsonObject(getMaterialMetadata(materialId.toString()));
        assertThat(responseObject.getString("materialId"), equalTo(materialId.toString()));
        // and
        final Response documentContentResponse = getMaterialContent(materialId, UUID.randomUUID());
        assertThat(documentContentResponse.readEntity(String.class), equalTo(MaterialContent));
    }


}

