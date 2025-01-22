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
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
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
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyCreateLetterRequested;
import static uk.gov.moj.cpp.progression.stub.SysDocGeneratorStub.pollSysDocGenerationRequestsForPrisonCourtRegister;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.stub.SysDocGeneratorStub;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NowDocumentRequestIT extends AbstractIT {

    private static final String NOW_DOCUMENT_REQUESTS = "nowDocumentRequests";
    private static final String MATERIAL_ID = "materialId";
    private static final String MATERIAL_MATERIAL_ADDED = "material.material-added";
    private static final String HEARING_ID = "%HEARING_ID%";
    private static final String ORIGINATOR = "originator";
    private static final String ORIGINATOR_VALUE = "court";
    private static int counter = 1;
    private String materialId;
    private String hearingId;
    private String defendantId;
    private String requestId;
    private UUID userId;
    final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    @BeforeAll
    public static void setupBefore() {
        SysDocGeneratorStub.stubDocGeneratorEndPoint();
        NotificationServiceStub.setUp();
    }

    @BeforeEach
    public void setup() {
        hearingId = randomUUID().toString();
        defendantId = randomUUID().toString();
        materialId = randomUUID().toString();
        requestId = randomUUID().toString();
        userId = randomUUID();
    }

    @Test
    public void shouldAddFinancialNowDocumentRequest() throws IOException, JSONException {
        final String body = prepareAddNowFinancialDocumentRequestPayload();

        requestNowsDocument(body);

        getNowDocumentRequestsFor(requestId, anyOf(
                withJsonPath("$.nowDocumentRequests[0].requestId", equalTo(requestId)),
                withJsonPath("$.nowDocumentRequests[0].hearingId", equalTo(hearingId)),
                withJsonPath("$.nowDocumentRequests[0].materialId", equalTo(materialId)))
        );

        sendPublicEventForFinancialImpositionAcknowledgement();

        sendPublicEventForDocumentAvailable();

        verifyMaterialCreated(materialId);

        sendPublicEventForMaterialAdded();

        verifyCreateLetterRequested(of("letterUrl", materialId));
    }

    @Test
    public void shouldAddNonFinancialNowDocumentRequest() throws IOException, JSONException {

        final String payload = prepareAddNowNonFinancialDocumentRequestPayload("progression.add-non-financial-now-document-request.json");
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(payload);
        final NowDocumentRequest nowDocumentRequest = jsonToObjectConverter.convert(jsonObject, NowDocumentRequest.class);

        requestNowsDocument(payload);

        sendPublicEventForDocumentAvailable();

        verifyMaterialCreated(materialId);

        sendPublicEventForMaterialAdded();

        verifyCreateLetterRequested(of("letterUrl", materialId));

        final String nowDocumentRequestPayload = getNowDocumentRequest(hearingId,
                anyOf(withJsonPath("$.nowDocumentRequests[0].hearingId", equalTo(hearingId))));
        final JsonObject nowDocumentRequests = stringToJsonObjectConverter.convert(nowDocumentRequestPayload);
        final JsonObject nowDocumentRequestJsonObject = nowDocumentRequests.getJsonArray(NOW_DOCUMENT_REQUESTS).getJsonObject(0);
        assertThat(nowDocumentRequest.getMaterialId().toString(), is(nowDocumentRequestJsonObject.getString(MATERIAL_ID)));
    }

    private void requestNowsDocument(final String body) throws IOException {
        final Response writeResponse = postCommand(getWriteUrl("/nows"),
                "application/vnd.progression.add-now-document-request+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
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

    private void sendPublicEventForDocumentAvailable() throws JSONException {
        System.out.println(counter);
        final List<JSONObject> jsonObjects = pollSysDocGenerationRequestsForPrisonCourtRegister(
                Matchers.hasSize(counter), "NOWs");

        final UUID payloadFileServiceId = fromString(jsonObjects.get(counter-1).getString("payloadFileServiceId"));

        final String commandName = "public.systemdocgenerator.events.document-available";

        final Metadata metadata = getMetadataFrom(userId.toString(), fromString(materialId), commandName);

        messageProducerClientPublic.sendMessage(commandName, JsonEnvelope.envelopeFrom(metadata, documentAvailablePayload(payloadFileServiceId, "OPE_Layout16", materialId.toString(), randomUUID())));
        counter++;
    }

    private Metadata getMetadataFrom(final String userId, final UUID materialId, final String name) {
        return metadataFrom(createObjectBuilder()
                .add(ORIGINATOR, materialId.toString())
                .add(ID, randomUUID().toString())
                .add(HeaderConstants.USER_ID, userId)
                .add(NAME, name)
                .build()).build();
    }

    private JsonObject documentAvailablePayload(final UUID payloadFileServiceId, final String templateIdentifier,
                                                final String reportId, final UUID generatedDocumentId) {
        return createObjectBuilder()
                .add("payloadFileServiceId", payloadFileServiceId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", "NOWs")
                .add("documentFileServiceId", generatedDocumentId.toString())
                .add("generatedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("generateVersion", 1)
                .build();
    }

}
