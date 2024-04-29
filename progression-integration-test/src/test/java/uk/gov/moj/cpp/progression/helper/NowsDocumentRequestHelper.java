package uk.gov.moj.cpp.progression.helper;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_FAILED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_GENERATED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_RECORDED;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.Metadata;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

public class NowsDocumentRequestHelper extends AbstractTestHelper {

    private String USER_ID = AbstractTestHelper.USER_ID;

    private static final String ORIGINATOR = "PRISON_COURT_REGISTER";

    protected MessageConsumer privateEventsConsumer;

    protected MessageConsumer privateEventsConsumer2;

    protected MessageConsumer privateEventsConsumer3;

    protected MessageProducer publicMessageProducer;

    public NowsDocumentRequestHelper() {
        privateEventsConsumer = QueueUtil.privateEvents.createPrivateConsumer(EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_RECORDED);
        privateEventsConsumer2 = QueueUtil.privateEvents.createPrivateConsumer(EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_GENERATED);
        privateEventsConsumer3 = QueueUtil.privateEvents.createPrivateConsumer(EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_FAILED);
        publicMessageProducer = QueueUtil.publicEvents.createPublicProducer();
    }

    public void verifyPrisonCourtRegisterDocumentRequestRecordedPrivateTopic(final String courtCentreId) {
        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.get("courtCentreId"), is(courtCentreId));
    }

    public void verifyPrisonCourtRegisterDocumentFailedPrivateTopic(final String courtCentreId, final String payloadFileId) {
        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer3);
        assertThat(jsonResponse.get("courtCentreId"), is(courtCentreId));
        assertThat(jsonResponse.get("payloadFileId"), is(payloadFileId));
    }

    public void verifyPrisonCourtRegisterRequestsExists(final UUID courtCentreId) {
        getPrisonCourtRegisterDocumentRequests(courtCentreId.toString(), allOf(
                JsonPathMatchers.withJsonPath("$.prisonCourtRegisterDocumentRequests[*].courtCentreId", hasItem(courtCentreId.toString()))
        ));
    }


    public String verifyPrisonCourtRegisterIsGenerated(final UUID courtCentreId, final UUID payloadFileServiceId) {
        return getPrisonCourtRegisterDocumentRequests(courtCentreId.toString(), allOf(
                JsonPathMatchers.withJsonPath("$.prisonCourtRegisterDocumentRequests[*].fileId", hasItem(payloadFileServiceId.toString())),
                JsonPathMatchers.withJsonPath("$.prisonCourtRegisterDocumentRequests[*].courtCentreId", hasItem(courtCentreId.toString()))
        ));
    }

    public void sendSystemDocGeneratorPublicAvailableEvent(final UUID userId, final UUID courtCentreId, final UUID payloadFileServiceId, final UUID documentFileServiceId) {
        final String commandName = "public.systemdocgenerator.events.document-available";
        final Metadata metadata = getMetadataFrom(userId.toString(), courtCentreId, commandName);
        sendMessage(publicMessageProducer, commandName, documentAvailablePayload(payloadFileServiceId, "OEE_Layout5", courtCentreId.toString(), documentFileServiceId), metadata);
    }

    public void sendSystemDocGeneratorPublicFailedEvent(final UUID userId, final UUID courtCentreId, final UUID payloadFileServiceId) {
        final String commandName = "public.systemdocgenerator.events.generation-failed";
        final Metadata metadata = getMetadataFrom(userId.toString(), courtCentreId, commandName);
        sendMessage(publicMessageProducer, commandName, documentFailedPayload(payloadFileServiceId, "OEE_Layout5", courtCentreId.toString()), metadata);
    }

    private JsonObject documentAvailablePayload(final UUID payloadFileServiceId, final String templateIdentifier, final String reportId, final UUID generatedDocumentId) {
        return Json.createObjectBuilder()
                .add("payloadFileServiceId", payloadFileServiceId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", ORIGINATOR)
                .add("documentFileServiceId", generatedDocumentId.toString())
                .add("generatedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("generateVersion", 1)
                .build();
    }

    private JsonObject documentFailedPayload(final UUID payloadFileServiceId, final String templateIdentifier, final String reportId) {
        return Json.createObjectBuilder()
                .add("payloadFileServiceId", payloadFileServiceId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", ORIGINATOR)
                .add("failedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("reason", "Test")
                .build();
    }

    private Metadata getMetadataFrom(final String userId, final UUID courtCentreId, String name) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ORIGINATOR, courtCentreId.toString())
                .add(ID, randomUUID().toString())
                .add(HeaderConstants.USER_ID, userId)
                .add(NAME, name)
                .build()).build();
    }

    private String getPrisonCourtRegisterDocumentRequests(final String requestStatus, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(StringUtils.join("/prison-court-register/request/", requestStatus)),
                "application/vnd.progression.query.prison-court-register-document-by-court-centre+json")
                .withHeader(HeaderConstants.USER_ID, USER_ID))
                .timeout(40, TimeUnit.SECONDS)
                .until(
                        status().is(Response.Status.OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }
}
