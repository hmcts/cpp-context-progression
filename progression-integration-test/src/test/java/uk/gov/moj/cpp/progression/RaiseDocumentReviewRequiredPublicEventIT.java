package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_EVENT_DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseWithUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyCasesForSearchCriteria;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.test.utils.core.messaging.Poller;

import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class RaiseDocumentReviewRequiredPublicEventIT extends AbstractIT {

    private final Poller poller = new Poller();

    @Before
    public void setup() {
        setupUsersGroupQueryStub();
    }

    @Test
    public void shouldRaiseReviewRequiredEventIfNonHtmsUserInCorrectGroupsUploadsCourtDocument() throws Exception {

        final UUID documentId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final String urn = generateUrn();

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");

        addProsecutionCaseWithUrn(caseId.toString(), defendantId.toString(), urn);
        verifyCasesForSearchCriteria(urn, new Matcher[]{withJsonPath("$.searchResults[0].caseId", equalTo(caseId.toString()))});

        final String addCourtDocumentPayload = getPayload("progression.add-court-document.json")
                .replaceAll("%RANDOM_DOCUMENT_ID%", documentId.toString())
                .replaceAll("%RANDOM_CASE_ID%", caseId.toString())
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId.toString());

        try (final MessageConsumer messageConsumer = publicEvents.createConsumer(PUBLIC_EVENT_DOCUMENT_REVIEW_REQUIRED)) {
            final Response response = postCommand(getWriteUrl("/courtdocument/" + documentId),
                    "application/vnd.progression.add-court-document+json",
                    addCourtDocumentPayload);
            assertThat(response.getStatusCode(), is(SC_ACCEPTED));

            final Optional<JsonObject> message = poller.pollUntilFound(() -> retrieveMessageAsJsonObject(messageConsumer));
            if (message.isPresent()) {
                final JsonObject responsePayload = message.get();
                assertThat(responsePayload.getString("caseId"), is(caseId.toString()));
                assertThat(responsePayload.getString("urn"), is(urn));
            } else {
                fail();
            }
        }
    }
}

