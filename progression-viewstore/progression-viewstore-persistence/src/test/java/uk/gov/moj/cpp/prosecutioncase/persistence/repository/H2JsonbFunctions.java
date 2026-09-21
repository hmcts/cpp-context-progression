package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stand-ins for the two PostgreSQL jsonb functions CourtDocumentIndexCriteriaRepository asks for,
 * so the search that filters on a section id can actually be executed against H2.
 *
 * Hibernate renders that predicate as {@code jsonb_extract_path_text(jsonb(payload), 'documentTypeId')}.
 * H2 has neither function, so without these the query fails to parse and the only branch of
 * getCourtDocumentPredicates that uses them cannot be tested at all.
 *
 * These are deliberately the smallest thing that behaves correctly for a flat payload of string
 * values, which is what the court document payload is. They are not a jsonb implementation, and
 * nothing outside this test should use them: a nested object, an array or a non-string value would
 * not be handled. The real behaviour is PostgreSQL's, and the integration tests cover that.
 */
public final class H2JsonbFunctions {

    private H2JsonbFunctions() {
    }

    /**
     * PostgreSQL casts text to jsonb here. H2 holds the payload as a plain string, so there is
     * nothing to convert and the value passes straight through.
     */
    public static String jsonb(final String value) {
        return value;
    }

    /**
     * Returns the value of a top-level string key, or null when the key is absent — which is what
     * {@code ->>} does in PostgreSQL.
     */
    public static String jsonbExtractPathText(final String json, final String key) {
        if (json == null) {
            return null;
        }
        final Matcher matcher = Pattern
                .compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
}
