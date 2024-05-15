package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_GENERATED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_RECORDED;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;


public class PrisonCourtRegisterDocumentRequestIT extends AbstractIT {
    private static final String DOCUMENT_TEXT = STRING.next();
    protected MessageConsumer privateEventsConsumer;
    protected MessageConsumer privateEventsConsumer2;
    private String courtCentreId;
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Before
    public void setup() {
        stringToJsonObjectConverter = new StringToJsonObjectConverter();
        courtCentreId = randomUUID().toString();
        privateEventsConsumer = QueueUtil.privateEvents.createPrivateConsumer(EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_RECORDED);
        privateEventsConsumer2 = QueueUtil.privateEvents.createPrivateConsumer(EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_GENERATED);
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        NotificationServiceStub.setUp();
    }

    @Test
    public void shouldAddPrisonCourtDocumentRequest() throws IOException {
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);
        final String body = getPayload("progression.prison-court-register-document-request.json")
                .replace("%COURT_CENTRE_ID%", courtCentreId)
                .replaceAll("%HEARING_DATE%", ZonedDateTime.now(UTC).toString())
                .replaceAll("%HEARING_ID%", hearingId.toString());

        final Response writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                "application/vnd.progression.add-prison-court-register+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.get("courtCentreId"), is(courtCentreId));

        final JsonPath jsonResponse2 = retrieveMessage(privateEventsConsumer2);
        assertThat(jsonResponse2.get("courtCentreId"), is(courtCentreId));
        assertThat(jsonResponse2.get("fileId"), is(notNullValue()));
        verifyPrisonCourtRegisterRequestsExists(UUID.fromString(courtCentreId), hearingId);
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

    private String getPrisonCourtRegisterDocumentRequests(final UUID courtCentreId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(StringUtils.join("/prison-court-register/request/", courtCentreId)),
                "application/vnd.progression.query.prison-court-register-document-by-court-centre+json")
                .withHeader(HeaderConstants.USER_ID, USER_ID))
                .timeout(60, TimeUnit.SECONDS)
                .until(
                        status().is(javax.ws.rs.core.Response.Status.OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }
}
