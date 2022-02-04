package uk.gov.moj.cpp.progression.query.api.helper;

import static javax.json.Json.createObjectBuilder;

public class RuleConstants {

    private RuleConstants() {
    }

    public static String petFormGrantAccessPermission() {
        return createObjectBuilder().add("object", "PetForm").add("action", "GrantAccess").build().toString();
    }
}
