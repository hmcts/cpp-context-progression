package uk.gov.moj.cpp.progression.domain.transformation.service;

import static java.lang.String.format;

import uk.gov.moj.cpp.progression.domain.transformation.domain.ResultDefinition;
import uk.gov.moj.cpp.progression.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.progression.domain.transformation.helper.JDBCConnectionFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ResultDefinitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultDefinitionService.class);
    private static final String QUERY_RESULT_DEFINITION = "select payload from result_definition_copy\n" +
            "where deleted = false\n" +
            "and start_date <= ?\n" +
            "and (end_date is null or end_date >= ?)\n" +
            "and lower(payload::jsonb->>'label') = lower(?)\n" +
            "order by version desc";

    private static final String PAYLOAD = "payload";

    public ResultDefinition getResultDefinition(final String eventDate, final String label) {
        LOGGER.debug("Retrieving result definition using for label '{}' and date '{}'", label, eventDate);
        try (final Connection connection = new JDBCConnectionFactory().getConnection();
             final PreparedStatement statement = connection.prepareStatement(QUERY_RESULT_DEFINITION)) {
            statement.setDate(1, Date.valueOf(eventDate));
            statement.setDate(2, Date.valueOf(eventDate));
            statement.setString(3, label);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    final String payloadAsString = rs.getString(PAYLOAD);
                    return new Gson().fromJson(payloadAsString, ResultDefinition.class);
                }
            }
            LOGGER.error("Result definition not found for label '{}' and date '{}'", label, eventDate);
            throw new TransformationException(format("Result definition not found for label '%s' and date '%s'", label, eventDate));
        } catch (SQLException e) {
            throw new TransformationException(format("Error retrieving result definition for date '%s' and matching label '%s'", eventDate, label), e);
        }
    }

}
