package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.Collection;
import java.util.List;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysDocGeneratorStub {

    private static final String SYS_DOC_GENERATOR_URL = "/.*/rest/systemdocgenerator/generate-document";

    private static final String GENERATE_DOCUMENT_MEDIA_TYPE = "application/vnd.systemdocgenerator.generate-document+json";

    private final static Logger LOGGER = LoggerFactory.getLogger(SysDocGeneratorStub.class);

    public static void stubDocGeneratorEndPoint() {
        stubPingFor("systemdocgenerator-service");

        stubFor(post(urlPathMatching(SYS_DOC_GENERATOR_URL))
                .withHeader(CONTENT_TYPE, equalTo(GENERATE_DOCUMENT_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, GENERATE_DOCUMENT_MEDIA_TYPE)
                ));
    }

    public static List<JSONObject> pollSysDocGenerationRequests(final Matcher<Collection<?>> matcher) {
        try {

            return await().until(() ->
                    findAll(postRequestedFor(urlPathMatching(SYS_DOC_GENERATOR_URL)))
                            .stream()
                            .map(LoggedRequest::getBodyAsString)
                            .map(t -> {
                                try {
                                    return new JSONObject(t);
                                } catch (JSONException e) {
                                    return null;
                                }
                            })
                            .collect(toList()), matcher);
        } catch (final ConditionTimeoutException timeoutException) {
            LOGGER.info("Exception while finding the captured requests in wire mock:" + timeoutException);
            return emptyList();
        }
    }

}
