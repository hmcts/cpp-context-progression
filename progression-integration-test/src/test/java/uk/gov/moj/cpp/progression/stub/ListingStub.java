package uk.gov.moj.cpp.progression.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.helper.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

public class ListingStub {

    public static final String LISTING_COMMAND = "/listing-service/command/api/rest/listing/cases";
    public static final String LISTING_COMMAND_TYPE = "listing.command.send-case-for-listing";

    public static void stubSendCaseForListing() {
        InternalEndpointMockUtils.stubPingFor("listing-service");

        stubFor(post(urlPathEqualTo(LISTING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)));

        stubFor(get(urlPathEqualTo(LISTING_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(LISTING_COMMAND, LISTING_COMMAND_TYPE);
    }
}
