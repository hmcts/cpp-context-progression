package uk.gov.moj.cpp.progression.query.rules;


import org.junit.Test;

import static java.util.Arrays.stream;

public class CaseSearchByUrnDetailsTest extends ProgressionQueryRuleExecutor {

    private static final String[] ACTION_GROUPS = new String[] {"System Users","CMS", "Court Clerks","Crown Court Admin",
            "Listing Officers", "Judiciary", "Case Officer", "Court Clerks"};

    private static final String[] ALLOWED_USER_GROUPS = new String[] {"System Users","CMS", "Court Clerks","Crown Court Admin",
                    "Listing Officers", "Judiciary", "Case Officer", "Court Clerks"};

    private static final String[] NOT_ALLOWED_USER_GROUPS = new String[] {"Charging Lawyers",
                    "Court Administrators",  "Court Operations Officers",
                    "Group Name", "Group name 3", "IDAM", "Judge", "JudicialOfficer",
                    "Legal Advisers", "MCSS", "Magistrates", "Solicitors", "TFL Users",
                    "Test Group",  "Genesis"};

    private static final String MEDIA_TYPE = "progression.query.case-by-urn";

    @Test
    public void shouldPassAccessControl() throws Exception {
        assertSuccessfulOutcome(executeRules(MEDIA_TYPE, ACTION_GROUPS, ALLOWED_USER_GROUPS));
    }

    @Test
    public void shouldNotPassAccessControl() throws Exception {
        assertFailureOutcome(executeRules(MEDIA_TYPE, ACTION_GROUPS, NOT_ALLOWED_USER_GROUPS));
    }

    @Test
    public void shouldAllowUserBelongingToAGroup() throws Exception {
        stream(ALLOWED_USER_GROUPS).forEach(group -> {
            assertSuccessfulOutcome(executeRules(MEDIA_TYPE, ACTION_GROUPS, group));
        });
    }

    @Test
    public void shouldNotAllowUserBelongingToANotAllowedGroup() throws Exception {
        stream(NOT_ALLOWED_USER_GROUPS).forEach(group -> {
            assertFailureOutcome(executeRules(MEDIA_TYPE, ACTION_GROUPS, group));
        });
    }
}
