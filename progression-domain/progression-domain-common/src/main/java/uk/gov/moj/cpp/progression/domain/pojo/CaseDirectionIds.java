package uk.gov.moj.cpp.progression.domain.pojo;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class CaseDirectionIds implements Serializable {
    private final List<UUID> ids;

    public CaseDirectionIds(final List<UUID> ids){
        this.ids = ids;
    }

    public List<UUID> getIds() {
        return ids;
    }

    public static Builder caseDirectionIds() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CaseDirectionIds that = (CaseDirectionIds) o;
        return Objects.equals(ids, that.ids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ids);
    }

    public static class Builder {
        private List<UUID> ids;

        public Builder withIds(final List<UUID> ids) {
            this.ids = ids;
            return this;
        }

        public Builder withValuesFrom(final CaseDirectionIds caseDirectionIds) {
            this.ids = caseDirectionIds.ids;
            return this;
        }

        public CaseDirectionIds build(){
            return new CaseDirectionIds(ids);
        }
    }
}
