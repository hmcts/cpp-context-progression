package uk.gov.moj.cpp.progression.helper;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_INFORMANT_REGISTER_DOCUMENT_REQUEST_GENERATED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_INFORMANT_REGISTER_DOCUMENT_REQUEST_NOTIFICATION_IGNORED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_INFORMANT_REGISTER_DOCUMENT_REQUEST_NOTIFIED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_INFORMANT_REGISTER_DOCUMENT_REQUEST_RECORDED;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.jsonpath.matchers.IsJson;
import org.hamcrest.CoreMatchers;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import com.jayway.restassured.path.json.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;

public class InformantRegisterDocumentRequestHelper extends AbstractTestHelper {
    protected MessageConsumer privateIRRecordedEventsConsumer;
    protected MessageConsumer privateNotifiedEventsConsumer;
    protected MessageConsumer privateGeneratedEventsConsumer;
    protected MessageConsumer privateNotificationIgnoredEventsConsumer;
    private static final String ORIGINATOR = "informant_register";

    protected MessageProducer publicMessageProducer;

    public InformantRegisterDocumentRequestHelper() {
        privateIRRecordedEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_INFORMANT_REGISTER_DOCUMENT_REQUEST_RECORDED);
        privateNotifiedEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_INFORMANT_REGISTER_DOCUMENT_REQUEST_NOTIFIED);
        privateNotificationIgnoredEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_INFORMANT_REGISTER_DOCUMENT_REQUEST_NOTIFICATION_IGNORED);
        publicMessageProducer = QueueUtil.publicEvents.createProducer();
    }

    public void verifyInformantRegisterDocumentRequestRecordedPrivateTopic(final String prosecutionAuthId) {
        final JsonPath jsonResponse = retrieveMessage(privateIRRecordedEventsConsumer);
        assertThat(jsonResponse.get("prosecutionAuthorityId"), is(prosecutionAuthId));
    }

    public void verifyInformantRegisterDocumentRequestNotifiedPrivateTopic(final String prosecutionAuthId) {
        await().pollInterval(Duration.ONE_SECOND).atMost(Duration.ONE_MINUTE).until(() -> retrieveMessage(privateNotifiedEventsConsumer).getString("prosecutionAuthorityId"),
                is(prosecutionAuthId));
    }

    public void verifyInformantRegisterNotificationIgnoredPrivateTopic(final String prosecutionAuthId) {
        await().pollInterval(Duration.ONE_SECOND).atMost(Duration.ONE_MINUTE).until(() -> retrieveMessage(privateNotificationIgnoredEventsConsumer).getString("prosecutionAuthorityId"),
                is(prosecutionAuthId));
    }

    public void verifyInformantRegisterRequestsExists(final UUID prosecutionAuthorityId) {
        getInformantRegisterDocumentRequests(RegisterStatus.RECORDED.name(), allOf(
                withJsonPath("$.informantRegisterDocumentRequests[*].prosecutionAuthorityId", hasItem(prosecutionAuthorityId.toString())),
                withJsonPath("$.informantRegisterDocumentRequests[*].status", hasItem(RegisterStatus.RECORDED.name()))
        ));
    }

    public String verifyInformantRegisterIsGenerated(final UUID prosecutionAuthorityId) {
        return getInformantRegisterDocumentRequests(RegisterStatus.GENERATED.name(), allOf(
                withJsonPath("$.informantRegisterDocumentRequests[*].prosecutionAuthorityId", hasItem(prosecutionAuthorityId.toString())),
                withJsonPath("$.informantRegisterDocumentRequests[*].status", hasItem(RegisterStatus.GENERATED.name()))
        ));
    }

    public void sendMaterialFileUploadedPublicEvent(final UUID userId, final UUID materialId, final UUID prosecutionAuthorityId) {
        final String commandName = "material.material-added";
        final Metadata metadata = getMetadataFrom(userId.toString(), prosecutionAuthorityId);
        final JsonObject payload = Json.createObjectBuilder().add("materialId", materialId.toString()).add(
                "fileDetails",
                Json.createObjectBuilder().add("alfrescoAssetId", "aGVsbG8=")
                        .add("mimeType", "text/plain").add("fileName", "file.txt"))
                .add("materialAddedDate", "2016-04-26T13:01:787.345").build();
        sendMessage(publicMessageProducer, commandName, payload, metadata);
    }

    private Metadata getMetadataFrom(final String userId, final UUID prosecutionAuthorityId) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ORIGINATOR, prosecutionAuthorityId.toString())
                .add(ID, randomUUID().toString())
                .add(HeaderConstants.USER_ID, userId)
                .add(NAME, "material.material-added")
                .build()).build();
    }

    private String getInformantRegisterDocumentRequests(final String status, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(StringUtils.join("/informant-register/request/", status)),
                "application/vnd.progression.query.informant-register-document-request+json")
                .withHeader(HeaderConstants.USER_ID, USER_ID))
                .timeout(60, TimeUnit.SECONDS)
                .until(
                        status().is(Response.Status.OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    public void verifyInformantRegisterIsNotified(final UUID prosecutionAuthorityId) {
        getInformantRegisterDocumentRequests(RegisterStatus.NOTIFIED.name(), allOf(
                withJsonPath("$.informantRegisterDocumentRequests[*].status", hasItem(RegisterStatus.NOTIFIED.name())),
                withJsonPath("$.informantRegisterDocumentRequests[*].prosecutionAuthorityId", hasItem(prosecutionAuthorityId.toString()))
        ));
    }
}
