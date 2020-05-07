package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_EVENT_DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.test.utils.core.messaging.Poller;

import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class RaiseDocumentReviewRequiredPublicEventIT extends AbstractIT {

    private final Poller poller = new Poller();

    @Test
    public void shouldRaiseReviewRequiredEventIfNonHtmsUserInCorrectGroupsUploadsCourtDocument() throws Exception {

        final UUID documentId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final String addCourtDocumentPayload = getPayload("progression.add-court-document.json")
                .replaceAll("%RANDOM_DOCUMENT_ID%", documentId.toString())
                .replaceAll("%RANDOM_CASE_ID%", caseId.toString())
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId.toString());

        final Response response = postCommand(getWriteUrl("/courtdocument/" + documentId),
                "application/vnd.progression.add-court-document+json",
                addCourtDocumentPayload);

        assertThat(response.getStatusCode(), is(SC_ACCEPTED));

        try (final MessageConsumer messageConsumer = publicEvents.createConsumer(PUBLIC_EVENT_DOCUMENT_REVIEW_REQUIRED)) {
            final Optional<JsonObject> message = poller.pollUntilFound(() -> retrieveMessageAsJsonObject(messageConsumer));
            if (message.isPresent()) {
                // check payload is something like
                //{
                //        "materialId": "45b0c3fe-afe6-4652-882f-7882d79eadd9",
                //        "receivedDateTime": "2020-01-20T13:50:00Z",
                //        "documentId": "45b0c3fe-afe6-4652-882f-7882d79eadd9",
                //        "source": "OTHER",
                //        "urn": "abcd123",
                //        "prosecutingAuthority": "abc",
                //        "documentType": "Applications",
                //        "code": [
                //        "uploaded-review-required"
                //        ]
                //}
            } else {
                fail();
            }
        }
    }
}

