package uk.gov.moj.cpp.progression;

import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class NotificationIT extends AbstractIT {

    @BeforeAll
    static void setup() {
        NotificationServiceStub.stubPostCallsNotificationNotify();
    }

    @Test
    public void shouldSendEmail() throws Exception {

        String body = "{\"caseId\":\"c1561fc5-6f3d-40ac-9f74-f1aef1b5850a\"," +
                "\"applicationId\":\"faffc14a-894c-4f67-94c5-90e3799eb4b6\"," +
                "\"materialId\":\"30346521-1d7c-41d9-88ad-a4ba26697d31\"," +
                "\"notifications\":[{\"notificationId\":\"8697138e-ac6f-4778-bd5f-6b97486c8dcf\"," +
                "\"templateId\":\"bddd10df-d0fb-4f19-8d2d-72febb2673af\",\"sendToAddress\":\"sendToAddress\"}]}";

        final Response writeResponse = postCommand(
                getWriteUrl("/notification"),
                "application/vnd.progression.send.email+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
    }

    @Test
    public void shouldSendPrint() throws Exception {

        String body = "{\"caseId\":\"b97a26cd-40b5-4da0-8d2f-ceb91e04b463\",\"notificationId\":\"d844e25e-7901-4c8a-9c8c-576d62e76f46\"," +
                "\"materialId\":\"fccb645e-2b0a-4d23-a662-bbecea7e9ac6\",\"postage\":false}";

        final Response writeResponse = postCommand(
                getWriteUrl("/notification"),
                "application/vnd.progression.send.print+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
    }

    @Test
    public void shouldUpdateSendToCps() throws Exception {

        String body = "{\"courtDocumentId\":\"be2775f8-a1b3-4504-b69a-6225ad498417\",\"sendToCps\":true}";

        final Response writeResponse = postCommand(
                getWriteUrl("/notification"),
                "application/vnd.progression.update-send-to-cps-flag+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
    }
}
