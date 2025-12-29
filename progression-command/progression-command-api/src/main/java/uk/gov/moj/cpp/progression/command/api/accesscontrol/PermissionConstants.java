package uk.gov.moj.cpp.progression.command.api.accesscontrol;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

public class PermissionConstants {
    static final String COTR_DEFENCE_ACCESS = "defence-access";
    static final String COTR_COURTS_ACCESS = "courts-access";
    static final String COTR = "COTR";
    static final String OBJECT = "object";
    static final String ACTION = "action";
    private static final String CREATE_ACTION = "Create";
    private static final String OBJECT_CASE = "Case";


    private PermissionConstants() {
    }

    public static String[] createCotrPermissions() {
        return new String[]{createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_DEFENCE_ACCESS).build().toString(),
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_COURTS_ACCESS).build().toString()};
    }

    public static String[] archivePermissions() {
        return new String[]{
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_COURTS_ACCESS).build().toString(),
        };
    }

    public static String[] serveDefendantCotrPermissions() {
        return new String[]{createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_DEFENCE_ACCESS).build().toString(),
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_COURTS_ACCESS).build().toString()};
    }

    public static String[] changeDefendantsCotrPermissions() {
        return new String[]{
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_DEFENCE_ACCESS).build().toString(),
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_COURTS_ACCESS).build().toString()};
    }
    
    public static String[] addFurtherInfoDefenceCotrPermissions() {
        return new String[]{
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_DEFENCE_ACCESS).build().toString(),
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_COURTS_ACCESS).build().toString()};
    }

    public static String[] updateReviewNotesPermissions() {
        return new String[] {
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_COURTS_ACCESS).build().toString()
        };
    }

    public static String[] getCaseCreatePermission() {
        return new String[]{
                createObjectBuilder().add(OBJECT, OBJECT_CASE).add(ACTION, CREATE_ACTION).build().toString(),
        };
    }

    public static String[] getUsersForCourtProceedingsForApplication() {
        return new String[]{"Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users", "Probation Admin", "Court Associate", "NCES"};
    }
}
