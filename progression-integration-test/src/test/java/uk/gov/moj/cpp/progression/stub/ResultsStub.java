package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import org.apache.http.HttpHeaders;

/**
 * Stubs cpp-context-results queries.
 */
public class ResultsStub {

    private static final String HEARING_DETAILS_INTERNAL_URL = "/results-service/query/api/rest/results/hearingDetails/internal/";
    private static final String HEARING_DETAILS_INTERNAL_MEDIA_TYPE = "results.hearing-details-internal+json";
    private static final String SJP_DECISION_PAYLOAD = "stub-data/results.get-hearing-details-internal.sjp-decision.json";

    /**
     * Stubs the payload Results stored for an SJP decision ({@code public.events.hearing.hearing-resulted}): a fine on
     * the case's offence, costs for the defendant and a guilty verdict, decided and shared on the given dates.
     */
    public static void stubSjpDecisionResults(final String decisionId, final String caseId, final String defendantId,
                                              final String orderedDate, final String sharedTime, final String fineAmount) {
        final String body = getPayload(SJP_DECISION_PAYLOAD)
                .replace("DECISION_ID", decisionId)
                .replace("CASE_ID", caseId)
                .replace("DEFENDANT_ID", defendantId)
                .replace("ORDERED_DATE", orderedDate)
                .replace("SHARED_TIME", sharedTime)
                .replace("FINE_AMOUNT", fineAmount);

        stubFor(get(urlPathEqualTo(HEARING_DETAILS_INTERNAL_URL + decisionId))
                .withHeader(HttpHeaders.ACCEPT, containing(HEARING_DETAILS_INTERNAL_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    public static void verifySjpDecisionResultsNotQueried(final String decisionId) {
        verify(0, getRequestedFor(urlPathEqualTo(HEARING_DETAILS_INTERNAL_URL + decisionId)));
    }
}
