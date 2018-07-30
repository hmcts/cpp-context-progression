package uk.gov.moj.cpp.external.domain.hearing;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(NON_NULL)
public final class InitiateHearing implements Serializable {

    private static final long serialVersionUID = -3401762697801936468L;
    private List<Case> cases;
    private Hearing hearing;

    @JsonCreator
    public InitiateHearing(@JsonProperty("cases") final List<Case> cases,
                       @JsonProperty("hearing") final Hearing hearing) {

        this.cases = (null == cases) ? new ArrayList<>() : new ArrayList<>(cases);
        this.hearing = hearing;
    }

    public List<Case> getCases() {
        return cases;
    }

    public Hearing getHearing() {
        return hearing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InitiateHearing)) {
            return false;
        }

        final InitiateHearing that = (InitiateHearing) o;

        if (hearing != null ? !hearing.equals(that.hearing) : that.hearing != null) {
            return false;
        }
        return cases != null ? cases.equals(that.cases) : that.cases == null;
    }

    @Override
    public int hashCode() {
        int result = hearing != null ? hearing.hashCode() : 0;
        result = 31 * result + (cases != null ? cases.hashCode() : 0);
        return result;
    }
}
