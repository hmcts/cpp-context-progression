package uk.gov.moj.cpp.progression.query.api.helper;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

public class PermissionRuleConstants {

    private static final String OBJECT = "object";
    private static final String ACTION = "action";
    private static final String VIEW = "View";

    private PermissionRuleConstants() {
    }

    public static String petFormGrantAccessPermission() {
        return createObjectBuilder().add(OBJECT, "PetForm").add(ACTION, "GrantAccess").build().toString();
    }

    public static String bcmViewPermission() {
        return createObjectBuilder().add(OBJECT, "BCM").add(ACTION, VIEW).build().toString();
    }

    public static String ptphViewPermission() {
        return createObjectBuilder().add(OBJECT, "PTPH").add(ACTION, VIEW).build().toString();
    }
}
