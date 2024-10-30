package uk.gov.moj.cpp.progression.util;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.awaitility.Awaitility.waitAtMost;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.stub.HearingStub.HEARING_COMMAND;
import static uk.gov.moj.cpp.progression.stub.HearingStub.HEARING_RESPONSE_TYPE;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDate;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.io.Resources;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.json.JSONException;
import org.json.JSONObject;


public class ReferBoxWorkApplicationHelper extends AbstractTestHelper {

    private static final String PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED = "public.progression.boxwork-application-referred";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    final String userId = randomUUID().toString();
    final String hearingId = randomUUID().toString();
    final String caseId = randomUUID().toString();
    final String dueDate = LocalDate.now().plusDays(5).toString();

    final JsonObject boxWorkApplicationJson = getHearingJsonObject("public.boxwork-application-referred.json", caseId, hearingId, dueDate);

    public ReferBoxWorkApplicationHelper() {
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED, userId), boxWorkApplicationJson);
        messageProducerClientPublic.sendMessage(PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED, publicEventEnvelope);
    }


    private JsonObject getHearingJsonObject(String path, String caseId, String hearingId, String dueDate) {
        return stringToJsonObjectConverter.convert(
                getPayloadForCreatingRequest(path)
                        .replaceAll("DUE_DATE", dueDate)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)

        );
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }

    public static String getPostBoxWorkApplicationReferredHearing(final String applicationId) {
        return waitAtMost(Duration.ofMinutes(5)).until(() ->
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
