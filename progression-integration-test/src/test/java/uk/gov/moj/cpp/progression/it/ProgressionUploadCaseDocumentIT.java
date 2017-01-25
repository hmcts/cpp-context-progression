package uk.gov.moj.cpp.progression.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.jgroups.util.UUID;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.jayway.restassured.response.Response;

import uk.gov.moj.cpp.progression.helper.StubUtil;

public class ProgressionUploadCaseDocumentIT extends AbstractIT {



    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private String caseId;

    private File file;

    @Before
    public void createCaseIdAndMockEndpoints() throws IOException {
        
        caseId = UUID.randomUUID().toString();
        StubUtil.resetStubs();
        StubUtil.setupStructureCaseStub(caseId, UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), UUID.randomUUID().toString());
        StubUtil.setupUsersGroupQueryStub();
        final String fileContent = "{ \"message\" : \"I Joe Bloggs plead guilty\"}";
        file = folder.newFile("plea.json");
        FileUtils.writeStringToFile(file, fileContent);
        stubFor(post(urlEqualTo(baseUri + "/structure-command-api/command/api/rest/structure/cases/"
                        + caseId + "/case-documents")).willReturn(aResponse().withStatus(202)));
        stubFor(get(urlEqualTo(
                        baseUri + "/structure-query-controller/query/controller/rest/structure/search"))
                                        .willReturn(aResponse().withStatus(200)));
        stubFor(get(urlEqualTo(
                        baseUri + "/structure-query-controller/query/controller/rest/structure/cases/"
                                        + caseId)).willReturn(aResponse().withStatus(200)));
        stubFor(post(urlEqualTo(
                        baseUri + "/material-command-api/command/api/rest/material/material-reference"))
                                        .willReturn(aResponse().withStatus(202)));
    }

    @Ignore("Jenkins environment doesn't support Alfresco yet!!!") @Test
    public void shouldUploadDocument() throws Exception {

        final Response writeResponse =
                        postCommand(getCommandUri("/cases/" + caseId + "/casedocuments"),
                                        "multipart/form-data", file);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    private String getCommandUri(final String path) {
        return baseUri + prop.getProperty("base-uri-command") + path;
    }

    private Response postCommand(final String uri, final String mediaType, final File file)
                    throws IOException {
        return given().spec(reqSpec).and().contentType(mediaType).multiPart(file)
                        .header("CJSCPPUID", UUID.randomUUID().toString()).when().post(uri).then()
                        .extract().response();
    }
}
