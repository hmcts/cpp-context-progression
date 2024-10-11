package uk.gov.moj.cpp.progression;

import static com.google.common.collect.ImmutableList.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.join;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.stubForApiNotification;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyCreateLetterRequested;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyNoLetterRequested;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import io.restassured.path.json.JsonPath;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.jms.JMSException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class NowDocumentRequestIT extends AbstractIT {

    private static final String NOW_DOCUMENT_REQUESTS = "nowDocumentRequests";
    private static final String MATERIAL_ID = "materialId";
    private static final String MATERIAL_MATERIAL_ADDED = "material.material-added";
    private static final String HEARING_ID = "%HEARING_ID%";
    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String ORIGINATOR = "originator";
    private static final String ORIGINATOR_VALUE = "court";
    private static final String PUBLIC_PROGRESSION_NOW_DOCUMENT_REQUESTED = "public.progression.now-document-requested";
    private static final JmsMessageConsumerClient messageConsumerClientPublicForNowDocumentRequested = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_NOW_DOCUMENT_REQUESTED).getMessageConsumerClient();

    private static final JmsMessageConsumerClient privateCourtDocumentAddedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-document-added").getMessageConsumerClient();
    private String materialId;
    private String hearingId;
    private String caseId1;
    private String caseId2;
    private String defendantId;
    private String requestId;
    private UUID userId;
    final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    public static Stream<Arguments> incompleteOrderAddresseePayloads() {
        return Stream.of(
                Arguments.of("progression.add-now-document-request-no-fixed-abode.json"),
                Arguments.of("progression.add-now-document-request-addressee-postcode-missing.json")
        );
    }

    @BeforeAll
    public static void setupBefore() {
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        NotificationServiceStub.setUp();
    }

    @BeforeEach
    public void setup() {
        hearingId = randomUUID().toString();
        caseId1 = randomUUID().toString();
        caseId2 = randomUUID().toString();
        defendantId = randomUUID().toString();
        materialId = randomUUID().toString();
        requestId = randomUUID().toString();
        userId = randomUUID();
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldAddFinancialNowDocumentRequest() throws IOException {
        final String body = prepareAddNowFinancialDocumentRequestPayload();

        final Response writeResponse = postCommand(getWriteUrl("/nows"),
                "application/vnd.progression.add-now-document-request+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        getNowDocumentRequestsFor(requestId, anyOf(
                withJsonPath("$.nowDocumentRequests[0].requestId", equalTo(requestId)),
                withJsonPath("$.nowDocumentRequests[0].hearingId", equalTo(hearingId)),
                withJsonPath("$.nowDocumentRequests[0].materialId", equalTo(materialId)))
        );

        sendPublicEventForFinancialImpositionAcknowledgement();

        verifyMaterialCreated();

        sendPublicEventForMaterialAdded();

        verifyCreateLetterRequested(of("letterUrl", materialId));
    }

    @Test
    public void shouldAddNonFinancialNowDocumentRequest() throws IOException {
        final String payload = prepareAddNowNonFinancialDocumentRequestPayload("progression.add-non-financial-now-document-request.json");
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(payload);
        final NowDocumentRequest nowDocumentRequest = jsonToObjectConverter.convert(jsonObject, NowDocumentRequest.class);

        final Response writeResponse = postCommand(getWriteUrl("/nows"),
                "application/vnd.progression.add-now-document-request+json",
                payload);

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String nowDocumentRequestPayload = getNowDocumentRequest(hearingId,
                anyOf(withJsonPath("$.nowDocumentRequests[0].hearingId", equalTo(hearingId))));

        verifyMaterialCreated();

        sendPublicEventForMaterialAdded();

        verifyCreateLetterRequested(of("letterUrl", materialId));

        final JsonObject nowDocumentRequests = stringToJsonObjectConverter.convert(nowDocumentRequestPayload);
        final JsonObject nowDocumentRequestJsonObject = nowDocumentRequests.getJsonArray(NOW_DOCUMENT_REQUESTS).getJsonObject(0);
        assertThat(nowDocumentRequest.getMaterialId().toString(), is(nowDocumentRequestJsonObject.getString(MATERIAL_ID)));
    }

    @MethodSource("incompleteOrderAddresseePayloads")
    @ParameterizedTest
    public void shouldSuppressPostalNotificationWhenOrderAddresseeAddressIsInvalid(final String fileName) throws IOException {
        final String payload = prepareAddNowNonFinancialDocumentRequestPayload(fileName);
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(payload);
        final NowDocumentRequest nowDocumentRequest = jsonToObjectConverter.convert(jsonObject, NowDocumentRequest.class);

        final Response writeResponse = postCommand(getWriteUrl("/nows"),
                "application/vnd.progression.add-now-document-request+json",
                payload);

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String nowDocumentRequestPayload = getNowDocumentRequest(hearingId,
                anyOf(withJsonPath("$.nowDocumentRequests[0].hearingId", equalTo(hearingId))));

        verifyMaterialCreated();

        sendPublicEventForMaterialAdded();

        verifyNoLetterRequested(of("letterUrl", materialId));

        final JsonObject nowDocumentRequests = stringToJsonObjectConverter.convert(nowDocumentRequestPayload);
        final JsonObject nowDocumentRequestJsonObject = nowDocumentRequests.getJsonArray(NOW_DOCUMENT_REQUESTS).getJsonObject(0);
        assertThat(nowDocumentRequest.getMaterialId().toString(), is(nowDocumentRequestJsonObject.getString(MATERIAL_ID)));
    }

    @Test
    public void shouldEmailNowDocumentRequest() throws IOException {
        final String payload = prepareEmailDocumentRequestPayload();
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(payload);
        final NowDocumentRequest nowDocumentRequest = jsonToObjectConverter.convert(jsonObject, NowDocumentRequest.class);

        final Response writeResponse = postCommand(getWriteUrl("/nows"),
                "application/vnd.progression.add-now-document-request+json",
                payload);

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String nowDocumentRequestPayload = getNowDocumentRequest(hearingId,
                anyOf(withJsonPath("$.nowDocumentRequests[0].hearingId", equalTo(hearingId))));

        sendMaterialFileUploadedPublicEvent(fromString(materialId), userId);

        final JsonObject nowDocumentRequests = stringToJsonObjectConverter.convert(nowDocumentRequestPayload);
        final JsonObject nowDocumentRequestJsonObject = nowDocumentRequests.getJsonArray(NOW_DOCUMENT_REQUESTS).getJsonObject(0);
        assertThat(nowDocumentRequest.getMaterialId().toString(), is(nowDocumentRequestJsonObject.getString(MATERIAL_ID)));

        verifyInMessagingQueue(messageConsumerClientPublicForNowDocumentRequested);
    }

    @Test
    public void shouldSendApiNotificationNowDocumentRequest() throws IOException, JSONException, JMSException {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        stubForApiNotification();
        List<String> caseUrns = createCaseAndFetchCaseUrn(1);

        final String payload = prepareApiNotificationDocumentRequestPayload();
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(payload);
        final NowDocumentRequest nowDocumentRequest = jsonToObjectConverter.convert(jsonObject, NowDocumentRequest.class);

        final Response writeResponse = postCommand(getWriteUrl("/nows"),
                "application/vnd.progression.add-now-document-request+json",
                payload);

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String nowDocumentRequestPayload = getNowDocumentRequest(hearingId,
                anyOf(withJsonPath("$.nowDocumentRequests[0].hearingId", equalTo(hearingId))));

        sendMaterialFileUploadedPublicEvent(fromString(materialId), userId);

        final JsonObject nowDocumentRequests = stringToJsonObjectConverter.convert(nowDocumentRequestPayload);
        final JsonObject nowDocumentRequestJsonObject = nowDocumentRequests.getJsonArray(NOW_DOCUMENT_REQUESTS).getJsonObject(0);
        assertThat(nowDocumentRequest.getMaterialId().toString(), is(nowDocumentRequestJsonObject.getString(MATERIAL_ID)));

        verifyInMessagingQueue(messageConsumerClientPublicForNowDocumentRequested);
    }

    @Test
    public void shouldSendApiNotificationNowDocumentRequest_multipleCases() throws IOException, JSONException, JMSException {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        stubForApiNotification();
        List<String> caseUrns = createCaseAndFetchCaseUrn(2);

        final String payload = prepareApiNotificationDocumentRequestPayloadForMultipleCases();
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(payload);
        final NowDocumentRequest nowDocumentRequest = jsonToObjectConverter.convert(jsonObject, NowDocumentRequest.class);

        final Response writeResponse = postCommand(getWriteUrl("/nows"),
                "application/vnd.progression.add-now-document-request+json",
                payload);

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String nowDocumentRequestPayload = getNowDocumentRequest(hearingId,
                anyOf(withJsonPath("$.nowDocumentRequests[0].hearingId", equalTo(hearingId))));

        sendMaterialFileUploadedPublicEvent(fromString(materialId), userId);

        verifyCourtDocumentAdded();

        final JsonObject nowDocumentRequests = stringToJsonObjectConverter.convert(nowDocumentRequestPayload);
        final JsonObject nowDocumentRequestJsonObject = nowDocumentRequests.getJsonArray(NOW_DOCUMENT_REQUESTS).getJsonObject(0);
        assertThat(nowDocumentRequest.getMaterialId().toString(), is(nowDocumentRequestJsonObject.getString(MATERIAL_ID)));

        verifyInMessagingQueue(messageConsumerClientPublicForNowDocumentRequested);
    }

    private static void verifyCourtDocumentAdded() {
        final JsonPath prosecutionCaseDefendantListingStatusChanged = QueueUtil.retrieveMessageAsJsonPath(privateCourtDocumentAddedConsumer);

        assertThat(prosecutionCaseDefendantListingStatusChanged.get("courtDocument.documentTypeDescription"), is("Electronic Notifications"));
        assertThat(prosecutionCaseDefendantListingStatusChanged.get("courtDocument.documentTypeId"), is("f471eb51-614c-4447-bd8d-28f9c2815c9e"));
        assertThat(prosecutionCaseDefendantListingStatusChanged.get("courtDocument.mimeType"), is("application/pdf"));
    }

    private List<String> createCaseAndFetchCaseUrn(int noOfCases) throws IOException, JMSException , JSONException {
        List<String> caseUrns = new ArrayList<>();
        int i = 0;
        String caseId = caseId1;
        while (i < noOfCases) {
            newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed").getMessageConsumerClient();
            addProsecutionCaseToCrownCourt(caseId, defendantId);
            String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            JsonObject jsonObject = jsonToObjectConverter.convert(stringToJsonObjectConverter.convert(response), JsonObject.class);
            JsonObject prosecutionCase = jsonObject.getJsonObject("prosecutionCase");
            JsonObject pcIdentifier = prosecutionCase.getJsonObject("prosecutionCaseIdentifier");
            caseUrns.add(pcIdentifier.getJsonString("prosecutionAuthorityReference").getString());

            i++;
            caseId = caseId2;
        }

        return caseUrns;
    }

    @Test
    public void shouldNotSendEmailWithNoOrderAddresseeInNowDocumentRequest() throws IOException {
        final String payload = prepareNoOrderAddresseeDocumentRequestPayload();
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(payload);
        final NowDocumentRequest nowDocumentRequest = jsonToObjectConverter.convert(jsonObject, NowDocumentRequest.class);

        final Response writeResponse = postCommand(getWriteUrl("/nows"),
                "application/vnd.progression.add-now-document-request+json",
                payload);

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String nowDocumentRequestPayload = getNowDocumentRequest(hearingId,
                anyOf(withJsonPath("$.nowDocumentRequests[0].hearingId", equalTo(hearingId))));

        sendMaterialFileUploadedPublicEvent(fromString(materialId), userId);

        final JsonObject nowDocumentRequests = stringToJsonObjectConverter.convert(nowDocumentRequestPayload);
        final JsonObject nowDocumentRequestJsonObject = nowDocumentRequests.getJsonArray(NOW_DOCUMENT_REQUESTS).getJsonObject(0);
        assertThat(nowDocumentRequest.getMaterialId().toString(), is(nowDocumentRequestJsonObject.getString(MATERIAL_ID)));
    }

    private void sendMaterialFileUploadedPublicEvent(final UUID materialId, final UUID userId) {
        final Metadata metadata = getMetadataFrom(userId.toString());
        final JsonObject payload = createObjectBuilder().add(MATERIAL_ID, materialId.toString()).add(
                        "fileDetails",
                        createObjectBuilder().add("alfrescoAssetId", "aGVsbG8=")
                                .add("mimeType", "text/plain").add("fileName", "file.txt"))
                .add("materialAddedDate", "2016-04-26T13:01:787.345").build();
        messageProducerClientPublic.sendMessage(MATERIAL_MATERIAL_ADDED, envelopeFrom(metadata, payload));
    }

    private void sendPublicEventForMaterialAdded() {
        final JsonObject materialAddPublicEventPayload = createObjectBuilder()
                .add(MATERIAL_ID, materialId)
                .add("fileServiceId", randomUUID().toString())
                .build();

        final Metadata metadata = metadataFrom(createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(NAME, MATERIAL_MATERIAL_ADDED)
                .add(ORIGINATOR, ORIGINATOR_VALUE)
                .add("context", createObjectBuilder()
                        .add(JsonMetadata.USER_ID, userId.toString()))
                .build()).build();

        messageProducerClientPublic.sendMessage(MATERIAL_MATERIAL_ADDED, envelopeFrom(metadata, materialAddPublicEventPayload));
    }

    private void sendPublicEventForFinancialImpositionAcknowledgement() {
        final String eventName = "public.stagingenforcement.enforce-financial-imposition-acknowledgement";

        final JsonObject payload = generateSuccessfulAcknowledgement(requestId);

        messageProducerClientPublic.sendMessage(eventName, envelopeFrom(metadataOf(randomUUID(), eventName)
                .withUserId(randomUUID().toString())
                .build(), payload));
    }

    private Metadata getMetadataFrom(final String userId) {
        return metadataFrom(createObjectBuilder()
                .add(ORIGINATOR, ORIGINATOR_VALUE)
                .add(ID, randomUUID().toString())
                .add(JsonMetadata.USER_ID, userId)
                .add(NAME, MATERIAL_MATERIAL_ADDED)
                .build()).build();
    }

    private String prepareAddNowFinancialDocumentRequestPayload() {
        String body = getPayload("progression.add-financial-now-document-request.json");
        body = body.replace(HEARING_ID, hearingId)
                .replace("%MATERIAL_ID%", materialId)
                .replace("%REQUEST_ID%", requestId)
                .replace("%DEFENDANT_ID%", defendantId);
        return body;
    }

    private String prepareAddNowNonFinancialDocumentRequestPayload(final String filename) {
        String body = getPayload(filename);
        body = body.replace(HEARING_ID, hearingId)
                .replace("%MATERIAL_ID%", materialId)
                .replace("%DEFENDANT_ID%", defendantId);
        return body;
    }

    private String prepareEmailDocumentRequestPayload() {
        String body = getPayload("progression.add-email-now-document-request.json");
        body = body.replace(HEARING_ID, hearingId)
                .replace("%MATERIAL_ID%", materialId)
                .replace("%DEFENDANT_ID%", defendantId);
        return body;
    }

    private String prepareApiNotificationDocumentRequestPayload() {
        String body = getPayload("progression.add-api-notification-now-document-request.json");
        body = body.replace(HEARING_ID, hearingId)
                .replace("%MATERIAL_ID%", materialId)
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%CASE_ID%", caseId1);
        return body;
    }

    private String prepareApiNotificationDocumentRequestPayloadForMultipleCases() {
        String body = getPayload("progression.add-api-notification-now-document-request-multiple.json");
        body = body.replace(HEARING_ID, hearingId)
                .replace("%MATERIAL_ID%", materialId)
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%CASE_ID1%", caseId1)
                .replace("%CASE_ID2%", caseId2);
        return body;
    }

    private String prepareNoOrderAddresseeDocumentRequestPayload() {
        String body = getPayload("progression.no-order-addressee-now-document-request.json");
        body = body.replace(HEARING_ID, hearingId)
                .replace("%MATERIAL_ID%", materialId)
                .replace("%DEFENDANT_ID%", defendantId);
        return body;
    }

    private JsonObject generateSuccessfulAcknowledgement(final String requestId) {

        final String EXPORT_STATUS = "exportStatus";
        final String ORIGINATOR = "originator";
        final String REQUEST_ID = "requestId";
        final String UPDATED = "updated";
        final String ACCOUNT_NUMBER = "accountNumber";
        final String ACKNOWLEDGEMENT = "acknowledgement";

        final JsonObjectBuilder builder = createObjectBuilder()
                .add(ORIGINATOR, "Courts")
                .add(REQUEST_ID, requestId)
                .add(EXPORT_STATUS, "ENFORCEMENT_ACKNOWLEDGED")
                .add(UPDATED, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + "Z");

        final JsonObjectBuilder acknowledgement = createObjectBuilder();
        acknowledgement.add(ACCOUNT_NUMBER, generateAccountNumber());

        builder.add(ACKNOWLEDGEMENT, acknowledgement);

        return builder.build();
    }

    private String generateAccountNumber() {
        return randomNumeric(8) + randomAlphabetic(1).toUpperCase();
    }

    private static String getNowDocumentRequestsFor(final String requestId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(join("", "/nows/request/", requestId)),
                "application/vnd.progression.query.now-document-requests-by-request-id+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    private String getNowDocumentRequest(final String hearingId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(StringUtils.join("/nows/hearing/", hearingId)),
                "application/vnd.progression.query.now-document-request-by-hearing+json")
                .withHeader(HeaderConstants.USER_ID, UUID.randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(javax.ws.rs.core.Response.Status.OK),
                        payload().isJson(allOf(matchers))).getPayload();
    }

    private static void verifyInMessagingQueue(final JmsMessageConsumerClient consumer) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageBody(consumer);
        assertTrue(message.isPresent());
    }
}
