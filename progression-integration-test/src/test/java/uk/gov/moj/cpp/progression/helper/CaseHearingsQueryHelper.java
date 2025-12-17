package uk.gov.moj.cpp.progression.helper;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;

import org.hamcrest.Matcher;

public class CaseHearingsQueryHelper {

    private static final String PROGRESSION_QUERY_GET_CASE_HEARINGS = "application/vnd.progression.query.casehearings+json";
    private static final String PROGRESSION_QUERY_GET_CASE_HEARINGS_FOR_COURT_EXTRACT = "application/vnd.progression.query.case-hearings-for-court-extract+json";
    private static final String PROGRESSION_QUERY_GET_CASE_HEARING_TYPES = "application/vnd.progression.query.case.hearingtypes+json";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";

    public static void pollForCaseHearings(final String caseId, final Matcher... matchers) {
        pollForResponse("/prosecutioncases/" + caseId,
                PROGRESSION_QUERY_GET_CASE_HEARINGS,
                randomUUID().toString(),
                matchers
        );
    }

    public static void pollForCaseHearingsForCourtExtract(final String caseId, final String defendantId, final Matcher... matchers) {
        pollForResponse("/prosecutioncases/" + caseId + "/defendants/" + defendantId,
                PROGRESSION_QUERY_GET_CASE_HEARINGS_FOR_COURT_EXTRACT,
                randomUUID().toString(),
                matchers
        );
    }

    public static void pollCaseHearingTypes(final String caseId, final String orderDate, final Matcher... matchers) {
        pollForResponse("/prosecutioncases/" + caseId + "?orderDate=" + orderDate,
                PROGRESSION_QUERY_GET_CASE_HEARING_TYPES,
                randomUUID().toString(),
                matchers);
    }

    public static String pollForHearing(final String hearingId, final Matcher... matchers) {
        return pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON, matchers);
    }
}
