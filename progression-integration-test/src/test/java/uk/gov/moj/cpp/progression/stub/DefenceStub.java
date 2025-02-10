package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayloadAsJsonObject;

import java.text.MessageFormat;

import javax.json.JsonObject;

import org.apache.http.HttpHeaders;


public class DefenceStub {

    public static void stubForAssociatedOrganisation(final JsonObject payload, final String defendantId) {
        String body = payload.toString();

        stubFor(get(urlPathEqualTo(MessageFormat.format("/defence-service/query/api/rest/defence/defendants/{0}/associatedOrganisation", defendantId)))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    public static void stubForAssociatedOrganisation(final String resourceName, final String defendantId) {
        stubForAssociatedOrganisation(getPayloadAsJsonObject(resourceName), defendantId);
    }

    public static void stubForAssociatedCaseDefendantsOrganisation(final String resourceName, final String caseId) {
        final String body = getPayload(resourceName);

        stubFor(get(urlPathEqualTo(MessageFormat.format("/defence-service/query/api/rest/defence/cases/{0}", caseId)))
                .withQueryParam("withAddress", equalTo("true"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    public static void stubForCaseDefendantsOrganisation(final String resourceName, final String caseId, final String defendantId1,
                                                         final String defendantId2, final String defendant1FirstName,
                                                         final String defendant1LastName, final String defendant2FirstName, final String defendant2LastName) {

        String body = getPayload(resourceName)
                .replace("${defendantId1}", defendantId1)
                .replace("${defendantId2}", defendantId2)
                .replace("${defendant1FirstName}", defendant1FirstName)
                .replace("${defendant1LastName}", defendant1LastName)
                .replace("${defendant2FirstName}", defendant2FirstName)
                .replace("${defendant2LastName}", defendant2LastName)
                .replace("${caseId}", caseId);
        stubFor(get(urlPathEqualTo(MessageFormat.format("/defence-service/query/api/rest/defence/cases/{0}", caseId)))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));

    }

    public static void stubForAssociatedDefendantsForDefenceOrganisation(final String resourceName, final String userId) {
        String body = getPayload(resourceName);

        stubFor(get(urlPathEqualTo(MessageFormat.format("/defence-service/query/api/rest/defence/defenceusers/{0}", userId)))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    public static void stubForDefendantIdpcMetadata(final String resourceName, final String defendantId) {
        String body = getPayload(resourceName);

        stubFor(get(urlPathEqualTo(MessageFormat.format("/defence-service/query/api/rest/defence/defendants/{0}/idpc/metadata", defendantId)))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }
}
