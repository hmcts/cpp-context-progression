package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getMaterialContent;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupMaterialStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.stubMaterialContent;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;

import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

public class ReadCourtDocumentIT extends AbstractIT {

    private final String mimeType = "application/pdf";
    private final String MaterialContent = "Material content for uploaded material";
    private String caseId;
    private UUID materialId;
    private String defendantId;
    private static final String QUERY_USERGROUPS_BY_MATERIAL_ID_JSON = "application/vnd.progression.query.usergroups-by-material-id+json";


    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        materialId = randomUUID();
        defendantId = randomUUID().toString();
        setupMaterialStub(materialId.toString());
        stubMaterialContent(materialId, MaterialContent.getBytes(), mimeType);
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
    }


    @Test
    public void shouldGetMaterialMetadataAndContent() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialId.toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString());

        pollForResponse("/search?q=" + materialId.toString(), QUERY_USERGROUPS_BY_MATERIAL_ID_JSON,
                withJsonPath("$.allowedUserGroups[0]", is("Listing Officers")));
        // and
        pollForResponse("/material/" + materialId + "/metadata",
                "application/vnd.progression.query.material-metadata+json",
                withJsonPath("$.materialId", is(materialId.toString()))
        );

        final Response documentContentResponse = getMaterialContent(materialId, randomUUID());
        assertThat(documentContentResponse.readEntity(String.class), equalTo(MaterialContent));
    }


}

