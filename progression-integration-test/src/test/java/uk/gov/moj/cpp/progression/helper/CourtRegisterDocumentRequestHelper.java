package uk.gov.moj.cpp.progression.helper;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_COURT_REGISTER_DOCUMENT_REQUEST_RECORDED;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import io.restassured.path.json.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;

public class CourtRegisterDocumentRequestHelper extends AbstractTestHelper {

    private String USER_ID = AbstractTestHelper.USER_ID;

    private static final String ORIGINATOR = "court_register";

    private static final JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(EVENT_SELECTOR_COURT_REGISTER_DOCUMENT_REQUEST_RECORDED).getMessageConsumerClient();
    private static final JmsMessageProducerClient publicMessageProducer = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    public void verifyCourtRegisterDocumentRequestRecordedPrivateTopic(final String courtCentreId) {
        final JsonPath jsonResponse = retrieveMessageAsJsonPath(privateEventsConsumer);
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
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(metadata, documentAvailablePayload(UUID.randomUUID(), "OEE_Layout5", courtCentreId.toString(), UUID.randomUUID()));
        publicMessageProducer.sendMessage(commandName, publicEventEnvelope);
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
