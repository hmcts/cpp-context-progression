package uk.gov.moj.cpp.progression;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.Cleaner.closeSilently;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.helper.CourtRegisterDocumentRequestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.SysDocGeneratorStub;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CourtRegisterDocumentRequestIT extends AbstractIT {

    private StringToJsonObjectConverter stringToJsonObjectConverter;

    private MessageProducer producer;

    @Before
    public void setup() {
        stringToJsonObjectConverter = new StringToJsonObjectConverter();
        SysDocGeneratorStub.stubDocGeneratorEndPoint();
        producer = QueueUtil.publicEvents.createPublicProducer();
    }

    @After
    public void tearDown() throws JMSException {
        closeSilently(producer);
    }

    @Test
    public void shouldCreateCourtRegisterDocumentRequest() throws IOException {
        final UUID courtCentreId = randomUUID();
        final String courtHouse = STRING.next();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.now(UTC);
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);

        final Response writeResponse = recordCourtRegister(courtCentreId, courtHouse, registerDate, hearingId, hearingDate);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final CourtRegisterDocumentRequestHelper courtRegisterDocumentRequestHelper = new CourtRegisterDocumentRequestHelper();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterDocumentRequestRecordedPrivateTopic(courtCentreId.toString());
        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId);

        generateCourtRegister();
        final JsonObject response = stringToJsonObjectConverter.convert(courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId));

        final Optional<JsonObject> courtRegister = response.getJsonArray("courtRegisterDocumentRequests").getValuesAs(JsonObject.class)
                .stream().filter(cr -> cr.getString("courtCentreId").equals(courtCentreId.toString()))
                .findFirst();
        assertThat(courtRegister.isPresent(), is(true));
        SysDocGeneratorStub.pollSysDocGenerationRequests(Matchers.hasSize(1));
        courtRegisterDocumentRequestHelper.sendSystemDocGeneratorPublicEvent(USER_ID_VALUE_AS_ADMIN, courtCentreId);
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsNotified(courtCentreId);
    }


    @Test
    public void shouldGenerateCourtRegisterByDateAndCourtHouses() throws IOException {

        final UUID courtCentreId_1 = randomUUID();
        final UUID courtCentreId_2 = randomUUID();
        final UUID hearingId_1 = randomUUID();
        final UUID hearingId_2 = randomUUID();
        final ZonedDateTime registerDate_1 = ZonedDateTime.now(UTC);
        final ZonedDateTime registerDate_2 = ZonedDateTime.now(UTC).minusMinutes(2);
        final ZonedDateTime hearingDate_1 = ZonedDateTime.now(UTC).minusHours(1);
        final ZonedDateTime hearingDate_2 = ZonedDateTime.now(UTC).minusHours(2);
        final String courtHouse_1 = STRING.next();
        final String courtHouse_2 = STRING.next();

        recordCourtRegister(courtCentreId_1, courtHouse_1, registerDate_1, hearingId_1, hearingDate_1);
        recordCourtRegister(courtCentreId_2, courtHouse_2, registerDate_2, hearingId_2, hearingDate_2);

        CourtRegisterDocumentRequestHelper courtRegisterDocumentRequestHelper = new CourtRegisterDocumentRequestHelper();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId_1);
        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId_2);

        generateCourtRegister();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId_1);
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId_2);

        final String body = getPayload("progression.generate-court-register-by-date-and-court-house.json")
                .replaceAll("%REGISTER_DATE%", LocalDate.now().toString())
                .replaceAll("%COURT_HOUSE%", courtHouse_1 + "," + courtHouse_2);

        final Response generateRegisterResponse = postCommand(
                getWriteUrl("/court-register/generate"),
                "application/vnd.progression.generate-court-register-by-date+json",
                body);
        assertThat(generateRegisterResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final JsonObject response = stringToJsonObjectConverter.convert(courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId_1));

        final Optional<JsonObject> courtRegister1 = response.getJsonArray("courtRegisterDocumentRequests").getValuesAs(JsonObject.class)
                .stream().filter(cr -> cr.getString("courtCentreId").equals(courtCentreId_1.toString()))
                .findFirst();

        final Optional<JsonObject> courtRegister2 = response.getJsonArray("courtRegisterDocumentRequests").getValuesAs(JsonObject.class)
                .stream().filter(cr -> cr.getString("courtCentreId").equals(courtCentreId_1.toString()))
                .findFirst();

        assertThat(courtRegister1.isPresent(), is(true));
        assertThat(courtRegister2.isPresent(), is(true));
        SysDocGeneratorStub.pollSysDocGenerationRequests(Matchers.hasSize(1));
    }

    @Test
    public void shouldGenerateCourtRegisterOnlyByRequestDate() throws IOException {
        CourtRegisterDocumentRequestHelper courtRegisterDocumentRequestHelper = new CourtRegisterDocumentRequestHelper();
        final UUID courtCentreId = randomUUID();
        final String courtHouse = STRING.next();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.now(UTC);
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);

        recordCourtRegister(courtCentreId, courtHouse, registerDate, hearingId, hearingDate);

        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId);

        generateCourtRegister();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId);

        final String body = getPayload("progression.generate-court-register-by-date.json")
                .replaceAll("%REGISTER_DATE%", LocalDate.now().toString());

        final Response generateRegisterResponse = postCommand(
                getWriteUrl("/court-register/generate"),
                "application/vnd.progression.generate-court-register-by-date+json",
                body);
        assertThat(generateRegisterResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final JsonObject response = stringToJsonObjectConverter.convert(courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId));

        final Optional<JsonObject> courtRegister = response.getJsonArray("courtRegisterDocumentRequests").getValuesAs(JsonObject.class)
                .stream().filter(cr -> cr.getString("courtCentreId").equals(courtCentreId.toString()))
                .findFirst();
        assertThat(courtRegister.isPresent(), is(true));
        SysDocGeneratorStub.pollSysDocGenerationRequests(Matchers.hasSize(1));
    }

    @Test
    public void shouldGenerateCourtRegisterForLatestHearingSharedRequest() throws IOException {
        CourtRegisterDocumentRequestHelper courtRegisterDocumentRequestHelper = new CourtRegisterDocumentRequestHelper();
        final UUID courtCentreId = randomUUID();
        final String courtHouse = STRING.next();

        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);

        final ZonedDateTime registerDate1 = ZonedDateTime.now(UTC).minusMinutes(3);

        final Response writeResponse1 = recordCourtRegister(courtCentreId, courtHouse, registerDate1, hearingId, hearingDate);

        assertThat(writeResponse1.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final ZonedDateTime registerDate2 = ZonedDateTime.now(UTC).minusMinutes(2);
        final Response writeResponse2 = recordCourtRegister(courtCentreId, courtHouse, registerDate2, hearingId, hearingDate);

        assertThat(writeResponse2.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final ZonedDateTime registerDate3 = ZonedDateTime.now(UTC).minusMinutes(1);
        final Response writeResponse3 = recordCourtRegister(courtCentreId, courtHouse, registerDate3, hearingId, hearingDate);

        assertThat(writeResponse3.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final Response writeResponse4 = recordCourtRegister(courtCentreId, courtHouse, registerDate1, hearingId, hearingDate);

        assertThat(writeResponse4.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId);

        generateCourtRegister();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId);


        final ZonedDateTime registerDate4 = ZonedDateTime.now(UTC).minusMinutes(3);

        final Response writeResponse5 = recordCourtRegister(courtCentreId, courtHouse, registerDate4, hearingId, hearingDate);

        assertThat(writeResponse5.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId);
        generateCourtRegister();

        final JsonObject response = stringToJsonObjectConverter.convert(courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId));

        final Optional<JsonObject> courtRegister = response.getJsonArray("courtRegisterDocumentRequests").getValuesAs(JsonObject.class)
                .stream().filter(cr -> cr.getString("courtCentreId").equals(courtCentreId.toString()))
                .findFirst();
        assertThat(courtRegister.isPresent(), is(true));
        SysDocGeneratorStub.pollSysDocGenerationRequests(Matchers.hasSize(1));
        courtRegisterDocumentRequestHelper.sendSystemDocGeneratorPublicEvent(USER_ID_VALUE_AS_ADMIN, courtCentreId);
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsNotified(courtCentreId);

    }

    private Response recordCourtRegister(final UUID courtCentreId, final String courtHouse, final ZonedDateTime registerDate, final UUID hearingId, final ZonedDateTime hearingDate) throws IOException {
        final String body = getAggregateCourtRegisterDocumentRequestPayload(courtCentreId, courtHouse, registerDate, hearingId, hearingDate);

        return postCommand(
                getWriteUrl("/court-register"),
                "application/vnd.progression.add-court-register+json",
                body);
    }

    private void generateCourtRegister() throws IOException {
        final Response generateRegisterResponse = postCommand(
                getWriteUrl("/court-register/generate"),
                "application/vnd.progression.generate-court-register+json",
                "");
        assertThat(generateRegisterResponse.getStatusCode(), equalTo(SC_ACCEPTED));
    }

    private String getAggregateCourtRegisterDocumentRequestPayload(final UUID courtCentreId, final String courtHouse, final ZonedDateTime registerDate, final UUID hearingId, final ZonedDateTime hearingDate) {
        String body = getPayload("progression.add-court-register.json");
        body = body.replaceAll("%COURT_CENTRE_ID%", courtCentreId.toString())
                .replaceAll("%COURT_HOUSE%", courtHouse)
                .replaceAll("%HEARING_DATE%", hearingDate.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%REGISTER_DATE%", registerDate.toString());
        return body;
    }
}
