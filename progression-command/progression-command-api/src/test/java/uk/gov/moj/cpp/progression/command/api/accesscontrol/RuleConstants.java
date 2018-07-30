package uk.gov.moj.cpp.progression.command.api.accesscontrol;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

public final class RuleConstants {

    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_CROWN_COURT_ADMIN = "Crown Court Admin";
    private static final String GROUP_LISTING_OFFICERS = "Listing Officers";
    private static final String GROUP_CMS = "CMS";
    private static final String GROUP_JUDICIARY = "Judiciary";

    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getAddCaseDocumentActionGroups() {
        return Arrays.asList(GROUP_SYSTEM_USERS, GROUP_LISTING_OFFICERS, GROUP_CROWN_COURT_ADMIN);
    }

    public static List<String> getAddDefendantActionGroups() {
        return Arrays.asList(GROUP_CMS);
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
        return Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS);
    }

    public static List<String> getUpdateDefendantActionGroups() {
        return Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS);
    }
}