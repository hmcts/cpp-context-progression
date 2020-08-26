package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.join;
import static java.util.Collections.singletonList;
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
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
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
import static uk.gov.moj.cpp.progression.helper.Cleaner.closeSilently;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyCreateLetterRequested;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.JsonMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.jayway.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NowDocumentRequestIT extends AbstractIT {

    private String materialId;
    private String hearingId;
    private String defendantId;
    private String requestId;
    private UUID userId;

    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String ORIGINATOR = "originator";
    private static final String ORIGINATOR_VALUE = "court";
    private MessageProducer messageProducerClientPublic;
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Before
    public void setup() {
        messageProducerClientPublic = publicEvents.createProducer();
        hearingId = randomUUID().toString();
        defendantId = randomUUID().toString();
        materialId = randomUUID().toString();
        requestId = randomUUID().toString();
        userId = randomUUID();
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @After
    public void tearDown() throws JMSException {
        closeSilently(messageProducerClientPublic);
    }

    @Test
    public void shouldAddFinancialNowDocumentRequest() throws IOException {
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        NotificationServiceStub.setUp();

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

        verifyCreateLetterRequested(singletonList("letterUrl"));
    }

    @Test
    public void shouldAddNonFinancialNowDocumentRequest() throws IOException {
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        NotificationServiceStub.setUp();
        IdMapperStub.setUp();

        final String payload = prepareAddNowNonFinancialDocumentRequestPayload();
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

        verifyCreateLetterRequested(Arrays.asList("letterUrl"));

        final JsonObject nowDocumentRequests = stringToJsonObjectConverter.convert(nowDocumentRequestPayload);
        final JsonObject nowDocumentRequestJsonObject = nowDocumentRequests.getJsonArray("nowDocumentRequests").getJsonObject(0);
        assertThat(nowDocumentRequest.getMaterialId().toString(), is(nowDocumentRequestJsonObject.getString("materialId")));
    }

    @Test
    public void shouldEmailNowDocumentRequest() throws IOException {
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        NotificationServiceStub.setUp();
        IdMapperStub.setUp();

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
        final JsonObject nowDocumentRequestJsonObject = nowDocumentRequests.getJsonArray("nowDocumentRequests").getJsonObject(0);
        assertThat(nowDocumentRequest.getMaterialId().toString(), is(nowDocumentRequestJsonObject.getString("materialId")));
    }

    @Test
    public void shouldNotSendEmailWithNoOrderAddresseeInNowDocumentRequest() throws IOException {
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        NotificationServiceStub.setUp();
        IdMapperStub.setUp();

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
        final JsonObject nowDocumentRequestJsonObject = nowDocumentRequests.getJsonArray("nowDocumentRequests").getJsonObject(0);
        assertThat(nowDocumentRequest.getMaterialId().toString(), is(nowDocumentRequestJsonObject.getString("materialId")));
    }

    private void sendMaterialFileUploadedPublicEvent(final UUID materialId, final UUID userId) {
        final String commandName = "material.material-added";
        final Metadata metadata = getMetadataFrom(userId.toString());
        final JsonObject payload = createObjectBuilder().add("materialId", materialId.toString()).add(
                "fileDetails",
                createObjectBuilder().add("alfrescoAssetId", "aGVsbG8=")
                        .add("mimeType", "text/plain").add("fileName", "file.txt"))
                .add("materialAddedDate", "2016-04-26T13:01:787.345").build();
        sendMessage(messageProducerClientPublic, commandName, payload, metadata);
    }

    private void sendPublicEventForMaterialAdded() {
        final JsonObject materialAddPublicEventPayload = Json.createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("fileServiceId", randomUUID().toString())
                .build();

        final Metadata metadata = metadataFrom(createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(NAME, "material.material-added")
                .add(ORIGINATOR, ORIGINATOR_VALUE)
                .add("context", createObjectBuilder()
                        .add(JsonMetadata.USER_ID, userId.toString()))
                .build()).build();

        sendMessage(publicEvents.createProducer(),
                "material.material-added",
                materialAddPublicEventPayload,
                metadata);
    }

    private void sendPublicEventForFinancialImpositionAcknowledgement() {
        final String eventName = "public.stagingenforcement.enforce-financial-imposition-acknowledgement";

        final JsonObject payload = generateSuccessfulAcknowledgement(requestId);

        sendMessage(publicEvents.createProducer(),
                eventName,
                payload,
                metadataOf(randomUUID(), eventName)
                        .withUserId(randomUUID().toString())
                        .build());
    }

    private Metadata getMetadataFrom(final String userId) {
        return metadataFrom(createObjectBuilder()
                .add(ORIGINATOR, ORIGINATOR_VALUE)
                .add(ID, randomUUID().toString())
                .add(JsonMetadata.USER_ID, userId)
                .add(NAME, "material.material-added")
                .build()).build();
    }

    private String prepareAddNowFinancialDocumentRequestPayload() {
        String body = getPayload("progression.add-financial-now-document-request.json");
        body = body.replaceAll("%HEARING_ID%", hearingId)
                .replaceAll("%MATERIAL_ID%", materialId)
                .replaceAll("%REQUEST_ID%", requestId)
                .replaceAll("%DEFENDANT_ID%", defendantId);
        return body;
    }

    private String prepareAddNowNonFinancialDocumentRequestPayload() {
        String body = getPayload("progression.add-non-financial-now-document-request.json");
        body = body.replaceAll("%HEARING_ID%", hearingId)
                .replaceAll("%MATERIAL_ID%", materialId)
                .replaceAll("%DEFENDANT_ID%", defendantId);
        return body;
    }

    private String prepareEmailDocumentRequestPayload() {
        String body = getPayload("progression.add-email-now-document-request.json");
        body = body.replaceAll("%HEARING_ID%", hearingId)
                .replaceAll("%MATERIAL_ID%", materialId)
                .replaceAll("%DEFENDANT_ID%", defendantId);
        return body;
    }

    private String prepareNoOrderAddresseeDocumentRequestPayload() {
        String body = getPayload("progression.no-order-addressee-now-document-request.json");
        body = body.replaceAll("%HEARING_ID%", hearingId)
                .replaceAll("%MATERIAL_ID%", materialId)
                .replaceAll("%DEFENDANT_ID%", defendantId);
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
                .withHeader(USER_ID, UUID.randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    private String getNowDocumentRequest(final String hearingId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(StringUtils.join("/nows/hearing/", hearingId)),
                "application/vnd.progression.query.now-document-request-by-hearing+json")
                .withHeader(HeaderConstants.USER_ID, USER_ID))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(javax.ws.rs.core.Response.Status.OK),
                        payload().isJson(allOf(matchers))).getPayload();
    }
}