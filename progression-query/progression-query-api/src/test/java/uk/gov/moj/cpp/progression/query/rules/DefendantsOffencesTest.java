package uk.gov.moj.cpp.progression.query.rules;


import org.junit.Test;

import static java.util.Arrays.stream;

public class DefendantsOffencesTest extends ProgressionQueryRuleExecutor {

    private static final String[] ACTION_GROUPS = new String[] {"Crown Court Admin",
                    "Listing Officers", "Judiciary", "Case Officer"};

    private static final String[] ALLOWED_USER_GROUPS = new String[] {"Crown Court Admin",
                    "Listing Officers", "Judiciary", "Case Officer"};

    private static final String MEDIA_TYPE = "progression.query.defendant-offences";

    private static final String[] NOT_ALLOWED_USER_GROUPS = new String[] {"CMS", "Charging Lawyers",
                    "Court Administrators", "Court Clerks", "Court Operations Officers",
                    "Group Name", "Group name 3", "IDAM", "Judge", "JudicialOfficer",
                    "Legal Advisers", "MCSS", "Magistrates", "Solicitors", "TFL Users",
                    "Test Group", "System Users", "Genesis"};

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
