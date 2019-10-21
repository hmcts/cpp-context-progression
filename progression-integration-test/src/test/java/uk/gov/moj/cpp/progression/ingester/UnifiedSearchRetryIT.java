package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.helper.StubUtil.resetStubs;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;

import uk.gov.justice.services.test.utils.core.messaging.DeadLetterQueueBrowser;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class UnifiedSearchRetryIT {
    private static final String CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.create-court-application.json";
    private static final String EVENT_NAME = "progression.event.court-application-created";

    private DeadLetterQueueBrowser deadLetterQueueBrowser;


    @BeforeClass
    public static void beforeClass() {
        resetStubs();
        setupUsersGroupQueryStub();
        stubEnableAllCapabilities();
    }

    @Before
    public void setUp() throws IOException {
        deadLetterQueueBrowser = new DeadLetterQueueBrowser();
        deadLetterQueueBrowser.removeMessages();
        new ElasticSearchIndexRemoverUtil().deleteCaseIndex("crime_case_index");
    }

    @AfterClass
    public static void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    @Test
    public void shouldRetryToIngestData() throws IOException {
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        addCourtApplicationForIngestion(caseId, applicationId, randomUUID().toString(), randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        final Optional<List<String>> deadLetterQueueMessagesResult = getPoller().pollUntilFound(() -> {

            final List<String> deadLetterQueueMessages = deadLetterQueueBrowser.browse();
            if (!deadLetterQueueMessages.isEmpty()
                    && validMessageFound(caseId, deadLetterQueueMessages)) {
                return Optional.of(deadLetterQueueMessages);
            }
            return empty();
        });

        assertThat(deadLetterQueueMessagesResult.isPresent(), is(true));

        final String message = getRightDLQMessage(caseId, deadLetterQueueMessagesResult.get());

        with(message)
                .assertThat("courtApplication.id", is(applicationId), deadLetterQueueMessagesResult.get().toString())
                .assertThat("_metadata.name", is(EVENT_NAME), deadLetterQueueMessagesResult.get().toString());
    }

    private boolean validMessageFound(final String caseId, final List<String> deadLetterQueueMessages) {
        return !StringUtils.EMPTY.equals(getRightDLQMessage(caseId, deadLetterQueueMessages));
    }

    private String getRightDLQMessage(final String caseId, final List<String> deadLetterQueueMessages) {
        for (final String message : deadLetterQueueMessages) {
            if (message.contains(caseId) && message.contains(EVENT_NAME)) {
                return message;
            }
        }

        return StringUtils.EMPTY;
    }
}
