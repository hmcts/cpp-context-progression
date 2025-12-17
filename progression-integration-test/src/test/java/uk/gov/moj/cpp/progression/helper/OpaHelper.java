package uk.gov.moj.cpp.progression.helper;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;

import org.hamcrest.Matcher;

public class OpaHelper {

    private static final String PROGRESSION_QUERY_GET_OPA_PRESS_LIST = "application/vnd.progression.query.press-list-opa-notices+json";
    private static final String PROGRESSION_QUERY_GET_OPA_PUBLIC_LIST = "application/vnd.progression.query.public-list-opa-notices+json";
    private static final String PROGRESSION_QUERY_GET_OPA_RESULT_LIST = "application/vnd.progression.query.result-list-opa-notices+json";

    public static String pollForOpaPressList(final Matcher... matchers) {
        return pollForResponse("/opa-notice",
                PROGRESSION_QUERY_GET_OPA_PRESS_LIST,
                randomUUID().toString(),
                matchers
        );
    }

    public static String pollForOpaPublicList(final Matcher... matchers) {
        return pollForResponse("/opa-notice",
                PROGRESSION_QUERY_GET_OPA_PUBLIC_LIST,
                randomUUID().toString(),
                matchers
        );
    }

    public static String pollForOpaResultList(final Matcher... matchers) {
        return pollForResponse("/opa-notice",
                PROGRESSION_QUERY_GET_OPA_RESULT_LIST,
                randomUUID().toString(),
                matchers
        );
    }
}
