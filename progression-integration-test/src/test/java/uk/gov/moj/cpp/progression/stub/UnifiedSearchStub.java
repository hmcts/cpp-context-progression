package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.listAllStubMappings;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;


public class UnifiedSearchStub {

    private static final String SEARCH_QUERY = "/unifiedsearchquery-service/query/api/rest/unifiedsearchquery/defendant-cases";
    private static final String SEARCH_QUERY_TYPE = "application/vnd.unifiedsearch.query.defendant.cases+json";
    private static final String SERVICE_NAME = "unifiedsearchquery-service";

    public static void removeStub() {
        listAllStubMappings()
                .getMappings()
                .removeIf(m -> m.getRequest().getUrlPath() != null && m.getRequest().getUrlPath().equals(SEARCH_QUERY));
    }

    public static void stubUnifiedSearchQueryExactMatchWithEmptyResults() {
        InternalEndpointMockUtils.stubPingFor(SERVICE_NAME);

        stubFor(get(urlPathMatching(SEARCH_QUERY))
                .inScenario("EXACT_IS_EMPTY_PARTIAL_HAS_RECORD")
                .whenScenarioStateIs(STARTED)
                .withQueryParam("proceedingsConcluded", matching("false"))
                .withQueryParam("crownOrMagistrates", matching("true"))
                .withQueryParam("pageSize", matching(".*"))
                .withQueryParam("startFrom", matching(".*"))
                .withQueryParam("pncId", matching(".*"))
                .withQueryParam("lastName", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getUnifiedSearchEmptyResult())
                )
        .willSetStateTo("PARTIAL"));

