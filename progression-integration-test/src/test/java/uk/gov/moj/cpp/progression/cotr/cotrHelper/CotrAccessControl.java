package uk.gov.moj.cpp.progression.cotr.cotrHelper;

import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubUserWithPermission;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

public class CotrAccessControl {

    public static void mockDefenceUserCotrAccessControl(final String userId) {
        final String permission = getPayload("stub-data/usersgroups.defence-user-permission-for-cotr.json");
        stubUserWithPermission(userId, permission);
    }

    public static void mockProsecutionUserCotrAccessControl(final String userId) {
        final String permission = getPayload("stub-data/usersgroups.prosecution-user-permission-for-cotr.json");
        stubUserWithPermission(userId, permission);
    }

    public static void mockCourtsUserCotrAccessControl(final String userId) {
        final String permission = getPayload("stub-data/usersgroups.courts-user-permission-for-cotr.json");
        stubUserWithPermission(userId, permission);
    }
}
