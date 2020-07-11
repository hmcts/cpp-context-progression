package uk.gov.moj.cpp.progression;

import com.jayway.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.helper.CourtRegisterDocumentRequestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.SysDocGeneratorStub;

import javax.jms.MessageProducer;
import javax.json.JsonObject;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

public class CourtRegisterDocumentRequestIT extends AbstractIT {

    private String DOCUMENT_TEXT = "court register pdf content";

    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private MessageProducer producer;

    @Before
    public void setup() {
        SysDocGeneratorStub.stubDocGeneratorEndPoint();
        producer = QueueUtil.publicEvents.createProducer();
    }

    @Test
    public void shouldCreateCourtRegisterDocumentRequest() throws IOException {
        final UUID courtCentreId = randomUUID();
        final String courtHouse = STRING.next();
        final CourtRegisterDocumentRequestHelper courtRegisterDocumentRequestHelper = new CourtRegisterDocumentRequestHelper();

        final Response writeResponse = recordCourtRegister(courtCentreId, courtHouse);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
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
        CourtRegisterDocumentRequestHelper courtRegisterDocumentRequestHelper = new CourtRegisterDocumentRequestHelper();
        final UUID courtCentreId_1 = randomUUID();
        final UUID courtCentreId_2 = randomUUID();
        final String courtHouse_1 = STRING.next();
        final String courtHouse_2 = STRING.next();

        recordCourtRegister(courtCentreId_1, courtHouse_1);
        recordCourtRegister(courtCentreId_2, courtHouse_2);


        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId_1);
        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId_2);

        generateCourtRegister();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId_1);

        final String body = getPayload("progression.generate-court-register-by-date-and-court-house.json")
                .replaceAll("%REGISTER_DATE%", LocalDate.now().toString())
                .replaceAll("%COURT_HOUSE%", courtHouse_1 + "," + courtHouse_2);

        final Response generateRegisterResponse = postCommand(
                getWriteUrl("/court-register/generate"),
                "application/vnd.progression.generate-court-register-by-date+json",
                body);
        assertThat(generateRegisterResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final JsonObject response = stringToJsonObjectConverter.convert(courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId_1));

        final Optional<JsonObject> courtRegister = response.getJsonArray("courtRegisterDocumentRequests").getValuesAs(JsonObject.class)
                .stream().filter(cr -> cr.getString("courtCentreId").equals(courtCentreId_1.toString()))
                .findFirst();

        assertThat(courtRegister.isPresent(), is(true));
        SysDocGeneratorStub.pollSysDocGenerationRequests(Matchers.hasSize(1));
    }

    @Test
    public void shouldGenerateCourtRegisterOnlyByRequestDate() throws IOException {
        CourtRegisterDocumentRequestHelper courtRegisterDocumentRequestHelper = new CourtRegisterDocumentRequestHelper();
        final UUID courtCentreId = randomUUID();
        final String courtHouse = STRING.next();

        recordCourtRegister(courtCentreId, courtHouse);

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

    private Response recordCourtRegister(final UUID courtCentreId, final String courtHouse) throws IOException {
        final String body = getAggregateCourtRegisterDocumentRequestPayload(courtCentreId, courtHouse);

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

    private String getAggregateCourtRegisterDocumentRequestPayload(final UUID courtCentreId, final String courtHouse) {
        String body = getPayload("progression.add-court-register.json");
        body = body.replaceAll("%COURT_CENTRE_ID%", courtCentreId.toString())
                .replaceAll("%COURT_HOUSE%", courtHouse)
                .replaceAll("%HEARING_DATE%", ZonedDateTime.now(UTC).toString())
                .replaceAll("%HEARING_ID%", randomUUID().toString())
                .replaceAll("%REGISTER_DATE%", ZonedDateTime.now(UTC).toString());
        return body;
    }
}
