package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getMaterialContent;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupMaterialStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.stubMaterialContent;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupDefenceClientPermission;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupOrganisation;

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

    @Test
    public void shouldGetMaterialMetadataAndContentForDefence() throws Exception {
        final UUID organisationId = randomUUID();

        final String organisation = getPayload("stub-data/usersgroups.get-organisation-details.json")
                .replace("%ORGANISATION_ID%", organisationId.toString());


        final String permission = getPayload("stub-data/usersgroups.get-permission-for-user-by-defendant.json")
                .replace("%USER_ID%", USER_ID_VALUE.toString())
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%ORGANISATION_ID%", organisationId.toString());

        stubUserGroupOrganisation(USER_ID_VALUE.toString(), organisation);
        stubUserGroupDefenceClientPermission(defendantId, permission);

        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialId.toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString());

        pollForResponse("/search?q=" + materialId.toString(), QUERY_USERGROUPS_BY_MATERIAL_ID_JSON,
                withJsonPath("$.allowedUserGroups[0]", is("Listing Officers")));
        // and
        pollForResponse("/material/" + materialId + "/metadata",
                "application/vnd.progression.query.material-metadata+json",
                withJsonPath("$.materialId", is(materialId.toString()))
        );


        final Response documentContentResponse = getMaterialContent(materialId, USER_ID_VALUE,fromString(defendantId));
        assertThat(documentContentResponse.readEntity(String.class), equalTo(MaterialContent));
    }


    @Test
    public void shouldNotGetMaterialMetadataAndContentForDefence() throws Exception {
        final UUID userOrganisationId = randomUUID();
        final UUID permittedOrganisationId = randomUUID();
        final UUID  defendantIdPermission = randomUUID();

        final String organisation = getPayload("stub-data/usersgroups.get-organisation-details.json")
                .replace("%ORGANISATION_ID%", userOrganisationId.toString());


        final String permission = getPayload("stub-data/usersgroups.get-permission-for-user-by-defendant.json")
                .replace("%USER_ID%", USER_ID_VALUE.toString())
                .replace("%DEFENDANT_ID%", defendantIdPermission.toString())
                .replace("%ORGANISATION_ID%", permittedOrganisationId.toString());

        stubUserGroupOrganisation(USER_ID_VALUE.toString(), organisation);
        stubUserGroupDefenceClientPermission(defendantId, permission);

        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialId.toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString());

        pollForResponse("/search?q=" + materialId.toString(), QUERY_USERGROUPS_BY_MATERIAL_ID_JSON,
                withJsonPath("$.allowedUserGroups[0]", is("Listing Officers")));
        // and
        pollForResponse("/material/" + materialId + "/metadata",
                "application/vnd.progression.query.material-metadata+json",
                withJsonPath("$.materialId", is(materialId.toString()))
        );


        final Response documentContentResponse = getMaterialContent(materialId, USER_ID_VALUE,fromString(defendantId));
        assertThat(documentContentResponse.getStatus(), is(SC_FORBIDDEN));
    }


}

