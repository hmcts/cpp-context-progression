package uk.gov.moj.cpp.progression.domain.transformation.service;

import static java.lang.String.format;
import static java.util.UUID.fromString;

import uk.gov.moj.cpp.progression.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.progression.domain.transformation.helper.JDBCConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HearingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingService.class);

    // -1 in the json path below refers to the last array element
    private static final String QUERY_HEARING = "select (((payload::jsonb->'hearing'->'hearingDays')::json->-1)->>'sittingDay')::date, position_in_stream from hearing_event_log_copy \n" +
            "where payload::jsonb->'hearing'->'hearingDays' is not null\n" +
            "and stream_id = ?\n" +
            "order by 2 desc\n" +
            "limit 1";

    public LocalDate getHearingSittingDate(final String hearingId) {
        LOGGER.info("Retrieving hearing sitting date for stream with ID '{}'", hearingId);
        try (final Connection connection = new JDBCConnectionFactory().getConnection();
             final PreparedStatement statement = connection.prepareStatement(QUERY_HEARING)) {
            statement.setObject(1, fromString(hearingId));

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    return rs.getDate(1).toLocalDate();
                }
            }
            LOGGER.error("Hearing sitting date not found for stream '{}'", hearingId);
            throw new TransformationException(format("Hearing sitting date not found for stream '%s'", hearingId));
        } catch (final SQLException e) {
            throw new TransformationException(format("Hearing sitting date not found for stream '%s'", hearingId), e);
        }
    }

}
