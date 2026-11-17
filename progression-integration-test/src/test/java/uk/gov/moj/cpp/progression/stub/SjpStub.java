package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import java.util.List;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.apache.http.HttpHeaders;


public class SjpStub {

    private static final String SJP_CASES_URL = "/sjp-service/query/api/rest/sjp/cases/";
    private static final String SJP_QUERY_CASE_MEDIA_TYPE = "sjp.query.case+json";

    public static void setupSjpProsecutionCaseQueryStub(final String caseId, final String prosecutionAuthorityReference) {

        String body = getPayload("stub-data/sjp-query-api.get-prosecution-case-by-caseId.json")
                .replace("${CASE_ID}", caseId)
                .replace("${AUTH_REF}", prosecutionAuthorityReference);
        final String urlPath = SJP_CASES_URL;

                stubFor(get(urlPathMatching(urlPath+".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    /**
     * Stubs {@code sjp.query.case} for a case with the given decisions. Only the fields Case at a Glance reads are set.
     */
    public static void stubSjpCaseWithDecisions(final String caseId, final List<SjpDecision> decisions) {
        final JsonArrayBuilder caseDecisions = createArrayBuilder();
        decisions.forEach(decision -> {
            final JsonObjectBuilder caseDecision = createObjectBuilder()
                    .add("id", decision.id())
                    .add("savedAt", decision.savedAt());
            if (decision.applicationDecision()) {
                caseDecision.add("applicationDecision", createObjectBuilder()
                        .add("granted", true)
                        .add("outOfTime", false));
            }
            caseDecisions.add(caseDecision);
        });
        final String body = createObjectBuilder()
                .add("id", caseId)
                .add("caseDecisions", caseDecisions)
                .build().toString();

        stubFor(get(urlPathEqualTo(SJP_CASES_URL + caseId))
                .withHeader(HttpHeaders.ACCEPT, containing(SJP_QUERY_CASE_MEDIA_TYPE))
                .atPriority(1)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    public static void verifySjpCaseNotQueried(final String caseId) {
        verify(0, getRequestedFor(urlPathEqualTo(SJP_CASES_URL + caseId))
                .withHeader(HttpHeaders.ACCEPT, containing(SJP_QUERY_CASE_MEDIA_TYPE)));
    }

    /**
     * A decision as {@code sjp.query.case} returns it: {@code applicationDecision} is set when the decision is on an
     * application (for example a statutory declaration that sets the earlier decision aside).
     */
    public record SjpDecision(String id, String savedAt, boolean applicationDecision) {
    }
}
