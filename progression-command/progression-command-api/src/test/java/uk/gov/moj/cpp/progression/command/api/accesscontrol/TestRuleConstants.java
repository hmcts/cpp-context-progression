package uk.gov.moj.cpp.progression.command.api.accesscontrol;

import java.util.Arrays;
import java.util.List;

public final class TestRuleConstants {

   /* private static final String GROUP_CROWN_COURT_ADMIN = "Crown Court Admin";
    private static final String GROUP_LISTING_OFFICERS = "Listing Officers";
    private static final String GROUP_LEGAL_ADVISOR = "Legal Advisers";
    private static final String GROUP_COURT_CLERKS = "Court Clerks";
    private static final String GROUP_CMS = "CMS";

    private TestRuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getReferToCourtActionGroups() {
        return Arrays.asList(GROUP_LEGAL_ADVISOR);
    }

    @Deprecated
    public static List<String> getAddDefendantActionGroups() {
        return Arrays.asList(GROUP_CMS);
    }

    @Deprecated
    public static List<String> getUpdateOffencesForDefendantActionGroups() {
        return Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS,GROUP_COURT_CLERKS);
    }

    @Deprecated
    public static List<String> getUpdateDefendantActionGroups() {
        return Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS,GROUP_COURT_CLERKS);
    }*/


    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_CROWN_COURT_ADMIN = "Crown Court Admin";
    private static final String GROUP_LISTING_OFFICERS = "Listing Officers";
    private static final String GROUP_CMS = "CMS";
    private static final String GROUP_JUDICIARY = "Judiciary";
    private static final String GROUP_COURT_CLERKS = "Court Clerks";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";
    private static final String GROUP_NPS = "Probation Admin";
    private static final String GROUP_COURT_ASSOCIATE = "Court Associate";

    private TestRuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getAddCaseDocumentActionGroups() {
        return Arrays.asList(GROUP_SYSTEM_USERS, GROUP_LISTING_OFFICERS, GROUP_CROWN_COURT_ADMIN);
    }

    public static String getAddDefendantActionGroups() {
        return GROUP_CMS;
    }

    public static List<String> getUpdateBailStatusForDefendantActionGroups() {
        return Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS, GROUP_JUDICIARY);
    }

    public static List<String> getUpdateSolicitorFirmForDefendantActionGroups() {
        return Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS, GROUP_JUDICIARY);
    }

    public static List<String> getUpdateInterpreterorDefendantActionGroups() {
        return Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS, GROUP_JUDICIARY);
    }

    public static List<String> getUpdateOffencesForDefendantActionGroups() {
        return Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS,GROUP_COURT_CLERKS,GROUP_LEGAL_ADVISERS,GROUP_COURT_ADMINISTRATORS, GROUP_NPS,GROUP_COURT_ASSOCIATE);
    }

    public static List<String> getUpdateDefendantActionGroups() {
        return Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS,GROUP_COURT_CLERKS,GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS, GROUP_NPS,GROUP_COURT_ASSOCIATE);
    }

}