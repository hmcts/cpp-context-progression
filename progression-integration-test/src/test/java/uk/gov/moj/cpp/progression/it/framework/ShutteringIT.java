package uk.gov.moj.cpp.progression.it.framework;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.jmx.api.state.ApplicationManagementState.SHUTTERED;
import static uk.gov.justice.services.jmx.api.state.ApplicationManagementState.UNSHUTTERED;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.it.framework.util.ApplicationStateUtil.getApplicationState;

import uk.gov.justice.services.jmx.system.command.client.SystemCommandCaller;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcDataSourceProvider;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.cpp.progression.it.framework.util.ViewStoreQueryUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ShutteringIT {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final DataSource viewStoreDataSource = new TestJdbcDataSourceProvider().getViewStoreDataSource(CONTEXT_NAME);
    private final DataSource systemDataSource = new TestJdbcDataSourceProvider().getSystemDataSource(CONTEXT_NAME);
    private final Poller poller = new Poller();

    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();
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

    @After
    public void tearDown() {
        systemCommandCaller.callUnshutter();
        assertThat(getApplicationState(UNSHUTTERED), is(of(UNSHUTTERED)));
    }

    @Test
    public void shouldRebuildThePublishedEventTable() throws Exception {

        systemCommandCaller.callShutter();
        assertThat(getApplicationState(SHUTTERED), is(of(SHUTTERED)));

        final int numberOfCommands = 10;
        for (int i = 0; i < numberOfCommands; i++) {
            try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(randomUUID().toString())) {
                addDefendantHelper.addMinimalDefendant();
            }
        }

        final Optional<Integer> shutteredEvents = poller.pollUntilFound(() -> countEventsShuttered(numberOfCommands));
        assertThat(shutteredEvents.isPresent(), is(true));
        assertThat(shutteredEvents.get() >= numberOfCommands, is(true));

        assertThat(viewStoreQueryUtil.countEventsProcessed(numberOfCommands), is(empty()));

        final Optional<List<UUID>> idsFromViewStore = poller.pollUntilFound(() -> viewStoreQueryUtil.findIdsFromViewStore(numberOfCommands));
        assertThat(idsFromViewStore, is(empty()));

        systemCommandCaller.callUnshutter();
        assertThat(getApplicationState(UNSHUTTERED), is(of(UNSHUTTERED)));

        assertThat(poller.pollUntilFound(() -> viewStoreQueryUtil.countEventsProcessed(numberOfCommands)).isPresent(), is(true));

        final Optional<List<UUID>> catchupIdsFromViewStore = poller.pollUntilFound(() -> viewStoreQueryUtil.findIdsFromViewStore(numberOfCommands));
        assertThat(catchupIdsFromViewStore.isPresent(), is(true));
        assertThat(catchupIdsFromViewStore.get().size(), is(numberOfCommands));
    }

    private Optional<Integer> countEventsShuttered(final int expectedNumberOfEvents) {

        final String sql = "SELECT COUNT(*) FROM stored_command";
        try (final Connection connection = systemDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(sql);
             final ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {

                final int numberOfShutteredEvents = resultSet.getInt(1);

                if (numberOfShutteredEvents >= expectedNumberOfEvents) {
                    return of(numberOfShutteredEvents);
                }

                return empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to run " + sql, e);
        }

        return empty();
    }
}
