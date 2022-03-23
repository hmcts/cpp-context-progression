package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.json.JSONObject;

public class DocumentGeneratorStub {

    public static final String PATH = "/system-documentgenerator-api/rest/documentgenerator/render";

    public static void stubDocumentCreate(String documentText) {
        stubFor(post(urlPathMatching(PATH))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.system.documentgenerator.render+json"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(documentText.getBytes())));
    }

    public static void verifyCreate(List<String> expectedValues) {

        RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(PATH));
        expectedValues.forEach(
                expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
        );
        verify(requestPatternBuilder);
    }


    public static Optional<JSONObject> getCrownCourtExtractDocumentRequestByDefendant(final String defendantId) {
        return getDocumentRequestsAsStream().stream()
                .map(JSONObject::new)
                .filter(json -> json.getString("templateName").equals("CrownCourtExtract"))
                .map(json -> json.getJSONObject("templatePayload"))
                .filter(request -> request.getJSONObject("defendant").getString("id").equals(defendantId))
                .findFirst();
    }

    public static Optional<JsonObject> getSummonsTemplate(final String templateName, final String... contains) {
        final List<String> documentRequests = getDocumentRequestsAsStream();
        return documentRequests.stream()
                .map(s -> Json.createReader(new StringReader(s)).readObject())
                .filter(request -> Arrays.stream(contains).allMatch(request.toString()::contains))
                .filter(json -> json.getString("templateName").equals(templateName))
                .map(json -> json.getJsonObject("templatePayload"))
                .findFirst();
    }

    private static List<String> getDocumentRequestsAsStream() {
        return findAll(postRequestedFor(urlPathMatching(PATH))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.system.documentgenerator.render+json")))
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .collect(Collectors.toList());
    }
}