        waitForStubToBeReady(SEARCH_QUERY +
                "?proceedingsConcluded=false&crownOrMagistrates=true&pageSize=25&startFrom=0&pncId=12345&lastName=TIM", SEARCH_QUERY_TYPE);
    }

    public static void stubUnifiedSearchQueryExactMatchWithResults(final String caseId_1, final String caseId_2, final String defendantId_1, final String defendantId_2,
                                                                   final String pncId, final String croNumber) {
        InternalEndpointMockUtils.stubPingFor(SERVICE_NAME);

        stubFor(get(urlPathMatching(SEARCH_QUERY))
                .inScenario("EXACT_IS_NOT_EMPTY")
                .withQueryParam("proceedingsConcluded", matching("false"))
                .withQueryParam("crownOrMagistrates", matching("true"))
                .withQueryParam("pageSize", matching(".*"))
                .withQueryParam("startFrom", matching(".*"))
                .withQueryParam("pncId", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getUnifiedSearchResult(caseId_1, caseId_2, defendantId_1, defendantId_2, pncId, croNumber))
                ));

        waitForStubToBeReady(SEARCH_QUERY +
                "?proceedingsConcluded=false&crownOrMagistrates=true&pageSize=25&startFrom=0&pncId=TIM", SEARCH_QUERY_TYPE);
    }

    public static void stubUnifiedSearchQueryPartialMatch(final String caseId_1, final String caseId_2, final String defendantId_1, final String defendantId_2,
                                                          final String pncId, final String croNumber) {
        InternalEndpointMockUtils.stubPingFor(SERVICE_NAME);

        stubFor(get(urlPathMatching(SEARCH_QUERY))
                .inScenario("EXACT_IS_EMPTY_PARTIAL_HAS_RECORD")
                .whenScenarioStateIs("PARTIAL")
                .withQueryParam("proceedingsConcluded", matching(".*"))
                .withQueryParam("pageSize", matching(".*"))
                .withQueryParam("startFrom", matching(".*"))
                .withQueryParam("pncId", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getUnifiedSearchResult(caseId_1, caseId_2, defendantId_1, defendantId_2, pncId, croNumber))
                ).willSetStateTo(STARTED));

        waitForStubToBeReady(SEARCH_QUERY +
                        "?proceedingsConcluded=false&crownOrMagistrates=true&pageSize=25&startFrom=0&pncId=1234",
                SEARCH_QUERY_TYPE);
    }

    public static void stubUnifiedSearchQueryPartialMatchWithEmptyResults() {
        InternalEndpointMockUtils.stubPingFor(SERVICE_NAME);

        stubFor(get(urlPathMatching(SEARCH_QUERY))
                .inScenario("EXACT_IS_EMPTY_PARTIAL_IS_EMPTY")
                .whenScenarioStateIs("PARTIAL")
                .withQueryParam("proceedingsConcluded", matching(".*"))
                .withQueryParam("pageSize", matching(".*"))
                .withQueryParam("startFrom", matching(".*"))
                .withQueryParam("pncId", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getUnifiedSearchEmptyResult())
                ).willSetStateTo(STARTED));

        waitForStubToBeReady(SEARCH_QUERY +
                        "?proceedingsConcluded=false&crownOrMagistrates=true&pageSize=25&startFrom=0&pncId=1234",
                SEARCH_QUERY_TYPE);
    }
    private static String getUnifiedSearchEmptyResult() {
        return "{\n" +
                "  \"totalResults\": 0,\n" +
                "  \"cases\": []\n" +
                "  }\n";
    }

    private static String getUnifiedSearchResult(final String caseId_1, final String caseId_2, final String defendantId_1, final String defendantId_2,
                                                 final  String pncId, final String croNumber) {
        return "{\n" +
                "  \"totalResults\": 2,\n" +
                "  \"cases\": [\n" +
                "    {\n" +
                "      \"caseReference\": \"20GB12345666\",\n" +
                "      \"prosecutionCaseId\": \"" + caseId_1 + "\",\n" +
                "      \"defendants\": [\n" +
                "        {\n" +
                "          \"courtProceedingsInitiated\": \"2020-03-02T10:00:00.000Z\",\n" +
                "          \"defendantId\": \"" + defendantId_1 + "\",\n" +
                "          \"masterDefendantId\": \"0a5372c5-b60f-4d95-8390-8c6462e2d7af\",\n" +
                "          \"firstName\": \"Teagan\",\n" +
                "          \"middleName\": \"\",\n" +
                "          \"lastName\": \"SMITT\",\n" +
                "          \"dateOfBirth\": \"2019-04-21\",\n" +
                "          \"pncId\": \""+pncId+"\",\n" +
                "          \"croNumber\": \""+croNumber+"\",\n" +
                "          \"address\": {\n" +
                "            \"address1\": \"15, somewhere street\",\n" +
                "            \"address2\": \"15th Lane\",\n" +
                "            \"address5\": \"London\",\n" +
                "            \"postcode\": \"HA1 1QF\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"caseReference\": \"45GB12345777\",\n" +
                "      \"prosecutionCaseId\": \"" + caseId_2 + "\",\n" +
                "      \"defendants\": [\n" +
                "        {\n" +
                "          \"defendantId\": \"" + defendantId_2 + "\",\n" +
                "          \"masterDefendantId\": \"0a5372c5-b60f-4d95-8390-8c6462e2d7af\",\n" +
                "          \"courtProceedingsInitiated\": \"2020-03-03T10:00:00.000Z\",\n" +
                "          \"firstName\": \"Teagan\",\n" +
                "          \"middleName\": \"\",\n" +
                "          \"lastName\": \"SITH\",\n" +
                "          \"dateOfBirth\": \"\",\n" +
                "          \"pncId\": \"2098/1234568L\",\n" +
                "          \"croNumber\": \"123456/20L\",\n" +
                "          \"address\": {\n" +
                "            \"address1\": \"19, ABC street\",\n" +
                "            \"address2\": \"19th Lane\",\n" +
                "            \"address3\": \"\",\n" +
                "            \"address4\": \"\",\n" +
                "            \"address5\": \"London\",\n" +
                "            \"postcode\": \"HA1 1QF\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    public static void stubUnifiedSearchQueryExactMatchForSPISpec(final String caseId_1, final String defendantId_1) {
        InternalEndpointMockUtils.stubPingFor(SERVICE_NAME);

        stubFor(get(urlPathMatching(SEARCH_QUERY))
                .inScenario("EXACT_MATCH")
                .withQueryParam("proceedingsConcluded", matching("false"))
                .withQueryParam("crownOrMagistrates", matching("true"))
                .withQueryParam("pageSize", matching(".*"))
                .withQueryParam("startFrom", matching(".*"))
                .withQueryParam("pncId", matching("20160000233W"))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getUnifiedSearchResultForSPISpec(caseId_1, defendantId_1))
                ));

        waitForStubToBeReady(SEARCH_QUERY +
                "?proceedingsConcluded=false&crownOrMagistrates=true&pageSize=2&startFrom=0&pncId=20160000233W&lastName=Louis", SEARCH_QUERY_TYPE);
    }

    private static String getUnifiedSearchResultForSPISpec(final String caseId_1, final String defendantId_1) {
        return "{\n" +
                "  \"totalResults\": 1,\n" +
                "  \"cases\": [\n" +
                "    {\n" +
                "      \"caseReference\": \"45GB12345777\",\n" +
                "      \"prosecutionCaseId\": \"" + caseId_1 + "\",\n" +
                "      \"defendants\": [\n" +
                "        {\n" +
                "          \"defendantId\": \"" + defendantId_1 + "\",\n" +
                "          \"masterDefendantId\": \"0a5372c5-b60f-4d95-8390-8c6462e2d7af\",\n" +
                "          \"courtProceedingsInitiated\": \"2020-03-03T10:00:00.000Z\",\n" +
                "          \"firstName\": \"John\",\n" +
                "          \"middleName\": \"\",\n" +
                "          \"lastName\": \"Louis\",\n" +
                "          \"dateOfBirth\": \"\",\n" +
                "          \"pncId\": \"20160000233W\",\n" +
                "          \"croNumber\": \"123456/10L\",\n" +
                "          \"address\": {\n" +
                "            \"address1\": \"19, ABC street\",\n" +
                "            \"address2\": \"19th Lane\",\n" +
                "            \"address3\": \"\",\n" +
                "            \"address4\": \"\",\n" +
                "            \"address5\": \"London\",\n" +
                "            \"postcode\": \"HA1 1QF\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    public static void stubUnifiedSearchQueryExactMatchForCJSSpec(final String caseId_1, final String defendantId_1) {
        InternalEndpointMockUtils.stubPingFor(SERVICE_NAME);

        stubFor(get(urlPathMatching(SEARCH_QUERY))
                .inScenario("EXACT_MATCH")
                .withQueryParam("proceedingsConcluded", matching("false"))
                .withQueryParam("crownOrMagistrates", matching("true"))
                .withQueryParam("pageSize", matching(".*"))
                .withQueryParam("startFrom", matching(".*"))
                .withQueryParam("pncId", matching("2016%2F0000233W"))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getUnifiedSearchResultForCJSSpec(caseId_1,defendantId_1))
                ));

        waitForStubToBeReady(SEARCH_QUERY +
                "?proceedingsConcluded=false&crownOrMagistrates=true&pageSize=2&startFrom=0&pncId=2016%2F0000233W&lastName=Louis", SEARCH_QUERY_TYPE);
    }

    private static String getUnifiedSearchResultForCJSSpec(final String caseId_1, final String defendantId_1) {
        return "{\n" +
                "  \"totalResults\": 1,\n" +
                "  \"cases\": [\n" +
                "    {\n" +
                "      \"caseReference\": \"45GB12345777\",\n" +
                "      \"prosecutionCaseId\": \"" + caseId_1 + "\",\n" +
                "      \"defendants\": [\n" +
                "        {\n" +
                "          \"defendantId\": \"" + defendantId_1 + "\",\n" +
                "          \"masterDefendantId\": \"0a5372c5-b60f-4d95-8390-8c6462e2d7af\",\n" +
                "          \"courtProceedingsInitiated\": \"2020-03-03T10:00:00.000Z\",\n" +
                "          \"firstName\": \"Amy\",\n" +
                "          \"middleName\": \"\",\n" +
                "          \"lastName\": \"Louis\",\n" +
                "          \"dateOfBirth\": \"\",\n" +
                "          \"pncId\": \"2016/0000233W\",\n" +
                "          \"croNumber\": \"123456/20L\",\n" +
                "          \"address\": {\n" +
                "            \"address1\": \"19, ABC street\",\n" +
                "            \"address2\": \"19th Lane\",\n" +
                "            \"address3\": \"\",\n" +
                "            \"address4\": \"\",\n" +
                "            \"address5\": \"London\",\n" +
                "            \"postcode\": \"HA1 1QF\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    public static void stubUnifiedSearchQueryPartialMatchForSPISpec(final String caseId_1, final String defendantId_1) {
        InternalEndpointMockUtils.stubPingFor(SERVICE_NAME);

        stubFor(get(urlPathMatching(SEARCH_QUERY))
                .inScenario("PARTIAL_MATCH")
                .withQueryParam("proceedingsConcluded", matching("false"))
                .withQueryParam("crownOrMagistrates", matching("true"))
                .withQueryParam("pageSize", matching(".*"))
                .withQueryParam("startFrom", matching(".*"))
                .withQueryParam("pncId", matching("20160000234W"))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getUnifiedSearchPartialResultForSPISpec(caseId_1, defendantId_1))
                ));

        waitForStubToBeReady(SEARCH_QUERY +
                "?proceedingsConcluded=false&crownOrMagistrates=true&pageSize=2&startFrom=0&pncId=20160000234W", SEARCH_QUERY_TYPE);
    }

    private static String getUnifiedSearchPartialResultForSPISpec(final String caseId_1, final String defendantId_1) {
        return "{\n" +
                "  \"totalResults\": 1,\n" +
                "  \"cases\": [\n" +
                "    {\n" +
                "      \"caseReference\": \"45GB12345777\",\n" +
                "      \"prosecutionCaseId\": \"" + caseId_1 + "\",\n" +
                "      \"defendants\": [\n" +
                "        {\n" +
                "          \"defendantId\": \"" + defendantId_1 + "\",\n" +
                "          \"masterDefendantId\": \"0a5372c5-b60f-4d95-8390-8c6462e2d7af\",\n" +
                "          \"courtProceedingsInitiated\": \"2020-03-03T10:00:00.000Z\",\n" +
                "          \"firstName\": \"Watson\",\n" +
                "          \"middleName\": \"\",\n" +
                "          \"lastName\": \"Test\",\n" +
                "          \"dateOfBirth\": \"\",\n" +
                "          \"pncId\": \"20160000234W\",\n" +
                "          \"croNumber\": \"123456/10L\",\n" +
                "          \"address\": {\n" +
                "            \"address1\": \"19, ABC street\",\n" +
                "            \"address2\": \"19th Lane\",\n" +
                "            \"address3\": \"\",\n" +
                "            \"address4\": \"\",\n" +
                "            \"address5\": \"London\",\n" +
                "            \"postcode\": \"HA1 1QF\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    public static void stubUnifiedSearchQueryPartialMatchForCJSSpec(final String caseId_1, final String defendantId_1) {
        InternalEndpointMockUtils.stubPingFor(SERVICE_NAME);

        stubFor(get(urlPathMatching(SEARCH_QUERY))
                .inScenario("PARTIAL_MATCH")
                .withQueryParam("proceedingsConcluded", matching("false"))
                .withQueryParam("crownOrMagistrates", matching("true"))
                .withQueryParam("pageSize", matching(".*"))
                .withQueryParam("startFrom", matching(".*"))
                .withQueryParam("pncId", matching("2016%2F0000234W"))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getUnifiedSearchPartialResultForCJSSpec(caseId_1, defendantId_1))
                ));

        waitForStubToBeReady(SEARCH_QUERY +
                "?proceedingsConcluded=false&crownOrMagistrates=true&pageSize=2&startFrom=0&pncId=2016%2F0000234W", SEARCH_QUERY_TYPE);
    }

    private static String getUnifiedSearchPartialResultForCJSSpec(final String caseId_1, final String defendantId_1) {
        return "{\n" +
                "  \"totalResults\": 1,\n" +
                "  \"cases\": [\n" +
                "    {\n" +
                "      \"caseReference\": \"45GB12345777\",\n" +
                "      \"prosecutionCaseId\": \"" + caseId_1 + "\",\n" +
                "      \"defendants\": [\n" +
                "        {\n" +
                "          \"defendantId\": \"" + defendantId_1 + "\",\n" +
                "          \"masterDefendantId\": \"0a5372c5-b60f-4d95-8390-8c6462e2d7af\",\n" +
                "          \"courtProceedingsInitiated\": \"2020-03-03T10:00:00.000Z\",\n" +
                "          \"firstName\": \"Lee\",\n" +
                "          \"middleName\": \"\",\n" +
                "          \"lastName\": \"Andy\",\n" +
                "          \"dateOfBirth\": \"\",\n" +
                "          \"pncId\": \"2016/0000234W\",\n" +
                "          \"croNumber\": \"123456/10L\",\n" +
                "          \"address\": {\n" +
                "            \"address1\": \"19, ABC street\",\n" +
                "            \"address2\": \"19th Lane\",\n" +
                "            \"address3\": \"\",\n" +
                "            \"address4\": \"\",\n" +
                "            \"address5\": \"London\",\n" +
                "            \"postcode\": \"HA1 1QF\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}
