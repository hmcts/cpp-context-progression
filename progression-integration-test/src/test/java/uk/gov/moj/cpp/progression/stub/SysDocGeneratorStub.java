package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.awaitility.Awaitility.await;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.Matcher;
import org.json.JSONObject;

public class SysDocGeneratorStub {

    private static final String SYS_DOC_GENERATOR_URL = "/.*/rest/systemdocgenerator/generate-document";

    private static final String GENERATE_DOCUMENT_MEDIA_TYPE = "application/vnd.systemdocgenerator.generate-document+json";

    public static void stubAsyncDocumentGeneratorEndPoint() {
        stubFor(post(urlPathMatching(SYS_DOC_GENERATOR_URL))
                .withHeader(CONTENT_TYPE, equalTo(GENERATE_DOCUMENT_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, GENERATE_DOCUMENT_MEDIA_TYPE)
                ));
    }

    public static List<JSONObject> pollSysDocGenerationRequestsWithOriginatingSourceAndSourceCorrelationId(final Matcher<Collection<?>> matcher, final String originatingSource, final String sourceCorrelationId) {
        return await().until(() ->
        {
            List<JSONObject> list = new ArrayList<>();
            for (LoggedRequest loggedRequest : findAll(postRequestedFor(urlPathMatching(SYS_DOC_GENERATOR_URL)))) {
                String bodyAsString = loggedRequest.getBodyAsString();
                JSONObject j = new JSONObject(bodyAsString);
                if (j.getString("originatingSource").equals(originatingSource) && j.getString("sourceCorrelationId").contains(sourceCorrelationId)) {
                    list.add(j);
                }
            }
            return list;
        }, matcher);
    }

}
