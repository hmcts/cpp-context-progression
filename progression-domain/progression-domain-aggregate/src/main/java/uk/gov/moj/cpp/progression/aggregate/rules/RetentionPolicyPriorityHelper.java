package uk.gov.moj.cpp.progression.aggregate.rules;

import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.CUSTODIAL;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.REMITTAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RetentionPolicyPriorityHelper {

    private static final int DAYS_IN_CALENDAR_YEAR = 365;
    private static final int DAYS_IN_CALENDAR_MONTH = 30;
    private static final String SENTENCE_REG_EXP = "\\d(\\d)*";


    private RetentionPolicyPriorityHelper() {
        //no initialisation
    }

    public static RetentionPolicy getRetentionPolicyByPriority(final Collection<RetentionPolicy> retentionPolicyCollection) {
        final List<RetentionPolicy> retentionPolicies = new ArrayList<>(retentionPolicyCollection);

        retentionPolicies.sort((retentionPolicy1, retentionPolicy2) -> {
            int priorityCompare = Integer.compare(
                    retentionPolicy1.getPolicyType().getPriority(),
                    retentionPolicy2.getPolicyType().getPriority());

            if (priorityCompare != 0) {
                return -priorityCompare;
            }

            if ((retentionPolicy1.getPolicyType() == CUSTODIAL && retentionPolicy2.getPolicyType() == CUSTODIAL) ||
                    (retentionPolicy1.getPolicyType() == REMITTAL && retentionPolicy2.getPolicyType() == REMITTAL)) {

                return Integer.compare(
                        retentionPolicy1.getPeriodDays(),
                        retentionPolicy2.getPeriodDays());
            }

            return 0;
        });

        return retentionPolicies.get(retentionPolicies.size()-1);
    }

    public static int periodToDays(final String periodStr) {
        final List<Integer> matches = getSentenceYearsMonthsDays(periodStr);
        return (matches.get(0) * DAYS_IN_CALENDAR_YEAR)
                + (matches.get(1) * DAYS_IN_CALENDAR_MONTH)
                + matches.get(2);
    }

    private static List<Integer> getSentenceYearsMonthsDays(final String sentenceStr) {
        final Matcher matcher = Pattern.compile(SENTENCE_REG_EXP).matcher(sentenceStr);
        final List<Integer> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(Integer.valueOf(matcher.group()));
        }
        return matches;
    }

}
