package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PdfTestHelper.asPdf;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.awaitility.core.ConditionTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

public class DocumentGeneratorStub {

    private static final String PATH = "/systemdocgenerator-service/command/api/rest/systemdocgenerator/render";

    public static final String DOCUMENT_TEXT = STRING.next();

    public static void stubSynchronousDocumentGeneratorEndpoint() {
        stubDocumentCreate(DOCUMENT_TEXT);
    }

    public static void stubDocumentCreate(String documentText) {
        stubFor(post(urlPathMatching(PATH))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.systemdocgenerator.render+json"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(documentText.getBytes())));
    }

    public static Optional<JSONObject> getCrownCourtExtractDocumentRequestByDefendant(final String defendantId) {
        return getDocumentRequestsAsStream().stream()
                .map(t -> {
                    try {
                        return new JSONObject(t);
                    } catch (JSONException e) {
                        return null;
                    }
                })
                .filter(json -> {
                    try {
                        return json.getString("templateName").equals("CrownCourtExtract");
                    } catch (JSONException e) {
                        return false;
                    }
                })
                .map(json -> {
                    try {
                        return json.getJSONObject("templatePayload");
                    } catch (JSONException e) {
                        return json;
                    }
                })
                .filter(request -> {
                    try {
                        return request.getJSONObject("defendant").getString("id").equals(defendantId);
                    } catch (JSONException e) {
                        return false;
                    }
                })
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
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.systemdocgenerator.render+json")))
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .collect(Collectors.toList());
    }

    public static Optional<JsonObject> getHearingEventTemplate(final String templateName) {
        final List<String> documentRequests = getDocumentRequestsAsStream();
        return documentRequests.stream()
                .map(s -> Json.createReader(new StringReader(s)).readObject())
                .filter(json -> json.getString("templateName").equals(templateName))
                .map(json -> json.getJsonObject("templatePayload"))
                .findFirst();
    }

    public static Optional<JSONObject> pollDocumentGenerationRequest(final Predicate<JSONObject> requestPayloadPredicate) {
        try {
            return await().timeout(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> findAll(postRequestedFor(urlPathMatching(PATH)))
                    .stream()
                    .map(LoggedRequest::getBodyAsString)
                    .map(t -> {
                        try {
                            return new JSONObject(t);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(requestPayloadPredicate).findFirst(),
                    is(not(empty())));
        } catch (final ConditionTimeoutException timeoutException) {
            return Optional.empty();
        }
    }


    public static Optional<JSONObject> pollDocumentGenerationRequest(final String templateName) {
        return pollDocumentGenerationRequest(request -> {
            try {
                return request.getString("templateName").equals(templateName);
            }catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void stubDocumentGeneration(final String templateName) {
        stubFor(post(urlPathMatching(PATH))
                .withRequestBody(containing(String.format("\"%s\"", templateName)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(asPdf(templateName))
                        .withHeader(CONTENT_TYPE, "application/pdf")));
    }
}
