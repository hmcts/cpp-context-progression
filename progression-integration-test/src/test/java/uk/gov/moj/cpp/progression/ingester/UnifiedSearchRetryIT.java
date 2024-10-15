package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForIngestion;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.services.test.utils.core.messaging.DeadLetterQueueBrowser;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.progression.AbstractIT;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
public class UnifiedSearchRetryIT extends AbstractIT {
    public static final Poller POLLER = new Poller(300, 1000L);
    private static final String CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.create-court-application.json";
    private static final String EVENT_NAME = "progression.event.court-application-created";
    private DeadLetterQueueBrowser deadLetterQueueBrowser;

    @AfterAll
    public static void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();

    }

    @BeforeEach
    public void setup() throws IOException {
        deadLetterQueueBrowser = new DeadLetterQueueBrowser();
        deadLetterQueueBrowser.removeMessages();
        elasticSearchIndexRemoverUtil.deleteCaseIndex("crime_case_index");
    }

    @AfterEach
    public void teardown() throws IOException {
        elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex("crime_case_index");
    }

    @Test
    @SuppressWarnings("squid:S1607")
    public void shouldRetryToIngestData() throws IOException {
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        addCourtApplicationForIngestion(caseId, applicationId, randomUUID().toString(), randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), ApplicationStatus.DRAFT.toString(), CREATE_COURT_APPLICATION_COMMAND_RESOURCE_LOCATION);

        final Optional<List<String>> deadLetterQueueMessagesResult = POLLER.pollUntilFound(() -> {

            final List<String> deadLetterQueueMessages = deadLetterQueueBrowser.browse();
            if (!deadLetterQueueMessages.isEmpty()
                    && validMessageFound(applicationId, deadLetterQueueMessages)) {
                return Optional.of(deadLetterQueueMessages);
            }
            return empty();
        });

        assertThat(deadLetterQueueMessagesResult.isPresent(), is(true));

        final String message = getRightDLQMessage(applicationId, deadLetterQueueMessagesResult.get());

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
