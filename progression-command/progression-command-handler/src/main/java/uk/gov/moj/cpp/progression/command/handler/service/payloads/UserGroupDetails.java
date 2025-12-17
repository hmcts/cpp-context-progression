package uk.gov.moj.cpp.progression.command.handler.service.payloads;

import java.util.Objects;
import java.util.UUID;

public class UserGroupDetails {

    private final UUID groupId;

    private final String groupName;

    public UserGroupDetails(final UUID groupId, final String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
    }

    private UserGroupDetails(final UserGroupDetails.Builder builder) {
        groupId = builder.groupId;
        groupName = builder.groupName;
    }

    public static UserGroupDetails of(final UUID groupId, final String groupName) {
        return new UserGroupDetails(groupId, groupName);
    }

    public static UserGroupDetails.Builder newBuilder() {
        return new UserGroupDetails.Builder();
    }

    public static UserGroupDetails.Builder newBuilder(final UserGroupDetails copy) {
        final UserGroupDetails.Builder builder = new UserGroupDetails.Builder();
        builder.groupId = copy.groupId;
        builder.groupName = copy.getGroupName();
        return builder;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, groupName);
    }

    @Override
    public String toString() {
        return "OrganisationDetails{" +
                "id=" + groupId +
                ", name='" + groupName + '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UserGroupDetails that = (UserGroupDetails) o;
        return Objects.equals(groupId, that.getGroupId()) &&
                Objects.equals(groupName, that.getGroupName());
    }

    public static final class Builder {

        private UUID groupId;
        private String groupName;

        private Builder() {
        }

        public UserGroupDetails.Builder withId(final UUID val) {
            groupId = val;
            return this;
        }

        public UserGroupDetails.Builder withName(final String val) {
            groupName = val;
            return this;
        }

        public UserGroupDetails build() {
            return new UserGroupDetails(this);
        }
    }
}

