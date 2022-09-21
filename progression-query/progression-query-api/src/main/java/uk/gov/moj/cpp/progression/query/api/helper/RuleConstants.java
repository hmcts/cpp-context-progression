package uk.gov.moj.cpp.progression.query.api.helper;

import static javax.json.Json.createObjectBuilder;

public class RuleConstants {

    static final String COTR_DEFENCE_ACCESS = "defence-access";
    static final String COTR_COURTS_ACCESS = "courts-access";
    static final String COTR = "COTR";
    static final String OBJECT = "object";
    static final String ACTION = "action";

    static final String DASHBOARD_OBJECT = "DASHBOARD";
    static final String DASHBOARD_ACTION = "dashboard-access";

    private RuleConstants() {
    }

    public static String petFormGrantAccessPermission() {
        return createObjectBuilder().add(OBJECT, "PetForm").add(ACTION, "GrantAccess").build().toString();
    }

    public static String[] cotrTrialHearingsGrantAccessPermission() {
        return new String[]{createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_DEFENCE_ACCESS).build().toString(),
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_COURTS_ACCESS).build().toString()};
    }

    public static String[] cotrDetailsGrantAccessPermission() {
        return new String[]{createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_DEFENCE_ACCESS).build().toString(),
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_COURTS_ACCESS).build().toString()};
    }

    public static String[] cotrFormGrantAccessPermission() {
        return new String[]{createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_DEFENCE_ACCESS).build().toString(),
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_COURTS_ACCESS).build().toString()};
    }

    public static String[] searchTrialReadinessGrantAccessPermission() {
        return new String[]{createObjectBuilder().add(OBJECT, DASHBOARD_OBJECT).add(ACTION, DASHBOARD_ACTION).build().toString()
        };
    }

    public static String[] trialReadinessDetailsAccessPermission() {
        return new String[]{createObjectBuilder().add(OBJECT, DASHBOARD_OBJECT).add(ACTION, DASHBOARD_ACTION).build().toString()
        };
    }
}
