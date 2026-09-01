package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_FAILED;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider;
import uk.gov.justice.services.messaging.Metadata;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;

public class PrisonCourtRegisterDocumentRequestHelper extends AbstractTestHelper {

    private String USER_ID = AbstractTestHelper.USER_ID;

    private StringToJsonObjectConverter stringToJsonObjectConverter;

    private JmsMessageConsumerClient consumerForCourtApplicationCreated;

    private static final String ORIGINATOR = "PRISON_COURT_REGISTER";

    private JmsMessageConsumerClient privateEventsConsumer3;

    protected JmsMessageProducerClient publicMessageProducer;

    public PrisonCourtRegisterDocumentRequestHelper() {
        privateEventsConsumer3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_FAILED).getMessageConsumerClient();
        publicMessageProducer = JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        stringToJsonObjectConverter = new StringToJsonObjectConverter();
        consumerForCourtApplicationCreated = JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.court-application-proceedings-initiated").getMessageConsumerClient();
    }

    public void verifyPrisonCourtRegisterDocumentFailedPrivateTopic(final String courtCentreId, final String payloadFileId, final String prisonCourtRegisterId) {
        final JsonPath jsonResponse = retrieveMessageAsJsonPath(privateEventsConsumer3);
        assertThat(jsonResponse.get("courtCentreId"), is(courtCentreId));
        assertThat(jsonResponse.get("payloadFileId"), is(payloadFileId));
        assertThat(jsonResponse.get("id"), is(prisonCourtRegisterId));
    }

    public void verifyPrisonCourtRegisterRequestsExists(final UUID courtCentreId) {
        getPrisonCourtRegisterDocumentRequests(courtCentreId.toString(), allOf(
                JsonPathMatchers.withJsonPath("$.prisonCourtRegisterDocumentRequests[*].courtCentreId", hasItem(courtCentreId.toString()))
        ));
    }

    public void verifyPrisonCourtRegisterRequestsExists(final UUID courtCentreId, final UUID hearingId) {
        final String prisonCourtRegisterDocumentRequestPayload = getPrisonCourtRegisterDocumentRequests(courtCentreId, allOf(
                withJsonPath("$.prisonCourtRegisterDocumentRequests[*].courtCentreId", hasItem(courtCentreId.toString())),
                withJsonPath("$.prisonCourtRegisterDocumentRequests[*].fileId", is(notNullValue()))
        ));

        final JsonObject prisonCourtRegisterDocumentRequestJsonObject = stringToJsonObjectConverter.convert(prisonCourtRegisterDocumentRequestPayload);
        final JsonObject prisonCourtRegisterDocumentRequest = prisonCourtRegisterDocumentRequestJsonObject.getJsonArray("prisonCourtRegisterDocumentRequests").getJsonObject(0);
        final String payload = prisonCourtRegisterDocumentRequest.getString("payload");
        final JsonObject payloadJsonObject = stringToJsonObjectConverter.convert(payload);
        assertThat(payloadJsonObject.getString("hearingId"), is(hearingId.toString()));
    }


    public String verifyPrisonCourtRegisterIsGenerated(final UUID courtCentreId, final UUID payloadFileServiceId, final String prisonCourtRegisterId) {
        return getPrisonCourtRegisterDocumentRequests(courtCentreId.toString(), allOf(
                JsonPathMatchers.withJsonPath("$.prisonCourtRegisterDocumentRequests[*].fileId", hasItem(payloadFileServiceId.toString())),
                JsonPathMatchers.withJsonPath("$.prisonCourtRegisterDocumentRequests[*].id", hasItem(prisonCourtRegisterId)),
                JsonPathMatchers.withJsonPath("$.prisonCourtRegisterDocumentRequests[*].courtCentreId", hasItem(courtCentreId.toString()))
        ));
    }

    public String verifyPrisonCourtRegisterIsGeneratedWithoutPrisonCourtRegisterId(final UUID courtCentreId, final UUID payloadFileServiceId) {
        return getPrisonCourtRegisterDocumentRequests(courtCentreId.toString(), allOf(
                JsonPathMatchers.withJsonPath("$.prisonCourtRegisterDocumentRequests[*].fileId", hasItem(payloadFileServiceId.toString())),
                JsonPathMatchers.withJsonPath("$.prisonCourtRegisterDocumentRequests[*].courtCentreId", hasItem(courtCentreId.toString()))
        ));
    }

    public void sendSystemDocGeneratorPublicAvailableEvent(final UUID userId, final UUID prisonCourtRegisterStreamId, final UUID payloadFileServiceId, final UUID documentFileServiceId, final String id) {
        final String commandName = "public.systemdocgenerator.events.document-available";
        final Metadata metadata = getMetadataFrom(userId.toString(), prisonCourtRegisterStreamId, commandName);
        publicMessageProducer.sendMessage(commandName, envelopeFrom(metadata, documentAvailablePayload(payloadFileServiceId, "OEE_Layout5", prisonCourtRegisterStreamId.toString(), documentFileServiceId, id)));
    }

    public void sendSystemDocGeneratorPublicFailedEvent(final UUID userId, final UUID courtCentreId, final UUID payloadFileServiceId, final String prisonCourtRegisterId) {
        final String commandName = "public.systemdocgenerator.events.generation-failed";
        final Metadata metadata = getMetadataFrom(userId.toString(), courtCentreId, commandName);
        publicMessageProducer.sendMessage(commandName, envelopeFrom(metadata, documentFailedPayload(payloadFileServiceId, "OEE_Layout5", courtCentreId.toString(), prisonCourtRegisterId)));
    }

    public void verifyCourtApplicationCreatedPublicEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(applicationReference, is(notNullValue()));
    }

    public void verifyInMessagingQueueForProsecutionCaseCreated(final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated) {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
        final JsonObject reportingRestrictionObject = message.get().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions").getJsonObject(0);
        assertNotNull(reportingRestrictionObject);
    }

    private JsonObject documentAvailablePayload(final UUID payloadFileServiceId, final String templateIdentifier, final String reportId, final UUID generatedDocumentId, final String prisonCourtRegisterId) {
        return createObjectBuilder()
                .add("payloadFileServiceId", payloadFileServiceId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", ORIGINATOR)
                .add("documentFileServiceId", generatedDocumentId.toString())
                .add("generatedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("generateVersion", 1)
                .add("additionalInformation", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("propertyName", "prisonCourtRegisterId")
                                .add("propertyValue", prisonCourtRegisterId)
                        )
                )
                .build();
    }

    private JsonObject documentFailedPayload(final UUID payloadFileServiceId, final String templateIdentifier, final String reportId, final String prisonCourtRegisterId) {
        return createObjectBuilder()
                .add("payloadFileServiceId", payloadFileServiceId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", ORIGINATOR)
                .add("failedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("reason", "Test")
                .add("additionalInformation", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("propertyName", "prisonCourtRegisterId")
                                .add("propertyValue", prisonCourtRegisterId)
                        )
                )
                .build();
    }

    private Metadata getMetadataFrom(final String userId, final UUID courtCentreId, String name) {
        return metadataFrom(createObjectBuilder()
                .add(ORIGINATOR, courtCentreId.toString())
                .add(ID, randomUUID().toString())
                .add(HeaderConstants.USER_ID, userId)
                .add(NAME, name)
                .build()).build();
    }

    private String getPrisonCourtRegisterDocumentRequests(final String requestStatus, final Matcher... matchers) {
        return pollForResponse("/prison-court-register/request/" + requestStatus,
                "application/vnd.progression.query.prison-court-register-document-by-court-centre+json",
                USER_ID.toString(),
                matchers
        );
    }

    private String getPrisonCourtRegisterDocumentRequests(final UUID courtCentreId, final Matcher... matchers) {
        return pollForResponse("/prison-court-register/request/" + courtCentreId,
                "application/vnd.progression.query.prison-court-register-document-by-court-centre+json",
                USER_ID.toString(),
                matchers
        );
    }
}
