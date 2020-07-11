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
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_COURT_REGISTER_DOCUMENT_REQUEST_RECORDED;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;

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

public class CourtRegisterDocumentRequestHelper extends AbstractTestHelper {

    private String USER_ID = AbstractTestHelper.USER_ID;

    private static final String ORIGINATOR = "court_register";

    protected MessageConsumer privateEventsConsumer;

    protected MessageProducer publicMessageProducer;

    public CourtRegisterDocumentRequestHelper() {
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_COURT_REGISTER_DOCUMENT_REQUEST_RECORDED);
        publicMessageProducer = QueueUtil.publicEvents.createProducer();
    }

    public void verifyCourtRegisterDocumentRequestRecordedPrivateTopic(final String courtCentreId) {
        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.get("courtCentreId"), is(courtCentreId));
    }

    public void verifyCourtRegisterRequestsExists(final UUID courtCentreId) {
        getCourtRegisterDocumentRequests(RegisterStatus.RECORDED.name(), allOf(
                JsonPathMatchers.withJsonPath("$.courtRegisterDocumentRequests[*].courtCentreId", hasItem(courtCentreId.toString())),
                JsonPathMatchers.withJsonPath("$.courtRegisterDocumentRequests[*].status", hasItem(RegisterStatus.RECORDED.name()))
        ));
    }


    public String verifyCourtRegisterIsGenerated(final UUID courtCentreId) {
        return getCourtRegisterDocumentRequests(RegisterStatus.GENERATED.name(), allOf(
                JsonPathMatchers.withJsonPath("$.courtRegisterDocumentRequests[*].status", hasItem(RegisterStatus.GENERATED.name())),
                JsonPathMatchers.withJsonPath("$.courtRegisterDocumentRequests[*].courtCentreId", hasItem(courtCentreId.toString()))
        ));
    }

    public void sendSystemDocGeneratorPublicEvent(final UUID userId, final UUID courtCentreId) {
        final String commandName = "public.systemdocgenerator.events.document-available";
        final Metadata metadata = getMetadataFrom(userId.toString(), courtCentreId);
        sendMessage(publicMessageProducer, commandName, documentAvailablePayload(UUID.randomUUID(), "OEE_Layout5", courtCentreId.toString(), UUID.randomUUID()), metadata);
    }

    private JsonObject documentAvailablePayload(final UUID templatePayloadId, final String templateIdentifier, final String reportId, final UUID generatedDocumentId) {
        return Json.createObjectBuilder()
                .add("payloadFileServiceId", templatePayloadId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", "CourtRegister")
                .add("documentFileServiceId", generatedDocumentId.toString())
                .add("generatedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("generateVersion", 1)
                .build();
    }

    private Metadata getMetadataFrom(final String userId, final UUID courtCentreId) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ORIGINATOR, courtCentreId.toString())
                .add(ID, randomUUID().toString())
                .add(HeaderConstants.USER_ID, userId)
                .add(NAME, "public.systemdocgenerator.events.document-available")
                .build()).build();
    }

    private String getCourtRegisterDocumentRequests(final String requestStatus, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(StringUtils.join("/court-register/request/", requestStatus)),
                "application/vnd.progression.query.court-register-document-request+json")
                .withHeader(HeaderConstants.USER_ID, USER_ID))
                .timeout(40, TimeUnit.SECONDS)
                .until(
                        status().is(Response.Status.OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    public void verifyCourtRegisterIsNotified(final UUID courtCentreId) {
        getCourtRegisterDocumentRequests(RegisterStatus.NOTIFIED.name(), allOf(
                JsonPathMatchers.withJsonPath("$.courtRegisterDocumentRequests[*].status", hasItem(RegisterStatus.NOTIFIED.name())),
                JsonPathMatchers.withJsonPath("$.courtRegisterDocumentRequests[*].courtCentreId", hasItem(courtCentreId.toString()))
        ));
    }
}
