package uk.gov.moj.cpp.progression.ingester;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.eventstore.management.commands.IndexerCatchupCommand.INDEXER_CATCHUP;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;

import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitialIndexerIngestionIT extends AbstractIT {

    private static final Logger LOG = LoggerFactory.getLogger(InitialIndexerIngestionIT.class);
    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";

    private static final String HOST = getHost();
    private static final int PORT = 9990;
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();


    @BeforeEach
    public void setup() throws IOException {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanSystemTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "processed_event");
        deleteAndCreateIndex();
    }

    @AfterAll
    public static void tearDown() {
        cleanEventStoreTables();
    }

    @Test
    public void shouldRunIndexerCatchUpFindEventsInElasticSearchStore() throws Exception {
        final int totalCases = 3;
        for (int i = 0; i < totalCases; i++) {
            final String caseUrn = PreAndPostConditionHelper.generateUrn();
            addProsecutionCaseToCrownCourtForIngestion(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(),
                    randomUUID().toString(), randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);
        }

        checkCaseCountInElasticSearch(totalCases);

        elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();

        checkCaseCountInElasticSearch(0);

        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "processed_event", "stream_status", "stream_buffer");

        runIndexerCatchup();

        checkCaseCountInElasticSearch(totalCases);
    }

    private void checkCaseCountInElasticSearch(final int totalCases) {
        final Optional<Integer> numberOfCases = pollUntilRequiredResultsFound(totalCases);

        if (numberOfCases.isPresent()) {
            assertThat(numberOfCases.get(), CoreMatchers.is(totalCases));
        } else {
            fail("Failed to find " + totalCases + " cases in Index");
        }
    }

    private void runIndexerCatchup() throws Exception {
        final JmxParameters jmxParameters = jmxParameters()
                .withContextName(CONTEXT_NAME)
                .withHost(HOST)
                .withPort(PORT)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .build();
        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameters)) {

            systemCommanderClient.getRemote(CONTEXT_NAME).call(INDEXER_CATCHUP);
        }
    }

    private Optional<Integer> pollUntilRequiredResultsFound(int totalCases) {
        return getPoller().pollUntilFound(() -> {

            try {
                final JsonObject crime_case_index_data = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                final int totalResults = crime_case_index_data.getInt("totalResults");
                if (totalResults >= totalCases) {
                    return of(totalResults);
                }
            } catch (IOException e) {
                LOG.error("Failed to get totalResult ", e);
                fail("Failed to get totalResult.");
            }
            return empty();
        });
    }
}
