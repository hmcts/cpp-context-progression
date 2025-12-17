package uk.gov.moj.cpp.progression.util;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.awaitility.Awaitility.waitAtMost;
import static uk.gov.moj.cpp.progression.stub.HearingStub.HEARING_COMMAND;
import static uk.gov.moj.cpp.progression.stub.HearingStub.HEARING_RESPONSE_TYPE;

import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;

import java.time.Duration;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.json.JSONException;
import org.json.JSONObject;


public class ReferBoxWorkApplicationHelper extends AbstractTestHelper {

    public static String getPostBoxWorkApplicationReferredHearing(final String applicationId) {
        return waitAtMost(Duration.ofMinutes(1)).until(() ->
                {
                    final Stream<JSONObject> boxWorkCourtHearingRequestsAsStream = getBoxWorkApplicationReferredToCourtHearingRequestsAsStream();
                    return boxWorkCourtHearingRequestsAsStream
                            .filter(
                                    payload -> {
                                        try {
                                            JSONObject courtApplication = payload.getJSONObject("hearing").getJSONArray("courtApplications").getJSONObject(0);
                                            return courtApplication.getString("id").equals(applicationId);
                                        } catch (Exception e) {
                                            return false;
                                        }
                                    }
                            ).findFirst().map(JSONObject::toString).orElse("{hearing:{}}");
                }, JsonPathMatchers.hasJsonPath("$.hearing")
        );

    }

    private static Stream<JSONObject> getBoxWorkApplicationReferredToCourtHearingRequestsAsStream() {
        return findAll(postRequestedFor(urlPathEqualTo(HEARING_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(HEARING_RESPONSE_TYPE)))
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .map(t -> {
                    try {
                        return new JSONObject(t);
                    } catch (JSONException e) {
                        return null;
                    }
                });
    }

}
