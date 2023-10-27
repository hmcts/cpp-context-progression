package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import org.apache.http.HttpHeaders;


public class CorrespondenceStub {

    public static void stubForCorrespondenceCaseContacts(final String resourceName, final String caseId, final String defendantId1, final String defendantId2) {
        stubPingFor("correspondence-service");
        String body = getPayload(resourceName)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID_1", defendantId1)
                .replaceAll("DEFENDANT_ID_2", defendantId2);

        stubFor(get(urlPathEqualTo("/correspondence-service/query/api/rest/correspondence/contacts"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));

        waitForStubToBeReady("/correspondence-service/query/api/rest/correspondence/contacts", "application/vnd.correspondence.query.contacts+json");
    }
}
