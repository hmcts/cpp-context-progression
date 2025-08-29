package uk.gov.moj.cpp.progression.query.view;

import java.util.UUID;

public class UserGroupsDetails {

    private UUID groupId;
    private String groupName;

    public UserGroupsDetails(final UUID groupId, final String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(final UUID groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(final String groupName) {
        this.groupName = groupName;
    }
}
