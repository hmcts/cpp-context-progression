package uk.gov.moj.cpp.progression.stub;

import java.text.MessageFormat;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.progression.DMConstants.DIRECTION_TWELVE_DOT_EIGHT;
import static uk.gov.moj.cpp.progression.DMConstants.DIRECTION_TWELVE_DOT_FOUR;
import static uk.gov.moj.cpp.progression.DMConstants.DIRECTION_TWELVE_DOT_NINE;
import static uk.gov.moj.cpp.progression.DMConstants.DIRECTION_TWELVE_DOT_SEVEN;
import static uk.gov.moj.cpp.progression.DMConstants.DIRECTION_TYPE_ID_1;
import static uk.gov.moj.cpp.progression.DMConstants.DIRECTION_TYPE_ID_2;
import static uk.gov.moj.cpp.progression.DMConstants.DIRECTION_TYPE_ID_5;
import static uk.gov.moj.cpp.progression.DMConstants.REFERENCE_DATA_ALL_DIRECTION_MEDIA_TYPE;
import static uk.gov.moj.cpp.progression.DMConstants.REFERENCE_DATA_ALL_DIRECTION_URL;
import static uk.gov.moj.cpp.progression.DMConstants.REFERENCE_DATA_DIRECTION_MANAGEMENT_TYPE_URL;
import static uk.gov.moj.cpp.progression.DMConstants.REFERENCE_DATA_DIRECTION_MEDIA_TYPE;
import static uk.gov.moj.cpp.progression.DMConstants.REFERENCE_DATA_DIRECTION_TYPE_MEDIA_TYPE;
import static uk.gov.moj.cpp.progression.DMConstants.REFERENCE_DATA_DIRECTION_URL;
import static uk.gov.moj.cpp.progression.DMConstants.REFERENCE_DATA_SERVICE_NAME;
import static uk.gov.moj.cpp.progression.DMConstants.DIRECTION_TWELVE_DOT_ELEVEN;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;


public class ReferenceDataDirectionStub {

    public static final LocalDate ORDER_DATE = LocalDate.now();

    public static void stubGetReferenceDataDirection() {
        stubGetReferenceDataDirection("stub-data/refdata-direction-one-response.json", DIRECTION_TYPE_ID_1, ORDER_DATE.toString());
        stubGetReferenceDataDirection("stub-data/refdata-direction-two-response.json", DIRECTION_TYPE_ID_2, ORDER_DATE.toString());
        stubGetReferenceDataDirection("stub-data/refdata-direction-twelve-dot-four.json", DIRECTION_TWELVE_DOT_FOUR, ORDER_DATE.toString());
        stubGetReferenceDataDirection("stub-data/refdata-direction-twelve-dot-eleven.json", DIRECTION_TWELVE_DOT_ELEVEN, ORDER_DATE.toString());
        stubGetReferenceDataDirection("stub-data/refdata-direction-twelve-dot-seven.json", DIRECTION_TWELVE_DOT_SEVEN, ORDER_DATE.toString());
        stubGetReferenceDataDirection("stub-data/refdata-direction-twelve-dot-eight.json", DIRECTION_TWELVE_DOT_EIGHT, ORDER_DATE.toString());
        stubGetReferenceDataDirection("stub-data/refdata-direction-twelve-dot-five.json", DIRECTION_TYPE_ID_5, ORDER_DATE.toString());
        stubGetReferenceDataDirection("stub-data/refdata-direction-twelve-dot-nine.json", DIRECTION_TWELVE_DOT_NINE, ORDER_DATE.toString());
        stubGetReferenceDataDirection("stub-data/refdata-direction-twelve-dot-nine.json", DIRECTION_TWELVE_DOT_NINE, ORDER_DATE.toString());
    }

    public static void stubGetReferenceDataDirectionManagementType() {
        stubGetReferenceDataDirectionManagementType("stub-data/refdata-direction-management-types-response.json", DIRECTION_TYPE_ID_1, ORDER_DATE.toString());
    }

    public static void stubGetReferenceDataAllDirection() {
        stubGetReferenceDataAllDirection("stub-data/refdata-all-direction.json");
    }


    private static void stubGetReferenceDataDirection(final String fileName, final String... args) {
        stubPingFor(REFERENCE_DATA_SERVICE_NAME);

        final String urlPath = MessageFormat.format(REFERENCE_DATA_DIRECTION_URL, args);

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getPayload(fileName))));
        waitForStubToBeReady(urlPath, REFERENCE_DATA_DIRECTION_MEDIA_TYPE);
    }

    private static void stubGetReferenceDataAllDirection(final String fileName) {
        stubPingFor(REFERENCE_DATA_SERVICE_NAME);

        stubFor(get(urlPathMatching(REFERENCE_DATA_ALL_DIRECTION_URL))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getPayload(fileName))));
        waitForStubToBeReady(REFERENCE_DATA_ALL_DIRECTION_URL, REFERENCE_DATA_ALL_DIRECTION_MEDIA_TYPE);
    }

    private static void stubGetReferenceDataDirectionManagementType(final String fileName, final String... args) {
        stubPingFor(REFERENCE_DATA_SERVICE_NAME);

        final String urlPath = REFERENCE_DATA_DIRECTION_MANAGEMENT_TYPE_URL;

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(getPayload(fileName))));
        waitForStubToBeReady(urlPath, REFERENCE_DATA_DIRECTION_TYPE_MEDIA_TYPE);
    }
    
}
