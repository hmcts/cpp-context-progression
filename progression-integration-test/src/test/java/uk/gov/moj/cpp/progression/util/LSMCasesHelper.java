package uk.gov.moj.cpp.progression.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class LSMCasesHelper {

    public static Matcher[] getLsmQueryMatchers(final String linkType, final int numberOfCase, final String[] prosecutionCaseIds, final String[] defendantIds) {
        if (prosecutionCaseIds.length != numberOfCase) {
            throw new IllegalArgumentException(String.format("The count of prosecutionCaseIds does not match with numberOfCase parameter (%d)", numberOfCase));
        }

        if (defendantIds.length != numberOfCase) {
            throw new IllegalArgumentException(String.format("The count of defendantIds does not match with numberOfCase parameter (%d)", numberOfCase));
        }

        final Matcher<String> caseMatcher = anyOf(Arrays.stream(prosecutionCaseIds).map(Matchers::equalTo).collect(Collectors.toList()));
        final Matcher<String> defendantMatcher = anyOf(Arrays.stream(defendantIds).map(Matchers::equalTo).collect(Collectors.toList()));

        final List<Matcher> matchers = new ArrayList<>();
        final String prefix = "$." + linkType;

        matchers.add(withJsonPath(prefix + ".length()", equalTo(numberOfCase)));
        for (int i = 0; i < numberOfCase; i++) {
            final String arrayPrefix = String.format(prefix + "[%d]", i);
            matchers.add(withJsonPath(arrayPrefix + ".caseId", caseMatcher));
            matchers.add(withJsonPath(arrayPrefix + ".defendants[0].firstName", equalTo("Harry")));
            matchers.add(withJsonPath(arrayPrefix + ".defendants[0].middleName", equalTo("Jack")));
            matchers.add(withJsonPath(arrayPrefix + ".defendants[0].lastName", equalTo("Kane Junior")));
            matchers.add(withJsonPath(arrayPrefix + ".defendants[0].id", defendantMatcher));
            matchers.add(withJsonPath(arrayPrefix + ".defendants[0].offences[0].offenceTitle", equalTo("ROBBERY")));
        }

        return matchers.toArray(new Matcher[matchers.size()]);
    }
}
