package uk.gov.moj.cpp.progression;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.helper.PrisonCourtRegisterDocumentRequestHelper;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.stub.SysDocGeneratorStub;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;


public class PrisonCourtRegisterDocumentRequestIT extends AbstractIT {

    @Before
    public void setup() {
        SysDocGeneratorStub.stubDocGeneratorEndPoint();
        NotificationServiceStub.setUp();
    }

    @Test
    public void shouldGeneratePrisonCourtDocumentAsynchronously() throws IOException {
        final UUID courtCentreId = randomUUID();

        try (PrisonCourtRegisterDocumentRequestHelper prisonCourtRegisterDocumentRequestHelper = new PrisonCourtRegisterDocumentRequestHelper()) {

            final UUID hearingId = randomUUID();
            final String body = getPayload("progression.prison-court-register-document-request.json")
                    .replace("%COURT_CENTRE_ID%", courtCentreId.toString())
                    .replaceAll("%HEARING_DATE%", ZonedDateTime.now(UTC).toString())
                    .replaceAll("%HEARING_ID%", hearingId.toString());

            final Response writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                    "application/vnd.progression.add-prison-court-register+json",
                    body);

            assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

            prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterDocumentRequestRecordedPrivateTopic(courtCentreId.toString());
            prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterRequestsExists(courtCentreId);

            final List<JSONObject> jsonObjects = SysDocGeneratorStub.pollSysDocGenerationRequestsForPrisonCourtRegister(Matchers.hasSize(1), "PRISON_COURT_REGISTER");
            final JSONObject jsonObject = jsonObjects.get(0);
            final UUID payloadFileServiceId = fromString(jsonObject.getString("payloadFileServiceId"));
            final UUID documentFileServiceId = randomUUID();
            prisonCourtRegisterDocumentRequestHelper.sendSystemDocGeneratorPublicAvailableEvent(USER_ID_VALUE_AS_ADMIN, courtCentreId, payloadFileServiceId, documentFileServiceId);
            prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterIsGenerated(courtCentreId, documentFileServiceId);
        }
    }

    @Test
    public void shouldFailedPrisonCourtDocumentAsynchronously() throws IOException {
        final UUID courtCentreId = randomUUID();

        try (PrisonCourtRegisterDocumentRequestHelper prisonCourtRegisterDocumentRequestHelper = new PrisonCourtRegisterDocumentRequestHelper()) {

            final UUID hearingId = randomUUID();
            final String body = getPayload("progression.prison-court-register-document-request.json")
                    .replace("%COURT_CENTRE_ID%", courtCentreId.toString())
                    .replaceAll("%HEARING_DATE%", ZonedDateTime.now(UTC).toString())
                    .replaceAll("%HEARING_ID%", hearingId.toString());

            final Response writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                    "application/vnd.progression.add-prison-court-register+json",
                    body);

            assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

            prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterDocumentRequestRecordedPrivateTopic(courtCentreId.toString());
            prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterRequestsExists(courtCentreId);

            final List<JSONObject> jsonObjects = SysDocGeneratorStub.pollSysDocGenerationRequestsForPrisonCourtRegister(Matchers.hasSize(2), "PRISON_COURT_REGISTER");
            final JSONObject jsonObject = jsonObjects.get(1);
            final UUID payloadFileServiceId = fromString(jsonObject.getString("payloadFileServiceId"));
            prisonCourtRegisterDocumentRequestHelper.sendSystemDocGeneratorPublicFailedEvent(USER_ID_VALUE_AS_ADMIN, courtCentreId, payloadFileServiceId);
            prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterDocumentFailedPrivateTopic(courtCentreId.toString(), payloadFileServiceId.toString());

        }
    }
}
