package uk.gov.moj.cpp.progression.command.accesscontrol;

import static javax.json.Json.createObjectBuilder;

public class PermissionRuleConstants {


    private PermissionRuleConstants() {
    }

    public static String petFormGrantAccessPermission() {
        return createObjectBuilder().add("object", "PetForm").add("action", "GrantAccess").build().toString();
    }
}
