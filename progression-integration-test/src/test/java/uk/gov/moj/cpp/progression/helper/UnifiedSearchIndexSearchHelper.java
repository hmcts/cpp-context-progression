package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;

import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.hamcrest.Matcher;

public class UnifiedSearchIndexSearchHelper {

    private static final String INDEX_NAME = "crime_case_index";

    public static Optional<JsonObject> findBy(final Matcher[] matchers) {

        final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        final ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);

        return getPoller().pollUntilFound(() -> {
            try {
                final JsonObject elasticSearchCrimeCaseIndex = elasticSearchIndexFinderUtil.findAll(INDEX_NAME);
                if (elasticSearchCrimeCaseIndex.getInt("totalResults") > 0) {

                    final JsonArray indexes = elasticSearchCrimeCaseIndex.getJsonArray("index");
                    final Optional<JsonValue> matchedJsonValue = indexes.stream().filter(indexValue -> isJsonMatch(matchers, (JsonString) indexValue)).findFirst();

                    if (matchedJsonValue.isPresent()) {
                        final JsonString jsonString = (JsonString) matchedJsonValue.get();
                        final JsonObject value = jsonFromString(jsonString.getString());

                        return Optional.of(value);
                    }
                }
            } catch (final IOException e) {
                fail();
            }
            return empty();
        });
    }

    private static boolean isJsonMatch(final Matcher[] matchers, final JsonString jsonString) {
        try {
            assertThat(jsonString.getString(), isJson(allOf(matchers)));
        } catch (final AssertionError error) {
            return false;
        }
        return true;
    }

}
