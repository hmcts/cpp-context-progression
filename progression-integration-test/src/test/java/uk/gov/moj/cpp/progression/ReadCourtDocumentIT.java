package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getMaterialContent;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getMaterialContentResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupMaterialStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.stubMaterialContent;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubAdvocateRoleInCaseByCaseId;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupDefenceClientPermission;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupOrganisation;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReadCourtDocumentIT extends AbstractIT {

    private final String mimeType = "text/uri-list";
    private final String documentUrl = "http://documentlocation.com/myfile.pdf";
    private final JsonObject expectedResponse = createObjectBuilder().add("url", documentUrl).build();
    private String caseId;
    private UUID materialId;
    private String defendantId;
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();


    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        materialId = randomUUID();
        defendantId = randomUUID().toString();
        setupMaterialStub(materialId.toString());
        stubMaterialContent(materialId, documentUrl, mimeType);
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
    }

    @Test
    public void shouldGetMaterialMetadataAndContent() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialId.toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString());

        pollUsergroupsForMaterial();
        // and
        pollMaterialMetadata();

        final Response documentContentResponse = getMaterialContent(materialId, randomUUID());
        assertThat(stringToJsonObjectConverter.convert(documentContentResponse.readEntity(String.class)), equalTo(expectedResponse));
    }

    @Test
    public void shouldGetMaterialMetadataAndContentForDefence() throws Exception {
        final UUID organisationId = randomUUID();
        final UUID userId = randomUUID();

        final String organisation = getPayload("stub-data/usersgroups.get-organisation-details.json")
                .replace("%ORGANISATION_ID%", organisationId.toString());


        final String permission = getPayload("stub-data/usersgroups.get-permission-for-user-by-defendant.json")
                .replace("%USER_ID%", userId.toString())
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%ORGANISATION_ID%", organisationId.toString());

        stubUserGroupOrganisation(userId.toString(), organisation);
        stubUserGroupDefenceClientPermission(permission);

        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialId.toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString());

        pollUsergroupsForMaterial();
        // and
        pollMaterialMetadata();


        final Response documentContentResponse = getMaterialContent(materialId, userId, fromString(defendantId));
        assertThat(stringToJsonObjectConverter.convert(documentContentResponse.readEntity(String.class)), equalTo(expectedResponse));
    }

    @Test
    public void shouldNotGetMaterialMetadataAndContentForDefence() throws Exception {
        final UUID userOrganisationId = randomUUID();
        final UUID permittedOrganisationId = randomUUID();
        final UUID defendantIdPermission = randomUUID();
        final UUID userId = randomUUID();

        final String organisation = getPayload("stub-data/usersgroups.get-organisation-details.json")
                .replace("%ORGANISATION_ID%", userOrganisationId.toString());


        final String permission = getPayload("stub-data/usersgroups.get-permission-for-user-by-defendant.json")
                .replace("%USER_ID%", userId.toString())
                .replace("%DEFENDANT_ID%", defendantIdPermission.toString())
                .replace("%ORGANISATION_ID%", permittedOrganisationId.toString());

        stubUserGroupOrganisation(userId.toString(), organisation);
        stubUserGroupDefenceClientPermission(permission);

        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialId.toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString());

        pollUsergroupsForMaterial();
        // and
        pollMaterialMetadata();

        final Response documentContentResponse = getMaterialContent(materialId, userId, fromString(defendantId));
        assertThat(documentContentResponse.getStatus(), is(SC_FORBIDDEN));
    }

    @Test
    public void shouldGetMaterialMetadataAndContentForProsecution() throws Exception {

        // given
        final UUID userId = randomUUID();
        final String caseId = UUID.randomUUID().toString();

        final String userRoleInCase = getPayload("stub-data/defence.advocate.query.role-in-case-by-caseid.json")
                .replace("%CASE_ID%", caseId)
                .replace("%USER_ROLE_IN_CASE%", "prosecuting");

        stubAdvocateRoleInCaseByCaseId(caseId, userRoleInCase);

        //and
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialId.toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString());

        pollUsergroupsForMaterial();
        // and
        pollMaterialMetadata();

        String pathUri = "/material/" + materialId.toString() + "/content?caseId=" + caseId;

        //when
        final Response documentContentResponse = getMaterialContentResponse(pathUri, userId, "application/vnd.progression.query.material-content-for-prosecution+json");

        //then
        assertThat(stringToJsonObjectConverter.convert(documentContentResponse.readEntity(String.class)), equalTo(expectedResponse));
    }

    private void pollUsergroupsForMaterial() {
        pollForResponse("/search?q=" + materialId.toString(), "application/vnd.progression.query.usergroups-by-material-id+json",
                withJsonPath("$.allowedUserGroups[*]", hasItem("Listing Officers")));
    }

    private void pollMaterialMetadata() {
        pollForResponse("/material/" + materialId + "/metadata",
                "application/vnd.progression.query.material-metadata+json",
                withJsonPath("$.materialId", is(materialId.toString()))
        );
    }
}

