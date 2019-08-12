package uk.gov.moj.cpp.progression.it.framework;

import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcDataSourceProvider;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.it.framework.util.SystemCommandInvoker;
import uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.cpp.progression.it.framework.util.ViewStoreQueryUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

public class RunCatchupIT {

    private static final String FILENAME_TXT = "MaterialFullStackTestFile.txt";
    private static final String MIME_TYPE_TXT = "text/plain";
    private static final String FILE_PATH_TXT = "upload_samples/sample.txt";

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final DataSource viewStoreDataSource = new TestJdbcDataSourceProvider().getViewStoreDataSource(CONTEXT_NAME);
    private final Poller poller = new Poller();

    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private final ViewStoreQueryUtil viewStoreQueryUtil = new ViewStoreQueryUtil(viewStoreDataSource);
    private final SystemCommandInvoker systemCommandInvoker = new SystemCommandInvoker();

    @Before
    public void cleanDatabase() {

        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanSystemTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        viewStoreCleaner.cleanViewstoreTables();
        systemCommandInvoker.invokeUnshutter();
    }

    @Test
    public void shouldRebuildThePublishedEventTable() throws Exception {

        final int numberOfCommands = 10;

        for (int i = 0; i < numberOfCommands; i++) {
            try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(randomUUID().toString())) {
                addDefendantHelper.addMinimalDefendant();
            }
        }

        final Optional<Integer> publishedEventCount = poller.pollUntilFound(() -> viewStoreQueryUtil.countEventsProcessed(numberOfCommands));

        if (!publishedEventCount.isPresent()) {
            fail("Failed to process events");
        }

        assertThat(publishedEventCount.get() >= numberOfCommands, is(true));

        final List<UUID> defendantIdsFromViewStore = viewStoreQueryUtil.findDefendantIdsFromViewStore();

        assertThat(defendantIdsFromViewStore.size(), is(numberOfCommands));

        viewStoreCleaner.cleanViewstoreTables();
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);

        systemCommandInvoker.invokeCatchup();

        if (!poller.pollUntilFound(() -> viewStoreQueryUtil.countEventsProcessed(numberOfCommands)).isPresent()) {
            fail();
        }

        final List<UUID> idsFromViewStore = viewStoreQueryUtil.findDefendantIdsFromViewStore();

        assertThat(idsFromViewStore.size(), is(numberOfCommands));

        for (int i = 0; i < idsFromViewStore.size(); i++) {
            assertThat(idsFromViewStore, hasItem(defendantIdsFromViewStore.get(i)));
        }
    }
}
