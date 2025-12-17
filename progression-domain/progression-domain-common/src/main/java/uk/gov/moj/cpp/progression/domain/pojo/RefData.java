package uk.gov.moj.cpp.progression.domain.pojo;

import java.io.Serializable;
import java.util.UUID;

public class RefData implements Serializable {

    private final UUID directionRefDataId;
    private final String directionRefDataType;
    private final String directionRefDataName;

    public RefData(final UUID directionRefDataId, final String directionRefDataType, final String directionRefDataName) {
        this.directionRefDataId = directionRefDataId;
        this.directionRefDataType = directionRefDataType;
        this.directionRefDataName = directionRefDataName;
    }

    public UUID getDirectionRefDataId() {
        return directionRefDataId;
    }

    public String getDirectionRefDataType() {
        return directionRefDataType;
    }

    public String getDirectionRefDataName() {
        return directionRefDataName;
    }

    public static Builder refData() {
        return new Builder();
    }

    public static class Builder {
        private UUID directionRefDataId;
        private String directionRefDataType;
        private String directionRefDataName;

        public Builder withDirectionRefDataId(final UUID directionRefDataId) {
            this.directionRefDataId = directionRefDataId;
            return this;
        }

        public Builder withDirectionRefDataType(final String directionRefDataType) {
            this.directionRefDataType = directionRefDataType;
            return this;
        }

        public Builder withDirectionRefDataName(final String directionRefDataName) {
            this.directionRefDataName = directionRefDataName;
            return this;
        }

        public RefData build() {
            return new RefData(directionRefDataId, directionRefDataType, directionRefDataName);
        }
    }
}
