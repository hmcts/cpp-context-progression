package uk.gov.moj.cpp.progression;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.domain.helper.CourtRegisterHelper.getCourtRegisterStreamId;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.SysDocGeneratorStub.pollSysDocGenerationRequests;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.CourtRegisterDocumentRequestHelper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class CourtRegisterDocumentRequestIT extends AbstractIT {

    private final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.court-application-proceedings-initiated").getMessageConsumerClient();

    @Test
    public void shouldCreateCourtRegisterDocumentRequest() throws IOException {
        final UUID courtCentreId = randomUUID();
        final String courtHouse = STRING.next();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.now(UTC);
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);

        final UUID courtApplicationId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        intiateCourtProceedingForApplication(courtApplicationId.toString(), caseId.toString(), defendantId.toString(), defendantId.toString(), hearingId.toString(), "applications/progression.initiate-court-proceedings-for-application_for_prison_court_register.json");
        verifyCourtApplicationCreatedPublicEvent();

        final Response writeResponse = recordCourtRegister(courtCentreId, courtHouse, registerDate, hearingId, hearingDate, courtApplicationId);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final CourtRegisterDocumentRequestHelper courtRegisterDocumentRequestHelper = new CourtRegisterDocumentRequestHelper();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId);

        generateCourtRegister();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId);

        pollSysDocGenerationRequests(hasSize(1));

        final UUID courtCentreStreamId = getCourtRegisterStreamId(courtCentreId.toString(), registerDate.toLocalDate().toString());
        courtRegisterDocumentRequestHelper.sendSystemDocGeneratorPublicEvent(USER_ID_VALUE_AS_ADMIN, courtCentreStreamId);
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsNotified(courtCentreId);
    }


    @Test
    public void shouldGenerateCourtRegisterByDateAndCourtHouses() throws IOException {

        final UUID courtCentreId_1 = randomUUID();
        final UUID courtCentreId_2 = randomUUID();
        final UUID hearingId_1 = randomUUID();
        final UUID hearingId_2 = randomUUID();
        final UUID courtApplicationId1 = randomUUID();
        final UUID courtApplicationId2 = randomUUID();

        final ZonedDateTime registerDate_1 = ZonedDateTime.now(UTC);
        final ZonedDateTime registerDate_2 = ZonedDateTime.now(UTC).minusMinutes(2);
        final ZonedDateTime hearingDate_1 = ZonedDateTime.now(UTC).minusHours(1);
        final ZonedDateTime hearingDate_2 = ZonedDateTime.now(UTC).minusHours(2);
        final String courtHouse_1 = STRING.next();
        final String courtHouse_2 = STRING.next();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final UUID defendantId = randomUUID();

        intiateCourtProceedingForApplication(courtApplicationId1.toString(), caseId1.toString(), defendantId.toString(), defendantId.toString(), hearingId_1.toString(), "applications/progression.initiate-court-proceedings-for-application_for_prison_court_register.json");
        verifyCourtApplicationCreatedPublicEvent();
        recordCourtRegister(courtCentreId_1, courtHouse_1, registerDate_1, hearingId_1, hearingDate_1, courtApplicationId1);

        intiateCourtProceedingForApplication(courtApplicationId2.toString(), caseId2.toString(), defendantId.toString(), defendantId.toString(), hearingId_2.toString(), "applications/progression.initiate-court-proceedings-for-application_for_prison_court_register.json");
        verifyCourtApplicationCreatedPublicEvent();
        recordCourtRegister(courtCentreId_2, courtHouse_2, registerDate_2, hearingId_2, hearingDate_2, courtApplicationId2);

        CourtRegisterDocumentRequestHelper courtRegisterDocumentRequestHelper = new CourtRegisterDocumentRequestHelper();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId_1, courtCentreId_2);

        generateCourtRegister();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId_1, courtCentreId_2);

        final String body = getPayload("progression.generate-court-register-by-date-and-court-house.json")
                .replaceAll("%REGISTER_DATE%", LocalDate.now().toString())
                .replaceAll("%COURT_HOUSE%", courtHouse_1 + "," + courtHouse_2);

        final Response generateRegisterResponse = postCommand(
                getWriteUrl("/court-register/generate"),
                "application/vnd.progression.generate-court-register-by-date+json",
                body);
        assertThat(generateRegisterResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        pollSysDocGenerationRequests(hasSize(1));
    }

    @Test
    public void shouldGenerateCourtRegisterOnlyByRequestDate() throws IOException {
        CourtRegisterDocumentRequestHelper courtRegisterDocumentRequestHelper = new CourtRegisterDocumentRequestHelper();
        final UUID courtCentreId = randomUUID();
        final String courtHouse = STRING.next();
        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.now(UTC);
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        intiateCourtProceedingForApplication(courtApplicationId.toString(), caseId.toString(), defendantId.toString(), defendantId.toString(), hearingId.toString(), "applications/progression.initiate-court-proceedings-for-application_for_prison_court_register.json");
        verifyCourtApplicationCreatedPublicEvent();
        recordCourtRegister(courtCentreId, courtHouse, registerDate, hearingId, hearingDate, courtApplicationId);

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

        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId);

        pollSysDocGenerationRequests(hasSize(1));
    }

    @Test
    public void shouldGenerateCourtRegisterForLatestHearingSharedRequest() throws IOException {
        CourtRegisterDocumentRequestHelper courtRegisterDocumentRequestHelper = new CourtRegisterDocumentRequestHelper();
        final UUID courtCentreId = randomUUID();
        final String courtHouse = STRING.next();
        final UUID courtApplicationId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);

        final ZonedDateTime registerDate1 = ZonedDateTime.now(UTC).minusMinutes(3);

        intiateCourtProceedingForApplication(courtApplicationId.toString(), caseId.toString(), defendantId.toString(), defendantId.toString(), hearingId.toString(), "applications/progression.initiate-court-proceedings-for-application_for_prison_court_register.json");
        verifyCourtApplicationCreatedPublicEvent();

        final Response writeResponse1 = recordCourtRegister(courtCentreId, courtHouse, registerDate1, hearingId, hearingDate, courtApplicationId);
        assertThat(writeResponse1.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final ZonedDateTime registerDate2 = ZonedDateTime.now(UTC).minusMinutes(2);
        final Response writeResponse2 = recordCourtRegister(courtCentreId, courtHouse, registerDate2, hearingId, hearingDate, courtApplicationId);
        assertThat(writeResponse2.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final ZonedDateTime registerDate3 = ZonedDateTime.now(UTC).minusMinutes(1);
        final Response writeResponse3 = recordCourtRegister(courtCentreId, courtHouse, registerDate3, hearingId, hearingDate, courtApplicationId);
        assertThat(writeResponse3.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final Response writeResponse4 = recordCourtRegister(courtCentreId, courtHouse, registerDate1, hearingId, hearingDate, courtApplicationId);
        assertThat(writeResponse4.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId);

        generateCourtRegister();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId);


        final ZonedDateTime registerDate4 = ZonedDateTime.now(UTC).minusMinutes(3);
        final Response writeResponse5 = recordCourtRegister(courtCentreId, courtHouse, registerDate4, hearingId, hearingDate, courtApplicationId);
        assertThat(writeResponse5.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        courtRegisterDocumentRequestHelper.verifyCourtRegisterRequestsExists(courtCentreId);
        generateCourtRegister();
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsGenerated(courtCentreId);

        pollSysDocGenerationRequests(hasSize(1));
        courtRegisterDocumentRequestHelper.sendSystemDocGeneratorPublicEvent(USER_ID_VALUE_AS_ADMIN, getCourtRegisterStreamId(courtCentreId.toString(), registerDate4.toLocalDate().toString()));
        courtRegisterDocumentRequestHelper.verifyCourtRegisterIsNotified(courtCentreId);

    }

    private Response recordCourtRegister(final UUID courtCentreId, final String courtHouse, final ZonedDateTime registerDate, final UUID hearingId, final ZonedDateTime hearingDate,
                                         final UUID courtApplicationId) {
        final String body = getAggregateCourtRegisterDocumentRequestPayload(courtCentreId, courtHouse, registerDate, hearingId, hearingDate, courtApplicationId);

        return postCommand(
                getWriteUrl("/court-register"),
                "application/vnd.progression.add-court-register+json",
                body);
    }

    private void generateCourtRegister() {
        final Response generateRegisterResponse = postCommand(
                getWriteUrl("/court-register/generate"),
                "application/vnd.progression.generate-court-register+json",
                "");
        assertThat(generateRegisterResponse.getStatusCode(), equalTo(SC_ACCEPTED));
    }

    private String getAggregateCourtRegisterDocumentRequestPayload(final UUID courtCentreId, final String courtHouse, final ZonedDateTime registerDate, final UUID hearingId, final ZonedDateTime hearingDate,
                                                                   final UUID courtApplicationId) {
        String body = getPayload("progression.add-court-register.json");
        body = body.replaceAll("%COURT_CENTRE_ID%", courtCentreId.toString())
                .replaceAll("%COURT_HOUSE%", courtHouse)
                .replaceAll("%HEARING_DATE%", hearingDate.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%COURT_APPLICATION_ID%", courtApplicationId.toString())
                .replaceAll("%REGISTER_DATE%", registerDate.toString());
        return body;
    }

    private void verifyCourtApplicationCreatedPublicEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(applicationReference, is(notNullValue()));
    }
}
