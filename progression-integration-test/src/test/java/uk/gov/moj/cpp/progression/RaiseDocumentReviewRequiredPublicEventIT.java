package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_EVENT_DOCUMENT_REVIEW_REQUIRED;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseWithUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyCasesByCaseUrn;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupOrganisation;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.messaging.Poller;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RaiseDocumentReviewRequiredPublicEventIT extends AbstractIT {

    private final Poller poller = new Poller();

    @BeforeEach
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
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        final String organisation = getPayload("stub-data/usersgroups.get-organisation-details.json")
                .replace("%ORGANISATION_ID%", UUID.randomUUID().toString());

        stubUserGroupOrganisation(organisation);

        addProsecutionCaseWithUrn(caseId.toString(), defendantId.toString(), urn);
        verifyCasesByCaseUrn(urn, new Matcher[]{withJsonPath("$.searchResults[0].caseId", equalTo(caseId.toString()))});

        final String addCourtDocumentPayload = getPayload("progression.add-court-document.json")
                .replaceAll("%RANDOM_DOCUMENT_ID%", documentId.toString())
                .replaceAll("%RANDOM_CASE_ID%", caseId.toString())
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId.toString());

        final JmsMessageConsumerClient messageConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_DOCUMENT_REVIEW_REQUIRED).getMessageConsumerClient();

        final Response response = postCommand(getWriteUrl("/courtdocument/" + documentId),
                "application/vnd.progression.add-court-document+json",
                addCourtDocumentPayload);
        assertThat(response.getStatusCode(), is(SC_ACCEPTED));

        final Optional<JsonObject> message = poller.pollUntilFound(() -> retrieveMessageBody(messageConsumer));
        if (message.isPresent()) {
            final JsonObject responsePayload = message.get();
            assertThat(responsePayload.getString("caseId"), is(caseId.toString()));
            assertThat(responsePayload.getString("urn"), is(urn));
        } else {
            fail();
        }
    }
}

