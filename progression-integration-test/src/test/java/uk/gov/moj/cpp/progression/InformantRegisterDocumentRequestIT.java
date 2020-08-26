package uk.gov.moj.cpp.progression;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.Cleaner.closeSilently;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.helper.InformantRegisterDocumentRequestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;

import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InformantRegisterDocumentRequestIT extends AbstractIT {
    private MessageProducer producer;

    @Before
    public void setup() {
        producer = QueueUtil.publicEvents.createProducer();
    }

    @After
    public void tearDown() throws JMSException {
        closeSilently(producer);
    }

    @Test
    public void shouldAddInformantRegisterRequest() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.now(UTC);
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);
        final String prosecutionAuthorityCode = STRING.next();

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();

        final Response writeResponse = recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, registerDate, hearingId, hearingDate, "progression.add-informant-register-document-request.json");
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        helper.verifyInformantRegisterDocumentRequestRecordedPrivateTopic(prosecutionAuthorityId.toString());
        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        generateInformantRegister();
        helper.verifyInformantRegisterDocumentRequestNotifiedPrivateTopic(prosecutionAuthorityId.toString());
        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId);
    }

    @Test
    public void shouldGenerateInformantRegisterForLatestHearingSharedRequest() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final String prosecutionAuthorityCode = STRING.next();

        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);

        final ZonedDateTime registerDate1 = ZonedDateTime.now(UTC).minusMinutes(3);

        final Response writeResponse1 = recordInformantRegister(prosecutionAuthorityId,
                prosecutionAuthorityCode, registerDate1, hearingId, hearingDate,
                "progression.add-informant-register-document-request.json");

        assertThat(writeResponse1.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final ZonedDateTime registerDate2 = ZonedDateTime.now(UTC).minusMinutes(2);
        final Response writeResponse2 = recordInformantRegister(prosecutionAuthorityId,
                prosecutionAuthorityCode, registerDate2, hearingId, hearingDate,
                "progression.add-informant-register-document-request.json");

        assertThat(writeResponse2.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final ZonedDateTime registerDate3 = ZonedDateTime.now(UTC).minusMinutes(1);
        final Response writeResponse3 = recordInformantRegister(prosecutionAuthorityId,
                prosecutionAuthorityCode, registerDate3, hearingId, hearingDate,
                "progression.add-informant-register-document-request.json");

        assertThat(writeResponse3.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final Response writeResponse4 = recordInformantRegister(prosecutionAuthorityId,
                prosecutionAuthorityCode, registerDate1, randomUUID(), hearingDate,
                "progression.add-informant-register-document-request.json");

        assertThat(writeResponse4.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();
        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        generateInformantRegister();

        helper.verifyInformantRegisterDocumentRequestNotifiedPrivateTopic(prosecutionAuthorityId.toString());
        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId);


        final ZonedDateTime registerDate5 = ZonedDateTime.now(UTC).minusMinutes(3);

        final Response writeResponse5 = recordInformantRegister(prosecutionAuthorityId,
                prosecutionAuthorityCode, registerDate5, hearingId, hearingDate,
                "progression.add-informant-register-document-request.json");

        assertThat(writeResponse5.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);
        generateInformantRegister();

        helper.verifyInformantRegisterDocumentRequestNotifiedPrivateTopic(prosecutionAuthorityId.toString());
        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId);

    }

    @Test
    public void shouldGenerateInformantRegistersByDateAndProsecutionAuthorities() throws IOException {
        final UUID prosecutionAuthorityId_1 = randomUUID();
        final UUID prosecutionAuthorityId_2 = randomUUID();
        final UUID hearingId_1 = randomUUID();
        final UUID hearingId_2 = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.now(UTC);
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);
        final String prosecutionAuthorityCode_1 = STRING.next();
        final String prosecutionAuthorityCode_2 = STRING.next();
        final String fileName = "progression.add-informant-register-document-request.json";

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();

        recordInformantRegister(prosecutionAuthorityId_1, prosecutionAuthorityCode_1, registerDate, hearingId_1, hearingDate, fileName);
        recordInformantRegister(prosecutionAuthorityId_2, prosecutionAuthorityCode_2, registerDate, hearingId_2, hearingDate, fileName);

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId_1);
        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId_2);

        generateInformantRegister();

        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId_1);
        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId_2);

        final String generateIRByDateCommandBody = getPayload("progression.generate-informant-register-by-date-and-prosecution.json")
                .replaceAll("%REGISTER_DATE%", registerDate.toLocalDate().toString())
                .replaceAll("%PROSECUTION_AUTHORITY_CODE%", prosecutionAuthorityCode_1 + "," + prosecutionAuthorityCode_2);

        final Response generateRegisterByDateAndProsecutionResponse = postCommand(
                getWriteUrl("/informant-register/generate"),
                "application/vnd.progression.generate-informant-register-by-date+json",
                generateIRByDateCommandBody);

        assertThat(generateRegisterByDateAndProsecutionResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId_1);
    }

    @Test
    public void shouldGenerateInformantRegistersOnlyByRequestDate() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.now(UTC);
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);
        final String prosecutionAuthorityCode = STRING.next();

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();

        recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, registerDate, hearingId, hearingDate, "progression.add-informant-register-document-request.json");
        helper.verifyInformantRegisterDocumentRequestRecordedPrivateTopic(prosecutionAuthorityId.toString());

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        generateInformantRegister();
        helper.verifyInformantRegisterDocumentRequestNotifiedPrivateTopic(prosecutionAuthorityId.toString());

        generateInformantRegisterByDate(registerDate.toLocalDate());
        helper.verifyInformantRegisterIsNotified(prosecutionAuthorityId);
    }

    @Test
    public void shouldNotSendInformantRegistersNotificationWithoutRecipients() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.now(UTC);
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);
        final String prosecutionAuthorityCode = STRING.next();

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();

        recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, registerDate, hearingId, hearingDate, "progression.add-informant-register-document-request-without-recipients.json");
        helper.verifyInformantRegisterDocumentRequestRecordedPrivateTopic(prosecutionAuthorityId.toString());

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        generateInformantRegister();
        helper.verifyInformantRegisterNotificationIgnoredPrivateTopic(prosecutionAuthorityId.toString());
    }

    @Test
    public void shouldNotSendInformantRegistersNotificationWithoutMatchingTemplate() throws IOException {
        final UUID prosecutionAuthorityId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.now(UTC);
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);
        final String prosecutionAuthorityCode = STRING.next();

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();

        recordInformantRegister(prosecutionAuthorityId, prosecutionAuthorityCode, registerDate, hearingId, hearingDate, "progression.add-informant-register-document-request-without-matching-template.json");
        helper.verifyInformantRegisterDocumentRequestRecordedPrivateTopic(prosecutionAuthorityId.toString());

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        generateInformantRegister();
        helper.verifyInformantRegisterNotificationIgnoredPrivateTopic(prosecutionAuthorityId.toString());
    }

    private void generateInformantRegister() throws IOException {
        final Response generateRegisterResponse = postCommand(
                getWriteUrl("/informant-register/generate"),
                "application/vnd.progression.generate-informant-register+json",
                "");
        assertThat(generateRegisterResponse.getStatusCode(), equalTo(SC_ACCEPTED));
    }

    private void generateInformantRegisterByDate(final LocalDate registerDate) throws IOException {
        final String generateCommandBody = getPayload("progression.generate-informant-register-by-date.json")
                .replaceAll("%REGISTER_DATE%", registerDate.toString());

        final Response generateRegisterResponse = postCommand(
                getWriteUrl("/informant-register/generate"),
                "application/vnd.progression.generate-informant-register-by-date+json",
                generateCommandBody);
        assertThat(generateRegisterResponse.getStatusCode(), equalTo(SC_ACCEPTED));
    }

    private Response recordInformantRegister(final UUID prosecutionAuthorityId, final String prosecutionAuthorityCode, final ZonedDateTime registerDate, final UUID hearingId, final ZonedDateTime hearingDate, final String fileName) throws IOException {
        final String body = getPayload(fileName)
                .replaceAll("%PROSECUTION_AUTHORITY_ID%", prosecutionAuthorityId.toString())
                .replaceAll("%PROSECUTION_AUTHORITY_CODE%", prosecutionAuthorityCode)
                .replaceAll("%REGISTER_DATE%", registerDate.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%HEARING_DATE%", hearingDate.toString());

        return postCommand(getWriteUrl("/informant-register"),
                "application/vnd.progression.add-informant-register+json",
                body);
    }
}
