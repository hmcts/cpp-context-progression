package uk.gov.moj.cpp.progression.util;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;

import java.util.List;

import org.hamcrest.Matcher;

public class PartialMatchDefendantHelper {

    public static Matcher[] getPartialMatchDefendantMatchers(final String caseId, final String defendantId, final String pncId, final String croNumber) {
        List<Matcher> matchers = newArrayList(
                withJsonPath("$.matchedDefendants[0].prosecutionCaseId", is(caseId)),
                withJsonPath("$.matchedDefendants[0].defendantId", is(defendantId)),
                withJsonPath("$.matchedDefendants[0].defendantsMatched[0].pncId", is(pncId)),
                withJsonPath("$.matchedDefendants[0].defendantsMatched[0].croNumber", is(croNumber))
        );
        return matchers.toArray(new Matcher[0]);
    }

}
