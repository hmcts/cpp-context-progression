package uk.gov.moj.cpp.external.domain.listing;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(NON_NULL)
public final class ListingCase implements Serializable {

    private static final long serialVersionUID = -3401762697801936468L;
    private UUID caseId;
    private String urn;
    private List<Hearing> hearings;

    @JsonCreator
    public ListingCase(@JsonProperty("caseId") final UUID caseId,
                       @JsonProperty("urn") final String urn,
                       @JsonProperty("hearings") final List<Hearing> hearings) {

        this.caseId = caseId;
        this.urn = urn;
        this.hearings = (null == hearings) ? new ArrayList<>() : new ArrayList<>(hearings);
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    public List<Hearing> getHearings() {
        return new ArrayList<>(hearings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListingCase)) {
            return false;
        }

        final ListingCase that = (ListingCase) o;

        if (caseId != null ? !caseId.equals(that.caseId) : that.caseId != null) {
            return false;
        }
        if (urn != null ? !urn.equals(that.urn) : that.urn != null) {
            return false;
        }
        return hearings != null ? hearings.equals(that.hearings) : that.hearings == null;
    }

    @Override
    public int hashCode() {
        int result = caseId != null ? caseId.hashCode() : 0;
        result = 31 * result + (urn != null ? urn.hashCode() : 0);
        result = 31 * result + (hearings != null ? hearings.hashCode() : 0);
        return result;
    }
}
