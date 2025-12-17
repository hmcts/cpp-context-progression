package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import org.apache.http.HttpHeaders;


public class SjpStub {

    public static void setupSjpProsecutionCaseQueryStub(final String caseId, final String prosecutionAuthorityReference) {

        String body = getPayload("stub-data/sjp-query-api.get-prosecution-case-by-caseId.json")
                .replace("${CASE_ID}", caseId)
                .replace("${AUTH_REF}", prosecutionAuthorityReference);
        final String urlPath = "/sjp-service/query/api/rest/sjp/cases/";

                stubFor(get(urlPathMatching(urlPath+".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }
}
