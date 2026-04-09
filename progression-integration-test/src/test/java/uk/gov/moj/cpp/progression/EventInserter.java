package uk.gov.moj.cpp.progression;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.jdbc.persistence.DataAccessException;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;

import static uk.gov.justice.services.common.converter.ZonedDateTimes.toSqlTimestamp;

public class EventInserter {

    private static final String INSERT_EVENT_INTO_EVENT_LOG_SQL = """
            INSERT INTO
            event_log (
                 id,
                 stream_id,
                 position_in_stream,
                 name,
                 metadata,
                 payload,
                 date_created,
                 event_number,
                 previous_event_number,
                 is_published)
            VALUES(?, ?, ?, ?, ?, ?, ?, NULL, NULL, FALSE)
            ON CONFLICT DO NOTHING
            """;

    public static void insertEvent(JsonEnvelope jsonEnvelope) {
        final Metadata eventMetadata = jsonEnvelope.metadata();

        try (final Connection connection = new TestJdbcConnectionProvider().getEventStoreConnection("progression");
             final PreparedStatement ps = connection.prepareStatement(INSERT_EVENT_INTO_EVENT_LOG_SQL)) {

            ps.setObject(1, eventMetadata.id());
            ps.setObject(2, eventMetadata.streamId().orElse(null));
            ps.setLong(3, eventMetadata.position().orElse(null));
            ps.setString(4, eventMetadata.name());
            ps.setString(5, eventMetadata.asJsonObject().toString());
            ps.setString(6, extractPayloadAsString(jsonEnvelope));
            ps.setTimestamp(7, toSqlTimestamp(ZonedDateTime.now()));
            ps.executeUpdate();

        } catch (final SQLException e) {
            throw new DataAccessException("Failed to insert event to event_log table:" + jsonEnvelope, e);
        }
    }

    private static String extractPayloadAsString(final JsonEnvelope envelope) {
        final JsonObjectEnvelopeConverter jsonObjectEnvelopeConverter = new DefaultJsonObjectEnvelopeConverter();
        ReflectionUtil.setField(jsonObjectEnvelopeConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

        return jsonObjectEnvelopeConverter.extractPayloadFromEnvelope(
                jsonObjectEnvelopeConverter.fromEnvelope(envelope)).toString();
    }
}
