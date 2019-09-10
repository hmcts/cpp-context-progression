package uk.gov.moj.cpp.progression.it.framework.util;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

public class ViewStoreQueryUtil {

    private final DataSource viewStoreDataSource;

    public ViewStoreQueryUtil(final DataSource viewStoreDataSource) {
        this.viewStoreDataSource = viewStoreDataSource;
    }

    public Optional<List<UUID>> findIdsFromViewStore(final int expectedNumberOfIds) {

        final List<UUID> ids = new ArrayList<>();

        final String sql = "SELECT defendant_id FROM defendant";
        try (final Connection connection = viewStoreDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(sql);
             final ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                ids.add((UUID) resultSet.getObject("defendant_id"));
            }

            if (ids.size() >= expectedNumberOfIds) {
                return of(ids);
            }

            return empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to run " + sql, e);
        }
    }

    public Optional<Integer> countEventsProcessed(final int expectedNumberOfEvents) {

        final String sql = "SELECT COUNT(*) FROM processed_event";
        try (final Connection connection = viewStoreDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(sql);
             final ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {

                final int numberOfProcessedEvents = resultSet.getInt(1);

                if (numberOfProcessedEvents >= expectedNumberOfEvents) {
                    return of(numberOfProcessedEvents);
                }

                return empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to run " + sql, e);
        }

        return empty();
    }
}
