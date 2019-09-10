package uk.gov.moj.cpp.progression.it.framework;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.jmx.system.command.client.SystemCommandCaller;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcDataSourceProvider;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.cpp.progression.it.framework.util.ViewStoreQueryUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

public class RunCatchupIT {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final DataSource viewStoreDataSource = new TestJdbcDataSourceProvider().getViewStoreDataSource(CONTEXT_NAME);
    private final Poller poller = new Poller(10, 2000l);

    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private final ViewStoreQueryUtil viewStoreQueryUtil = new ViewStoreQueryUtil(viewStoreDataSource);
    private final SystemCommandCaller systemCommandCaller = new SystemCommandCaller(CONTEXT_NAME);

    @Before
    public void cleanDatabase() {

        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanSystemTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        viewStoreCleaner.cleanViewstoreTables();
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
        assertThat(publishedEventCount.isPresent(), is(true));
        assertThat(publishedEventCount.get() >= numberOfCommands, is(true));

        final Optional<List<UUID>> idsFromViewStore = poller.pollUntilFound(() -> viewStoreQueryUtil.findIdsFromViewStore(numberOfCommands));
        assertThat(idsFromViewStore.isPresent(), is(true));
        assertThat(idsFromViewStore.get().size(), is(numberOfCommands));

        viewStoreCleaner.cleanViewstoreTables();
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);

        systemCommandCaller.callCatchup();

        assertThat(poller.pollUntilFound(() -> viewStoreQueryUtil.countEventsProcessed(numberOfCommands)).isPresent(), is(true));

        final Optional<List<UUID>> catchupIdsFromViewStore = poller.pollUntilFound(() -> viewStoreQueryUtil.findIdsFromViewStore(numberOfCommands));
        assertThat(catchupIdsFromViewStore.isPresent(), is(true));
        assertThat(catchupIdsFromViewStore.get().size(), is(numberOfCommands));

        for (int i = 0; i < catchupIdsFromViewStore.get().size(); i++) {
            assertThat(catchupIdsFromViewStore.get(), hasItem(idsFromViewStore.get().get(i)));
        }
    }
}
