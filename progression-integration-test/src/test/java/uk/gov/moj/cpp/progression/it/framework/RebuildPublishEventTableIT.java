package uk.gov.moj.cpp.progression.it.framework;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.PublishedEvent;
import uk.gov.justice.services.jmx.system.command.client.SystemCommandCaller;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.SequenceSetter;
import uk.gov.justice.services.test.utils.persistence.TestJdbcDataSourceProvider;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

public class RebuildPublishEventTableIT {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final SequenceSetter sequenceSetter = new SequenceSetter();
    private final DataSource eventStoreDataSource = new TestJdbcDataSourceProvider().getEventStoreDataSource(CONTEXT_NAME);
    private final Poller poller = new Poller(10, 2000l);

    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
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

        final long nextEventNumber = sequenceSetter.getCurrentSequenceValue("event_sequence_seq", eventStoreDataSource) + 1;

        final String caseId = randomUUID().toString();
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(caseId)) {
            addDefendantHelper.addMinimalDefendant();
        }

        final int numberOfEvents = 1;
        final Optional<List<PublishedEvent>> publishedEvents = poller.pollUntilFound(() -> findPublishedEvents(numberOfEvents));

        if (publishedEvents.isPresent()) {
            final Long eventNumber = publishedEvents.get().get(0).getEventNumber().orElse(-1L);

            assertThat(eventNumber, is(nextEventNumber));
        } else {
            fail();
        }

        systemCommandCaller.callRebuild();

        final Optional<List<PublishedEvent>> rebuiltPublishedEvents = poller.pollUntilFound(() -> findPublishedEvents(numberOfEvents));

        if (rebuiltPublishedEvents.isPresent()) {
            final Long eventNumber = rebuiltPublishedEvents.get().get(0).getEventNumber().orElse(-1L);

            assertThat(eventNumber, is(1L));
        } else {
            fail();
        }
    }

    private Optional<List<PublishedEvent>> findPublishedEvents(final int numberOfEvents) {

        final List<PublishedEvent> publishedEvents = new ArrayList<>();

        final String sql = "SELECT * FROM published_event";
        try (final Connection connection = eventStoreDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(sql);
             final ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                final PublishedEvent publishedEvent = new PublishedEvent(
                        (UUID) resultSet.getObject("id"),
                        (UUID) resultSet.getObject("stream_id"),
                        resultSet.getLong("position_in_stream"),
                        resultSet.getString("name"),
                        resultSet.getString("metadata"),
                        resultSet.getString("payload"),
                        ZonedDateTimes.fromSqlTimestamp(resultSet.getTimestamp("date_created")),
                        resultSet.getLong("event_number"),
                        resultSet.getLong("previous_event_number")
                );

                publishedEvents.add(publishedEvent);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to run " + sql, e);
        }

        if (publishedEvents.size() >= numberOfEvents) {
            return of(publishedEvents);
        }

        return empty();
    }
}
