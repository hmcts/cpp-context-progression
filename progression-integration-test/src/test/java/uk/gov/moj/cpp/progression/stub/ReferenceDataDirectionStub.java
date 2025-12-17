package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.progression.DMConstants.DIRECTION_TYPE_ID_1;
import static uk.gov.moj.cpp.progression.DMConstants.REFERENCE_DATA_ALL_DIRECTION_URL;
import static uk.gov.moj.cpp.progression.DMConstants.REFERENCE_DATA_DIRECTION_MANAGEMENT_TYPE_URL;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import java.time.LocalDate;


public class ReferenceDataDirectionStub {

    public static final LocalDate ORDER_DATE = LocalDate.now();

    public static void stubGetReferenceDataDirectionManagementType() {
        stubGetReferenceDataDirectionManagementType("stub-data/refdata-direction-management-types-response.json", DIRECTION_TYPE_ID_1, ORDER_DATE.toString());
    }

    public static void stubGetReferenceDataAllDirection() {
        stubGetReferenceDataAllDirection("stub-data/refdata-all-direction.json");
    }

    private static void stubGetReferenceDataAllDirection(final String fileName) {
        stubFor(get(urlPathMatching(REFERENCE_DATA_ALL_DIRECTION_URL))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getPayload(fileName))));
    }

    private static void stubGetReferenceDataDirectionManagementType(final String fileName, final String... args) {
        stubFor(get(urlPathMatching(REFERENCE_DATA_DIRECTION_MANAGEMENT_TYPE_URL))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getPayload(fileName))));
    }
    
}
