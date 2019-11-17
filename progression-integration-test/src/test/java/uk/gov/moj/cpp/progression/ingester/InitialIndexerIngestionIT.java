package uk.gov.moj.cpp.progression.ingester;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.eventstore.management.commands.IndexerCatchupCommand.INDEXER_CATCHUP;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;

import uk.gov.justice.services.eventstore.management.commands.IndexerCatchupCommand;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitialIndexerIngestionIT {

    private static final Logger LOG = LoggerFactory.getLogger(InitialIndexerIngestionIT.class);
    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";

    private static final String HOST = getHost();
    private static final int PORT = 9990;
    private static final String CONTEXT = "progression";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private ElasticSearchIndexRemoverUtil elasticSearchIndexRemoverUtil = null;
    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();
    final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
    private ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);


    @Before
    public void before() throws IOException {
        elasticSearchIndexRemoverUtil = new ElasticSearchIndexRemoverUtil();
        elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();

        databaseCleaner.cleanEventStoreTables(CONTEXT);
        databaseCleaner.cleanSystemTables(CONTEXT);
        databaseCleaner.cleanStreamStatusTable(CONTEXT);
        databaseCleaner.cleanStreamBufferTable(CONTEXT);
        databaseCleaner.cleanViewStoreTables(CONTEXT, "processed_event");
    }

    @AfterClass
    public static void tearDown() {
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    @Test
    public void shouldRunIndexerCatchUpFindEventsInElasticSearchStore() throws Exception {
        final int totalCases = 2;
        for (int i = 0; i < totalCases; i++) {
            final String caseUrn = PreAndPostConditionHelper.generateUrn();
            addProsecutionCaseToCrownCourtForIngestion(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(),
                    randomUUID().toString(), randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);
        }

        checkCaseCountInElasticSearch(totalCases);

        elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();

        checkCaseCountInElasticSearch(0);

        databaseCleaner.cleanViewStoreTables(CONTEXT, "processed_event", "stream_status", "stream_buffer");

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
                .withContextName(CONTEXT)
                .withHost(HOST)
                .withPort(PORT)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .build();
        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameters)) {

            systemCommanderClient.getRemote(CONTEXT).call(INDEXER_CATCHUP);
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