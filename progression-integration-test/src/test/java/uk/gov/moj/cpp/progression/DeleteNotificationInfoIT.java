package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import java.io.IOException;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class DeleteNotificationInfoIT extends AbstractIT {

    @Test
    public void shouldDeleteNotificationInfo() throws IOException {
        final String commandUri = getWriteUrl("/delete-notification-info");
        final Response response = postCommand(commandUri,
                "application/vnd.progression.delete-notification-info+json",
                "{}");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

}
