package uk.gov.moj.cpp.progression.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static uk.gov.moj.cpp.progression.helper.FileUtil.getPayload;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

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


    public static void resetStubs() {
        reset();
        InternalEndpointMockUtils.stubPingFor("structure-query-api");
        InternalEndpointMockUtils.stubPingFor("usersgroups-query-api");
    }


    public static void setupStructureCaseStub(final String caseId, final String defendentId,final String defendent2Id,
                    final String caseProgressionId) {
        InternalEndpointMockUtils.stubPingFor("structure-query-api");
        stubFor(get(urlMatching("/structure-query-api/query/api/rest/structure/cases/.*"))
                        .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                                        .withHeader("CPPID", UUID.randomUUID().toString())
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(getJsonBodyStr(
                                                        "structure.query.case-defendants.json",
                                                        caseId, defendentId,defendent2Id, caseProgressionId))));
    }

    public static void setupUsersGroupQueryStub() {
        InternalEndpointMockUtils.stubPingFor("usersgroups-query-api");
        stubFor(get(urlMatching("/usersgroups-query-api/query/api/rest/usersgroups/users/.*"))
                        .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                                        .withHeader("CPPID", UUID.randomUUID().toString())
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(getPayload("users-groups-listing-officers-user.json"))));

    }

    public static String getJsonBodyStr(final String path, final String caseId,
                    final String defendantId, final String defendant2Id, final String caseProgressionId) {
        final String payload = getPayload(path);
        return payload.replace("RANDOM_ID", caseProgressionId).replace("RANDOM_CASE_ID", caseId)
                        .replace("DEF_ID_1", defendantId)
                        .replace("DEF_ID_2", defendant2Id)
                        .replace("DEF_PRG_ID", defendantId)
                        .replace("TODAY", LocalDate.now().toString());
    }

}
