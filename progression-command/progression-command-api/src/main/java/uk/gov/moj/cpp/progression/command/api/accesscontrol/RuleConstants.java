package uk.gov.moj.cpp.progression.command.api.accesscontrol;

import java.util.Arrays;
import java.util.List;


/**
 *
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public final class RuleConstants {

    private static final String GROUP_CROWN_COURT_ADMIN = "Crown Court Admin";
    private static final String GROUP_LISTING_OFFICERS = "Listing Officers";
    private static final String GROUP_COURT_CLERKS = "Court Clerks";
    private static final String GROUP_CMS = "CMS";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";
    private static final String GROUP_NPS = "Probation Admin";
    private static final String GROUP_COURT_ASSOCIATE = "Court Associate";



    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }


    public static List<String> getAddDefendantActionGroups() {
        return Arrays.asList(GROUP_CMS);
    }

    public static List<String> getUpdateOffencesForDefendantActionGroups() {
        return Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS, GROUP_COURT_CLERKS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS, GROUP_NPS, GROUP_COURT_ASSOCIATE);
    }

    public static List<String> getUpdateDefendantActionGroups() {
        return Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS, GROUP_COURT_CLERKS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS, GROUP_NPS, GROUP_COURT_ASSOCIATE);
    }


}

