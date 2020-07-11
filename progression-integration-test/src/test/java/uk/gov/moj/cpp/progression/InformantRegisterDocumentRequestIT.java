package uk.gov.moj.cpp.progression;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.helper.InformantRegisterDocumentRequestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.jms.MessageProducer;

import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class InformantRegisterDocumentRequestIT extends AbstractIT {
    private MessageProducer producer;
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Before
    public void setup() {
        producer = QueueUtil.publicEvents.createProducer();
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
    public void shouldGenerateInformantRegistersByDateAndProsecutionAuthorities() throws IOException {
        final UUID prosecutionAuthorityId_1 = randomUUID();
        final UUID prosecutionAuthorityId_2 = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.now(UTC);
        final ZonedDateTime hearingDate = ZonedDateTime.now(UTC).minusHours(1);
        final String prosecutionAuthorityCode_1 = STRING.next();
        final String prosecutionAuthorityCode_2 = STRING.next();
        final String fileName = "progression.add-informant-register-document-request.json";

        final InformantRegisterDocumentRequestHelper helper = new InformantRegisterDocumentRequestHelper();

        recordInformantRegister(prosecutionAuthorityId_1, prosecutionAuthorityCode_1, registerDate, hearingId, hearingDate, fileName);
        recordInformantRegister(prosecutionAuthorityId_2, prosecutionAuthorityCode_2, registerDate, hearingId, hearingDate, fileName);

        helper.verifyInformantRegisterDocumentRequestRecordedPrivateTopic(prosecutionAuthorityId_1.toString());
        helper.verifyInformantRegisterDocumentRequestRecordedPrivateTopic(prosecutionAuthorityId_2.toString());

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId_1);
        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId_2);

        generateInformantRegister();
        helper.verifyInformantRegisterDocumentRequestNotifiedPrivateTopic(prosecutionAuthorityId_1.toString());

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

        return postCommand(getWriteUrl(String.format("/informant-register")),
                "application/vnd.progression.add-informant-register+json",
                body);
    }
}
