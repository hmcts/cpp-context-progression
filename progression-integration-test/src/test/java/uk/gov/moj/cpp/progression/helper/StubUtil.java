package uk.gov.moj.cpp.progression.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static uk.gov.moj.cpp.progression.helper.FileUtil.getPayload;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Class to set up stub.
 */
public class StubUtil {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final int HTTP_STATUS_OK = 200;

    static {
        configureFor(HOST, 8080);
        reset();
    }


    public static void setupStructureCaseStub(final String caseId, final String defendentId) {
        // InternalEndpointMockUtils.stubPingFor("/structure-query-api");
        stubFor(get(urlPathEqualTo("/structure-query-api/query/api/rest/structure/cases/.*"))
                        .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                                        .withHeader("CPPID", UUID.randomUUID().toString())
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(getJsonBodyStr(
                                                        "structure.query.case-defendants.json",
                                                        caseId, defendentId))));
    }

    public static void setupUsersGroupDataActionClassificationStub() {
        // InternalEndpointMockUtils.stubPingFor("usersgroups-query-api");
        stubFor(get(urlMatching("/usersgroups-query-api/query/api/rest/usersgroups/users/.*"))
                        .willReturn(aResponse().withStatus(200)
                                        .withHeader("CPPID", UUID.randomUUID().toString())
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(getPayload("users-groups-system-user.json"))));

    }

    private static String getJsonBodyStr(final String fileName, final String caseId,
                    final String defendantId) {
        final String str = getPayload(fileName);
        return str.replace("RANDOM_ID", UUID.randomUUID().toString())
                        .replace("RANDOM_CASE_ID", caseId).replace("DEF_ID_1", defendantId)
                        .replace("DEF_ID_2", UUID.randomUUID().toString())
                        .replace("DEF_PRG_ID", UUID.randomUUID().toString())
                        .replace("TODAY", LocalDate.now().toString());
    }

}
