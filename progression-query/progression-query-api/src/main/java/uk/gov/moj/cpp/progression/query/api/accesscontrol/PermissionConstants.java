package uk.gov.moj.cpp.progression.query.api.accesscontrol;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

public class PermissionConstants {
    static final String COTR_DEFENCE_ACCESS = "defence-access";
    static final String COTR_PROSECUTION_ACCESS = "prosecution-access";
    static final String COTR_COURTS_ACCESS = "courts-access";
    static final String OBJECT ="object";
    static final String ACTION ="action";
    static final String COTR = "COTR";

    private PermissionConstants() {
    }

    public static String[] createCotrPermissions() {
        return new String[] {createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_DEFENCE_ACCESS).build().toString(),
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_PROSECUTION_ACCESS).build().toString(),
                createObjectBuilder().add(OBJECT, COTR).add(ACTION, COTR_COURTS_ACCESS).build().toString()};
    }
}
