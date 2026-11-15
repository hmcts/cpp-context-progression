package uk.gov.moj.cpp.progression.query.view.service;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * Prepares a published SJP decision ({@code hearing} JSON as stored by Results) for CAAG, before it is converted to a
 * core-domain {@code Hearing}. Every judicial result (offence, defendant and case level) is a JSON object with a
 * {@code judicialResultId}.
 */
public final class SjpDecisionPayloads {

    private static final String JUDICIAL_RESULT_ID = "judicialResultId";
    private static final String JUDICIAL_RESULT_TYPE_ID = "judicialResultTypeId";
    private static final String LAST_SHARED_DATE_TIME = "lastSharedDateTime";
    private static final String RESULT_TEXT = "resultText";
    private static final String LABEL = "label";
    private static final String SHORT_CODE_SEPARATOR = " - ";

    private SjpDecisionPayloads() {
    }

    /**
     * SJP does not set {@code lastSharedDateTime}, so CAAG would show no "Shared on" (BR5). Sets it to the decision's
     * {@code sharedTime} on every judicial result that has none.
     */
    public static JsonValue withLastSharedDateTime(final JsonValue hearing, final JsonValue sharedTime) {
        return mapJudicialResults(hearing, result -> isMissing(result, LAST_SHARED_DATE_TIME)
                ? copyOf(result).add(LAST_SHARED_DATE_TIME, sharedTime).build()
                : result);
    }

    /**
     * SJP result text is "label" plus prompts, so CAAG would show no short code. Prefixes it with the result definition's
     * short code, giving "&lt;short code&gt; - &lt;label&gt;..." as for criminal court results (BR4). A result whose
     * short code is unknown is left as it is.
     */
    public static JsonValue withShortCodes(final JsonValue hearing, final Map<UUID, String> shortCodes) {
        return mapJudicialResults(hearing, result -> {
            final String shortCode = typeId(result) == null ? null : shortCodes.get(typeId(result));
            final String text = result.containsKey(RESULT_TEXT) && !result.isNull(RESULT_TEXT)
                    ? result.getString(RESULT_TEXT) : result.getString(LABEL, null);
            if (shortCode == null || text == null || text.startsWith(shortCode + SHORT_CODE_SEPARATOR)) {
                return result;
            }
            return copyOf(result).add(RESULT_TEXT, shortCode + SHORT_CODE_SEPARATOR + text).build();
        });
    }

    /**
     * @return the result definition IDs ({@code judicialResultTypeId}) of every judicial result in the hearing.
     */
    public static Set<UUID> resultDefinitionIds(final JsonValue hearing) {
        final Set<UUID> ids = new HashSet<>();
        mapJudicialResults(hearing, result -> {
            if (typeId(result) != null) {
                ids.add(typeId(result));
            }
            return result;
        });
        return ids;
    }

    private static JsonValue mapJudicialResults(final JsonValue value, final UnaryOperator<JsonObject> resultMapper) {
        if (value instanceof JsonObject jsonObject) {
            final JsonObjectBuilder builder = createObjectBuilder();
            jsonObject.forEach((key, child) -> builder.add(key, mapJudicialResults(child, resultMapper)));
            final JsonObject mapped = builder.build();
            return mapped.containsKey(JUDICIAL_RESULT_ID) ? resultMapper.apply(mapped) : mapped;
        }
        if (value instanceof JsonArray jsonArray) {
            final JsonArrayBuilder builder = createArrayBuilder();
            jsonArray.forEach(child -> builder.add(mapJudicialResults(child, resultMapper)));
            return builder.build();
        }
        return value;
    }

    private static UUID typeId(final JsonObject result) {
        return isMissing(result, JUDICIAL_RESULT_TYPE_ID) ? null : UUID.fromString(result.getString(JUDICIAL_RESULT_TYPE_ID));
    }

    private static JsonObjectBuilder copyOf(final JsonObject jsonObject) {
        final JsonObjectBuilder builder = createObjectBuilder();
        jsonObject.forEach(builder::add);
        return builder;
    }

    private static boolean isMissing(final JsonObject jsonObject, final String key) {
        return !jsonObject.containsKey(key) || jsonObject.isNull(key);
    }
}
